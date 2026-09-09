package com.flowtap.app.session

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.flowtap.app.blocking.BlockOverlayActivity
import com.flowtap.app.data.prefs.BlockingMethod
import com.flowtap.app.util.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground service that keeps the session alive while the app is closed.
 * Notification uses a chronometer based on the persisted start time, so it
 * never drifts and survives process restarts (START_STICKY + DataStore restore).
 *
 * F3: onStartCommand AWAITS the persisted-session restore before deciding to
 * show the notification — this eliminates the stop/start churn that occurred
 * when the service was restarted by the system before the load completed.
 */
class SessionService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var compatJob: Job? = null
    private var timeoutJob: Job? = null
    private var lastForegroundPkg: String? = null
    private var lastForegroundAt: Long = 0L

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannels(this)
        // If this service was restarted by the system after a process death,
        // make sure the persisted session is loaded back into SessionManager.
        SessionManager.init((application as com.flowtap.app.FlowtapApp).container)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Foreground services must call startForeground() synchronously inside
        // onStartCommand — doing it after suspending work (restore, DataStore
        // reads, Main dispatch) trips ForegroundServiceDidNotStartInTime and
        // kills the process on cold start with a live session. Post a
        // placeholder immediately, then replace it once loaded.
        startForeground(
            Notifications.SESSION_NOTIF_ID,
            Notifications.startingNotification(this),
        )
        scope.launch {
            SessionManager.ensureLoaded()
            val snap = SessionManager.current
            withContext(Dispatchers.Main) {
                if (snap == null) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    val hardcore = runCatching {
                        (application as com.flowtap.app.FlowtapApp).container
                            .settingsDataStore.hardcoreMode.first()
                    }.getOrDefault(false)
                    startForeground(
                        Notifications.SESSION_NOTIF_ID,
                        Notifications.sessionNotification(this@SessionService, snap, hardcore),
                    )
                    scheduleTimeout(snap)
                    startCompatPolling()
                }
            }
        }
        return START_STICKY
    }

    /**
     * Compatibility blocking: polls UsageStatsManager every 2s when the user
     * chose COMPATIBILITY mode. Slow and battery-heavy by design — disclosed
     * in onboarding + Settings — but works without accessibility so banking
     * apps keep working.
     */
    private fun startCompatPolling() {
        compatJob?.cancel()
        compatJob = scope.launch {
            val container = (application as com.flowtap.app.FlowtapApp).container
            var lastBlockedAt = 0L
            val homePkgs = runCatching {
                val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                packageManager.queryIntentActivities(home, 0)
                    .map { it.activityInfo.packageName }.toSet()
            }.getOrDefault(emptySet())
            while (true) {
                delay(2000)
                val snap = SessionManager.current ?: continue
                val method = runCatching {
                    container.settingsDataStore.blockingMethod.first()
                }.getOrDefault(BlockingMethod.FULL_POWER)
                if (method != BlockingMethod.COMPATIBILITY) continue
                // B2: retain foreground knowledge — if the poller misses the
                // 5s window but the same app is still open, keep enforcing.
                val queried = queryForegroundPackage()
                val nowWall = System.currentTimeMillis()
                val pkg = queried?.also {
                    lastForegroundPkg = it
                    lastForegroundAt = nowWall
                } ?: lastForegroundPkg?.takeIf { nowWall - lastForegroundAt < 30_000 } ?: continue
                if (com.flowtap.app.blocking.BlockExemptions.isExempt(pkg, packageName, homePkgs)) continue
                val blocked = UsageBlockPoller.isBlocked(
                    pkg = pkg,
                    blockedApps = snap.blockedApps,
                    allowlistOnly = snap.allowlistOnly,
                    allowedApps = snap.allowedApps,
                )
                if (!blocked) continue
                val now = SystemClock.elapsedRealtime()
                if (now - lastBlockedAt < 2000) continue
                lastBlockedAt = now
                // HOME via intent (no accessibility GLOBAL_ACTION_HOME here);
                // the blocked app may flash for a frame — unavoidable, disclosed.
                runCatching {
                    startActivity(
                        Intent(Intent.ACTION_MAIN)
                            .addCategory(Intent.CATEGORY_HOME)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
                runCatching {
                    startActivity(
                        Intent(this@SessionService, BlockOverlayActivity::class.java)
                            .addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
                            ),
                    )
                }
            }
        }
    }

    private fun queryForegroundPackage(): String? {
        return runCatching {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val events = usm.queryEvents(now - 10_000, now)
            val collected = mutableListOf<UsageBlockPoller.ForegroundEvent>()
            while (events.hasNextEvent()) {
                val e = UsageEvents.Event()
                events.getNextEvent(e)
                if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    collected.add(
                        UsageBlockPoller.ForegroundEvent(
                            packageName = e.packageName ?: continue,
                            timeMs = e.timeStamp,
                        ),
                    )
                }
            }
            UsageBlockPoller.currentPackage(collected, now, windowMs = 5000L)
        }.getOrNull()
    }

    private fun scheduleTimeout(snap: com.flowtap.app.data.model.ActiveSessionSnapshot) {
        timeoutJob?.cancel()
        val expectedId = snap.sessionId
        val maxRemaining = SessionManager.MAX_SESSION_MS - snap.elapsedMs()
        val autoRemaining = snap.autoEndRemainingMs()
        val remaining = if (autoRemaining != null) minOf(maxRemaining, autoRemaining) else maxRemaining
        if (remaining <= 0) {
            if (SessionManager.current?.sessionId == expectedId) {
                SessionManager.endSession(com.flowtap.app.data.model.EndReason.TIMEOUT)
            }
            return
        }
        // D2: single deadline per service start, guarded by session ID so a
        // stale service never ends a newer session after rapid switching.
        timeoutJob = scope.launch {
            delay(remaining)
            if (SessionManager.current?.sessionId == expectedId) {
                SessionManager.endSession(com.flowtap.app.data.model.EndReason.TIMEOUT)
            }
        }
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = null

    override fun onDestroy() {
        compatJob?.cancel()
        timeoutJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }
}