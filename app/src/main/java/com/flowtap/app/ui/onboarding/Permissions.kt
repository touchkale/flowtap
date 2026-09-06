package com.flowtap.app.ui.onboarding

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.flowtap.app.R
import com.flowtap.app.admin.FocusDeviceAdminReceiver
import com.flowtap.app.session.DndController

/** Checks and deep-links for every permission the app needs. */
object Permissions {

    fun hasNfcHardware(context: Context): Boolean =
        NfcAdapter.getDefaultAdapter(context) != null

    fun isNfcEnabled(context: Context): Boolean =
        NfcAdapter.getDefaultAdapter(context)?.isEnabled == true

    fun enableNfc(context: Context) {
        runCatching { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
    }

    fun isAccessibilityEnabled(context: Context): Boolean {
        val expected = ComponentName(context, com.flowtap.app.blocking.FocusAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        // System stores "package/package.Class"; accept both forms defensively.
        val short = expected.flattenToShortString()
        val full = expected.flattenToString()
        return enabled.split(':').any {
            it.equals(short, ignoreCase = true) || it.equals(full, ignoreCase = true)
        }
    }

    fun enableAccessibility(context: Context) {
        runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
    }

    fun isDndGranted(context: Context): Boolean {
        val app = context.applicationContext as com.flowtap.app.FlowtapApp
        return DndController(app, app.container.settingsDataStore).isPolicyAccessGranted()
    }

    fun enableDnd(context: Context) {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(ComponentName(context, FocusDeviceAdminReceiver::class.java))
    }

    fun enableDeviceAdmin(context: Context) {
        val component = ComponentName(context, FocusDeviceAdminReceiver::class.java)
        runCatching {
            context.startActivity(
                Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                    .putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        context.getString(R.string.settings_admin_explanation),
                    )
            )
        }
    }

    /** Deactivates the device admin so the app can be uninstalled. */
    fun deactivateDeviceAdmin(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val component = ComponentName(context, FocusDeviceAdminReceiver::class.java)
        runCatching { dpm.removeActiveAdmin(component) }
    }

    /**
     * Opens the system app-info screen for Flowtap, where the user taps
     * "Uninstall". This is the reliable path on every Android version: the
     * direct ACTION_UNINSTALL_PACKAGE intent can be rejected by the package
     * installer on newer builds (it requires the signature-only
     * REQUEST_DELETE_PACKAGES permission even for self-uninstall).
     */
    fun requestUninstall(context: Context) {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}"),
                ),
            )
        }
    }

    fun areNotificationsGranted(context: Context): Boolean =
        com.flowtap.app.util.Notifications.hasPermission(context)

    /** True when the system would still show the permission rationale. */
    fun shouldShowNotifRationale(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return false
        val activity = context.findActivity() ?: return true
        return androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            android.Manifest.permission.POST_NOTIFICATIONS,
        )
    }

    /** Opens the app-info screen (for permanently-denied permissions). */
    fun openAppSettings(context: Context) {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}"),
                ),
            )
        }
    }

    private fun Context.findActivity(): android.app.Activity? {
        var ctx = this
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun isBatteryOptimizationExempt(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun enableBatteryOptimizationExemption(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(android.net.Uri.parse("package:${context.packageName}")),
            )
        }
    }

    /** UsageStats access for compatibility blocking (AppOps check). */
    fun isUsageAccessGranted(context: Context): Boolean {
        return runCatching {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName,
            )
            mode == android.app.AppOpsManager.MODE_ALLOWED
        }.getOrDefault(false)
    }

    fun openUsageAccessSettings(context: Context) {
        runCatching { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
    }

    /** Draw-over-apps for compatibility overlay windows. */
    fun canDrawOverlays(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    fun openOverlaySettings(context: Context) {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:${context.packageName}"),
                ),
            )
        }
    }

    /** Android 16+ gate on receiving NFC tag intents. */
    fun isNfcTagIntentAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 36) return true
        return runCatching { NfcAdapter.getDefaultAdapter(context)?.isTagIntentAllowed == true }
            .getOrDefault(true)
    }

    fun allowNfcTagIntents(context: Context) {
        if (Build.VERSION.SDK_INT < 36) return
        runCatching {
            context.startActivity(Intent(NfcAdapter.ACTION_CHANGE_TAG_INTENT_PREFERENCE))
        }
    }
}