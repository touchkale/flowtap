package com.flowtap.app.session

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.flowtap.app.data.model.ActiveSessionSnapshot
import com.flowtap.app.data.model.EndReason
import com.flowtap.app.data.model.FocusMode
import com.flowtap.app.data.model.FocusTag
import com.flowtap.app.data.model.NotificationMode
import com.flowtap.app.data.model.SessionRecord
import com.flowtap.app.data.prefs.SettingsDataStore
import com.flowtap.app.data.repo.FocusRepository
import com.flowtap.app.di.AppContainer
import com.flowtap.app.util.AppIcons
import com.flowtap.app.util.Haptics
import com.flowtap.app.util.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single source of truth for the running session.
 * Every component (UI, NFC router, accessibility service, foreground service)
 * observes [active] or calls one of the start/end entry points.
 *
 * IMPORTANT: state is persisted to DataStore at every transition so the app
 * survives process death. Elapsed time is always derived from the persisted
 * wall-clock start time — never from an in-memory counter.
 */
object SessionManager {

    private val mutex = Mutex()
    private lateinit var app: Context
    private lateinit var repo: FocusRepository
    private lateinit var prefs: SettingsDataStore
    private lateinit var dnd: DndController
    private lateinit var ringer: RingerController
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _active = MutableStateFlow<ActiveSessionSnapshot?>(null)
    val active: StateFlow<ActiveSessionSnapshot?> = _active

    private var lastTapAt = 0L
    private var autoEndJob: Job? = null

    val isActive: Boolean get() = _active.value != null
    val current: ActiveSessionSnapshot? get() = _active.value

    /**
     * Effective keep-awake for the running session: per-mode override wins,
     * otherwise the global setting. Evaluated live so a global change applies
     * to SYSTEM-following sessions without an app restart.
     */
    suspend fun effectiveKeepAwake(): Boolean {
        val snap = _active.value ?: return false
        snap.keepAwake?.let { return it }
        return prefs.keepScreenAwake.first()
    }

    fun init(container: AppContainer) {
        if (this::app.isInitialized) return
        app = container.appContext
        repo = container.focusRepository
        prefs = container.settingsDataStore
        dnd = DndController(app, prefs)
        ringer = RingerController(app, prefs)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch { ensureLoaded() }
    }

    /**
     * Idempotent: restores a persisted in-progress session (process death /
     * service restart). Safe to call repeatedly; no-ops once active or when
     * no snapshot exists. MUST be awaited by every entry point so a cold
     * process never misreads "no session" while a session is actually running.
     */
    suspend fun ensureLoaded() {
        mutex.withLock { loadPersistedLocked() }
    }

    /** Assumes the mutex is already held. */
    private suspend fun loadPersistedLocked() {
        if (_active.value != null) return
        val snap = prefs.activeSession.first() ?: return
        if (snap.elapsedMs() >= MAX_SESSION_MS) {
            endSessionInternal(EndReason.TIMEOUT, snap)
        } else {
            val autoRemaining = snap.autoEndRemainingMs()
            if (autoRemaining != null && autoRemaining <= 0L) {
                endSessionInternal(EndReason.TIMEOUT, snap)
            } else {
                _active.value = snap
                scheduleAutoEndLocked(snap)
                startForegroundService()
            }
        }
    }

    /** Assumes the mutex is already held. Cancels any prior deadline, schedules the next. */
    private fun scheduleAutoEndLocked(snap: ActiveSessionSnapshot) {
        autoEndJob?.cancel()
        val remaining = snap.autoEndRemainingMs() ?: return
        if (remaining <= 0L) return
        autoEndJob = scope.launch {
            delay(remaining)
            endSession(EndReason.TIMEOUT)
        }
    }

    /**
     * NFC tap entry point. Strict sessions end only with the starting tag
     * (N1); flexible sessions end on the same mode. A different mode switches
     * unless the running session is strict/locked, in which case the tap is
     * ignored and a reminder notification is shown.
     */
    fun handleTagTap(mode: FocusMode, tag: FocusTag?) {
        val now = System.currentTimeMillis()
        if (now - lastTapAt < TAP_DEBOUNCE_MS) return
        lastTapAt = now
        Haptics.tick(app)
        scope.launch {
            mutex.withLock {
                loadPersistedLocked()
                val cur = _active.value
                when {
                    cur == null -> startSessionInternal(mode, tag)
                    cur.modeId == mode.id -> {
                        if (cur.strictMode) {
                            if (SessionPolicy.strictTapEndsSession(cur, mode, tag)) {
                                endSessionInternal(EndReason.TAP_END, cur)
                            } else {
                                Notifications.showStrictBlock(app, cur)
                            }
                        } else {
                            endSessionInternal(EndReason.TAP_END, cur)
                        }
                    }
                    cur.strictMode -> Notifications.showStrictBlock(app, cur)
                    else -> {
                        endSessionInternal(EndReason.SWITCHED, cur)
                        startSessionInternal(mode, tag)
                    }
                }
            }
        }
    }

    /** Manual start from the app UI (soft start fallback). */
    fun startSession(mode: FocusMode, tag: FocusTag?) {
        scope.launch {
            mutex.withLock {
                loadPersistedLocked()
                startSessionInternal(mode, tag)
            }
        }
    }

    /**
     * Live-applies a notification-mode change to the running session (Home
     * quick toggle cycles None/Silent/DND). Tears down the old behavior and
     * applies the new one, and persists the snapshot so process death
     * mid-session keeps the new value.
     */
    fun setSessionNotificationMode(mode: NotificationMode) {
        scope.launch {
            mutex.withLock {
                loadPersistedLocked()
                val cur = _active.value ?: return@withLock
                if (cur.notificationMode == mode) return@withLock
                teardownNotification(cur.notificationMode)
                applyNotification(mode)
                val updated = cur.copy(notificationMode = mode)
                _active.value = updated
                prefs.saveActiveSession(updated)
            }
        }
    }

    /** Applies [mode] fresh (no teardown): DND needs master + policy access. */
    private suspend fun applyNotification(mode: NotificationMode): SoundApplyResult {
        return when (mode) {
            NotificationMode.DND -> {
                if (!dnd.isPolicyAccessGranted()) return SoundApplyResult.NOT_GRANTED
                if (!prefs.dndMasterEnabled.first()) return SoundApplyResult.DISABLED_BY_MASTER
                dnd.enableDnd()
            }
            NotificationMode.SILENT -> ringer.silence()
            NotificationMode.NONE -> SoundApplyResult.APPLIED
        }
    }

    private suspend fun teardownNotification(mode: NotificationMode) {
        when (mode) {
            NotificationMode.DND -> dnd.restoreDnd()
            NotificationMode.SILENT -> ringer.restore()
            NotificationMode.NONE -> Unit
        }
    }

    /**
     * Applies a universal-master change to the running session, if any:
     * turning the master off restores DND immediately, turning it on
     * re-engages DND for a DND-mode running session. Silent is unaffected.
     */
    fun applyDndMaster() {
        scope.launch {
            mutex.withLock {
                loadPersistedLocked()
                val cur = _active.value ?: return@withLock
                if (cur.notificationMode != NotificationMode.DND) return@withLock
                val master = prefs.dndMasterEnabled.first()
                if (!master) {
                    dnd.restoreDnd()
                } else if (dnd.isPolicyAccessGranted()) {
                    dnd.enableDnd()
                }
            }
        }
    }

    /** Manual end from the app UI / notification action. Enforces the shared lock policy (S4). */
    fun endSession(reason: EndReason = EndReason.IN_APP) {
        scope.launch {
            mutex.withLock {
                loadPersistedLocked()
                val cur = _active.value ?: return@withLock
                val hardcore = prefs.hardcoreMode.first()
                if (!SessionPolicy.canEnd(reason, cur.strictMode, hardcore)) return@withLock
                endSessionInternal(reason, cur)
            }
        }
    }

    /**
     * Re-applies the requested sound after a permission return or startup,
     * when a failed/late grant left the phone behaving differently from the
     * mode (S5). Safe to call repeatedly; no-ops without an active session.
     */
    fun reconcileSound() {
        scope.launch {
            mutex.withLock {
                loadPersistedLocked()
                val cur = _active.value ?: return@withLock
                if (cur.notificationMode == NotificationMode.DND) {
                    if (prefs.dndMasterEnabled.first() && dnd.isPolicyAccessGranted()) {
                        dnd.enableDnd()
                    }
                }
            }
        }
    }

    private suspend fun startSessionInternal(mode: FocusMode, tag: FocusTag?) {
        if (_active.value != null) return
        val startedAt = System.currentTimeMillis()
        val sessionId = repo.insertSession(
            SessionRecord(
                modeId = mode.id,
                tagId = tag?.id,
                startedAt = startedAt,
                endedAt = 0,
                durationMs = 0,
                endReason = EndReason.TAP_END,
            )
        )
        val snap = ActiveSessionSnapshot(
            sessionId = sessionId,
            modeId = mode.id,
            tagId = tag?.id,
            modeName = mode.name,
            tagName = tag?.name,
            iconName = mode.iconName,
            accentIndex = mode.accentIndex,
            strictMode = mode.strictMode,
            notificationMode = mode.notificationMode,
            blockedApps = mode.blockedApps,
            keepAwake = mode.keepAwake,
            autoEndMinutes = mode.autoEndMinutes?.takeIf { it > 0 },
            allowlistOnly = mode.allowlistOnly,
            allowedApps = mode.allowedApps,
            startedAtEpoch = startedAt,
        )
        // Universal master switch gates DND; silent applies directly.
        applyNotification(mode.notificationMode)
        prefs.saveActiveSession(snap)
        tag?.let { repo.touchTag(it.id) }
        _active.value = snap
        scheduleAutoEndLocked(snap)
        startForegroundService()
        Haptics.sessionStart(app)
        // Warm the icon cache so blocked-app badges render logos immediately.
        scope.launch { AppIcons.preload(app, mode.blockedApps, 192) }
        // Best-effort launch-on-start: never fails the session if the app is gone.
        mode.launchPackage?.takeIf { it.isNotBlank() }?.let { pkg ->
            runCatching {
                val launch = app.packageManager.getLaunchIntentForPackage(pkg)
                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ?: return@runCatching
                app.startActivity(launch)
            }
        }
    }

    private suspend fun endSessionInternal(reason: EndReason, snap: ActiveSessionSnapshot) {
        autoEndJob?.cancel()
        autoEndJob = null
        val endedAt = System.currentTimeMillis()
        repo.updateSession(
            SessionRecord(
                id = snap.sessionId,
                modeId = snap.modeId,
                tagId = snap.tagId,
                startedAt = snap.startedAtEpoch,
                endedAt = endedAt,
                durationMs = (endedAt - snap.startedAtEpoch).coerceAtLeast(0L),
                endReason = reason,
            )
        )
        if (snap.notificationMode == NotificationMode.DND) dnd.restoreDnd()
        if (snap.notificationMode == NotificationMode.SILENT) ringer.restore()
        prefs.clearActiveSession()
        _active.value = null
        stopService()
        Haptics.sessionEnd(app)
        Notifications.showSessionComplete(app, snap.modeName, (endedAt - snap.startedAtEpoch).coerceAtLeast(0L))
    }

    /**
     * Boot-time reconciliation: closes any session that was running when the
     * device powered off (reason REBOOT) and restores DND. Lightweight — does
     * not require full SessionManager init. Callers should hold the receiver
     * alive with goAsync() until this completes.
     */
    suspend fun reconcileOnBoot(container: AppContainer) {
        mutex.withLock {
            val p = container.settingsDataStore
            val r = container.focusRepository
            val snap = p.activeSession.first() ?: run {
                _active.value = null
                autoEndJob?.cancel()
                autoEndJob = null
                return
            }
            val endedAt = System.currentTimeMillis()
            r.updateSession(
                SessionRecord(
                    id = snap.sessionId,
                    modeId = snap.modeId,
                    tagId = snap.tagId,
                    startedAt = snap.startedAtEpoch,
                    endedAt = endedAt,
                    durationMs = (endedAt - snap.startedAtEpoch).coerceAtLeast(0L),
                    endReason = EndReason.REBOOT,
                )
            )
            if (snap.notificationMode == NotificationMode.DND) {
                DndController(container.appContext, p).restoreDnd()
            }
            if (snap.notificationMode == NotificationMode.SILENT) {
                RingerController(container.appContext, p).restore()
            }
            p.clearActiveSession()
            _active.value = null
            autoEndJob?.cancel()
            autoEndJob = null
        }
    }

    private fun startForegroundService() {
        Notifications.ensureChannels(app)
        ContextCompat.startForegroundService(
            app,
            Intent(app, SessionService::class.java),
        )
    }

    private fun stopService() {
        app.stopService(Intent(app, SessionService::class.java))
    }

    private const val TAP_DEBOUNCE_MS = 2000L
    const val MAX_SESSION_MS = 12 * 60 * 60 * 1000L
}
