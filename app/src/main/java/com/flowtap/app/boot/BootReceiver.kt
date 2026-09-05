package com.flowtap.app.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flowtap.app.FlowtapApp
import com.flowtap.app.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * On device reboot, closes any session that was running (reason REBOOT) and
 * restores DND to its prior state. Blocking is not restored after reboot (V1).
 *
 * F7: uses goAsync() so the process is kept alive until reconciliation
 * (DB write + DND restore) actually completes.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? FlowtapApp ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SessionManager.reconcileOnBoot(app.container)
            } finally {
                pending.finish()
            }
        }
    }
}