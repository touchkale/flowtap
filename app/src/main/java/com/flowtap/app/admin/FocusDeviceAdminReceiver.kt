package com.flowtap.app.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.flowtap.app.R

/**
 * Device Admin component. While active, the app cannot be uninstalled
 * (the user must deactivate the admin in Settings first). We add friction
 * on deactivation via [onDisableRequested].
 */
class FocusDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        context.getString(R.string.device_admin_disable_warning)

    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, R.string.device_admin_enabled_toast, Toast.LENGTH_LONG).show()
    }
}