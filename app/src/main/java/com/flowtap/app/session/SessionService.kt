package com.flowtap.app.session

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.flowtap.app.util.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannels(this)
        // If this service was restarted by the system after a process death,
        // make sure the persisted session is loaded back into SessionManager.
        SessionManager.init((application as com.flowtap.app.FlowtapApp).container)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            SessionManager.ensureLoaded()
            val snap = SessionManager.current
            withContext(Dispatchers.Main) {
                if (snap == null) {
                    stopSelf()
                } else {
                    startForeground(
                        Notifications.SESSION_NOTIF_ID,
                        Notifications.sessionNotification(this@SessionService, snap),
                    )
                    scheduleTimeout(snap)
                }
            }
        }
        return START_STICKY
    }

    private fun scheduleTimeout(snap: com.flowtap.app.data.model.ActiveSessionSnapshot) {
        val maxRemaining = SessionManager.MAX_SESSION_MS - snap.elapsedMs()
        val autoRemaining = snap.autoEndRemainingMs()
        val remaining = if (autoRemaining != null) minOf(maxRemaining, autoRemaining) else maxRemaining
        if (remaining <= 0) {
            SessionManager.endSession(com.flowtap.app.data.model.EndReason.TIMEOUT)
            return
        }
        scope.launch {
            delay(remaining)
            SessionManager.endSession(com.flowtap.app.data.model.EndReason.TIMEOUT)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}