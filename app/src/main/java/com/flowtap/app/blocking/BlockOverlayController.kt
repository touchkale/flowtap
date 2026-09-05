package com.flowtap.app.blocking

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.flowtap.app.MainActivity
import com.flowtap.app.R
import com.flowtap.app.data.model.ActiveSessionSnapshot
import com.flowtap.app.data.model.EndReason
import com.flowtap.app.session.SessionManager
import com.flowtap.app.util.Haptics
import com.flowtap.app.util.HoldToEnd
import com.flowtap.app.util.TimeUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * F6: draws the "Deep Work is active" overlay as a TYPE_ACCESSIBILITY_OVERLAY
 * window directly from the accessibility service. Requires zero permissions
 * and is NOT subject to background-activity-start restrictions — unlike
 * launching BlockOverlayActivity from a service, which some OEMs block.
 *
 * Plain-View implementation (no Compose/lifecycle wiring) — intentionally kept
 * dependency-light and robust.
 *
 * CRITICAL SAFETY: the overlay is dismissed on SCREEN_OFF / SHUTDOWN so it can
 * NEVER be left covering the lock screen or a powered-off display. The user can
 * always press the power button to escape; a new blocked-app event re-shows it.
 */
class BlockOverlayController(private val service: AccessibilityService) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())

    private var root: View? = null
    private var timerText: TextView? = null
    private var titleText: TextView? = null
    private var hintText: TextView? = null
    private var endButton: Button? = null
    private var holdContainer: LinearLayout? = null
    private var holdButton: Button? = null
    private var snap: ActiveSessionSnapshot? = null
    private var holdStartUptime = 0L
    private var holdLastTickSec = -1

    private val holdTick = object : Runnable {
        override fun run() {
            val elapsed = SystemClock.uptimeMillis() - holdStartUptime
            if (HoldToEnd.isComplete(elapsed)) {
                runCatching { Haptics.sessionEnd(service) }
                resetHold()
                SessionManager.endSession(EndReason.EMERGENCY)
                return
            }
            val remain = HoldToEnd.remainSecs(elapsed)
            if (remain != holdLastTickSec) {
                holdLastTickSec = remain
                runCatching { Haptics.tick(service) }
            }
            holdButton?.text = service.getString(R.string.overlay_hold_keep_x, remain)
            holdButton?.alpha = 0.55f + 0.45f * HoldToEnd.progress(elapsed)
            handler.postDelayed(this, 100)
        }
    }

    private fun resetHold() {
        handler.removeCallbacks(holdTick)
        holdLastTickSec = -1
        holdButton?.text = service.getString(R.string.overlay_hold_caps)
        holdButton?.alpha = 1f
        holdButton?.isPressed = false
    }

    /** Light or dark blocking screen, following the system night mode. */
    private val isDark: Boolean
        get() = (service.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF, Intent.ACTION_SHUTDOWN -> dismiss()
            }
        }
    }

    private val ticker = object : Runnable {
        override fun run() {
            timerText?.text = TimeUtil.formatClockTime(snap?.elapsedMs() ?: 0L)
            handler.postDelayed(this, 1000)
        }
    }

    init {
        runCatching {
            ContextCompat.registerReceiver(
                service,
                screenOffReceiver,
                IntentFilter(Intent.ACTION_SCREEN_OFF).apply {
                    addAction(Intent.ACTION_SHUTDOWN)
                },
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }
        // The overlay is shown EXPLICITLY from the accessibility service on a
        // blocked-app event (show()). This collector only auto-DISMISSES it
        // when the session ends — it never auto-shows it, so our own app's UI
        // is never covered while the user is managing the session.
        scope.launch {
            SessionManager.active.collect { current ->
                snap = current
                if (current == null) dismiss()
            }
        }
    }

    /** Immediately shows the overlay with the given session. Returns true on success. */
    fun show(session: ActiveSessionSnapshot): Boolean = runCatching {
        snap = session
        if (ensureShown()) render(session)
        true
    }.getOrDefault(false)

    private fun ensureShown(): Boolean {
        if (root != null) return true
        val wm = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        // NOTE: deliberately FOCUSABLE (no FLAG_NOT_FOCUSABLE) so the window
        // receives the BACK key. A fullscreen overlay swallows BACK otherwise
        // and the key feels dead. Back is routed to openTimer() via the root
        // view's key listener — see buildOverlayView().
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.OPAQUE,
        )
        params.gravity = Gravity.TOP or Gravity.START

        val view = buildOverlayView()
        wm.addView(view, params)
        root = view
        view.isFocusableInTouchMode = true
        view.requestFocus()
        handler.post(ticker)
        return true
    }

    private fun render(current: ActiveSessionSnapshot) {
        resetHold()
        val strict = current.strictMode
        titleText?.text = service.getString(R.string.overlay_active_x, current.modeName)
        hintText?.text = if (strict) {
            service.getString(R.string.overlay_strict_hint, current.tagName ?: "NFC")
        } else {
            service.getString(R.string.overlay_flex_hint)
        }
        // Strict sessions hide the instant End button but ALWAYS show the
        // hold-to-end emergency exit — the overlay can never trap the user.
        endButton?.visibility = if (strict) View.GONE else View.VISIBLE
        holdContainer?.visibility = if (strict) View.VISIBLE else View.GONE
        timerText?.text = TimeUtil.formatClockTime(current.elapsedMs())
    }

    private fun pillBackground(color: Int, radiusPx: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = radiusPx.toFloat()
        }

    private fun pillOutline(strokeColor: Int, radiusPx: Int, strokePx: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(0x00000000)
            setStroke(strokePx, strokeColor)
            cornerRadius = radiusPx.toFloat()
        }

    private fun buildOverlayView(): View {
        val dp = service.resources.displayMetrics.density
        fun px(dpv: Float) = (dpv * dp).toInt()

        val dark = isDark
        val bg = if (dark) 0xFF0A1411.toInt() else 0xFFEBF4F1.toInt()
        val primary = if (dark) 0xFFE4F2EE.toInt() else 0xFF0A1B16.toInt()
        val secondary = if (dark) 0xFF8FA8A1.toInt() else 0xFF47645C.toInt()
        val accent = if (dark) 0xFF2DBDA4.toInt() else 0xFF0B8F7A.toInt()
        val danger = if (dark) 0xFFE05B5B.toInt() else 0xFFC24848.toInt()
        val onAccent = 0xFFFFFFFF.toInt()

        val rootLayout = LinearLayout(service).apply {
            setBackgroundColor(bg)
            gravity = Gravity.CENTER
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setPadding(px(32f), px(32f), px(32f), px(32f))
            isFocusable = true
            isFocusableInTouchMode = true
            // BACK while the overlay is up exits to the home screen — never a
            // dead key, never revealing the blocked app underneath.
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    exitToHome()
                    true
                } else {
                    false
                }
            }
        }

        rootLayout.addView(ImageView(service).apply {
            setImageResource(R.drawable.ic_lock_overlay)
            layoutParams = LinearLayout.LayoutParams(px(48f), px(48f)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        })

        rootLayout.addView(SpacerView(px(20f)))

        titleText = TextView(service).apply {
            text = ""
            setTextColor(primary)
            textSize = 26f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
        }
        rootLayout.addView(titleText)

        rootLayout.addView(SpacerView(px(16f)))

        timerText = TextView(service).apply {
            // Clock format never exceeds 8 chars (HH:MM:SS); 40sp always fits.
            textSize = 40f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(accent)
            gravity = Gravity.CENTER
            includeFontPadding = false
            maxLines = 1
        }
        rootLayout.addView(timerText)

        rootLayout.addView(SpacerView(px(10f)))

        rootLayout.addView(TextView(service).apply {
            text = service.getString(R.string.overlay_elapsed)
            setTextColor(secondary)
            textSize = 14f
            gravity = Gravity.CENTER
        })

        rootLayout.addView(SpacerView(px(20f)))

        hintText = TextView(service).apply {
            textSize = 16f
            setTextColor(secondary)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        rootLayout.addView(hintText)

        rootLayout.addView(SpacerView(px(24f)))

        // Flexible sessions can be ended right from the overlay — this is the
        // escape hatch. Strict sessions hide it (tag-only by design).
        endButton = Button(service).apply {
            text = service.getString(R.string.common_end_session)
            textSize = 16f
            setTextColor(onAccent)
            background = pillBackground(accent, px(20f))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                px(56f),
            )
            setOnClickListener { SessionManager.endSession(EndReason.IN_APP) }
        }
        rootLayout.addView(endButton)

        // Strict sessions: emergency hold-to-end. Press-and-hold 3s with a
        // live countdown — deliberate enough to never fire accidentally, but
        // the overlay can never trap the user without their tag.
        holdContainer = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            addView(TextView(service).apply {
                text = service.getString(R.string.overlay_no_tag)
                setTextColor(secondary)
                textSize = 13f
                gravity = Gravity.CENTER
            })
            addView(SpacerView(px(8f)))
            holdButton = Button(service).apply {
                text = service.getString(R.string.overlay_hold_caps)
                textSize = 15f
                setTextColor(danger)
                background = pillOutline(danger, px(20f), px(1f).coerceAtLeast(2))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    px(56f),
                )
                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            holdStartUptime = SystemClock.uptimeMillis()
                            holdLastTickSec = -1
                            handler.post(holdTick)
                            runCatching { Haptics.tick(service) }
                            v.isPressed = true
                            true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            // Keeps screen readers correct: the press is a
                            // "click" from the accessibility perspective.
                            v.performClick()
                            resetHold()
                            true
                        }
                        else -> false
                    }
                }
            }
            addView(holdButton)
        }
        rootLayout.addView(holdContainer)

        rootLayout.addView(SpacerView(px(8f)))

        rootLayout.addView(Button(service).apply {
            text = service.getString(R.string.overlay_show_timer)
            textSize = 14f
            setTextColor(accent)
            background = pillBackground(0x00000000, px(20f))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                px(48f),
            )
            setOnClickListener { openTimer() }
        })

        return rootLayout
    }

    /**
     * Shared "take me to my timer" exit used by the button AND the hardware
     * back key. Dismisses first so the overlay is gone even if the app launch
     * is blocked — reopening a blocked app re-shows the overlay.
     */
    fun openTimer() {
        dismiss()
        val opened = runCatching {
            service.startActivity(
                Intent(service, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            )
            true
        }.getOrDefault(false)
        if (!opened) {
            // Newer Android blocks background activity starts from a
            // service — never leave the user guessing where to go.
            Toast.makeText(
                service,
                service.getString(R.string.overlay_open_app_toast),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    /** True while the overlay window is attached. */
    val isShowing: Boolean get() = root != null

    /**
     * Exit-to-home: dismisses the overlay and makes sure the user lands on
     * the home screen. Used by BACK (root key listener + service onKeyEvent
     * backup). The session keeps running — reopening a blocked app re-shows
     * the overlay. Returns true if the overlay was showing.
     */
    fun exitToHome(): Boolean {
        if (root == null) return false
        dismiss()
        runCatching { service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME) }
        return true
    }

    /**
     * Called from the service's onKeyEvent. Consumes BACK only while the
     * overlay is visible (the fullscreen window otherwise swallows it and the
     * key appears dead). Returns true if the key was handled.
     */
    fun handleBack(): Boolean = exitToHome()

    private fun SpacerView(heightPx: Int) = View(service).apply {
        layoutParams = LinearLayout.LayoutParams(1, heightPx)
    }

    fun dismiss() {
        val view = root ?: return
        root = null
        handler.removeCallbacks(ticker)
        resetHold()
        runCatching {
            (service.getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(view)
        }
    }

    fun destroy() {
        scope.cancel()
        runCatching { service.unregisterReceiver(screenOffReceiver) }
        dismiss()
    }
}