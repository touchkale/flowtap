package com.flowtap.app.util

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.flowtap.app.MainActivity
import com.flowtap.app.R
import com.flowtap.app.data.model.ActiveSessionSnapshot

object Notifications {
    const val CHANNEL_SESSION = "focus_session"
    const val CHANNEL_INFO = "focus_info"
    const val SESSION_NOTIF_ID = 1001
    const val COMPLETE_NOTIF_ID = 1002
    const val EXTRA_ACTION = "action"
    const val ACTION_END_SESSION = "end_session"

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_SESSION) == null) {
            val session = NotificationChannel(
                CHANNEL_SESSION,
                context.getString(R.string.notif_channel_session),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.notif_channel_session_desc)
                setShowBadge(false)
            }
            nm.createNotificationChannel(session)
        }
        if (nm.getNotificationChannel(CHANNEL_INFO) == null) {
            val info = NotificationChannel(
                CHANNEL_INFO,
                context.getString(R.string.notif_channel_info),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.notif_channel_info_desc) }
            nm.createNotificationChannel(info)
        }
    }

    /**
     * Synchronous placeholder posted the moment the service starts, before the
     * persisted session is loaded. Replaced with [sessionNotification] once
     * known — this ordering is what keeps cold-start restore crash-free.
     */
    fun startingNotification(context: Context): Notification {
        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_SESSION)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle(context.getString(R.string.notif_starting))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    fun sessionNotification(
        context: Context,
        snap: ActiveSessionSnapshot,
        hardcore: Boolean = false,
    ): Notification {
        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_SESSION)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle(snap.modeName)
            .setContentText(context.getString(R.string.notif_session_text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setWhen(snap.startedAtEpoch)
            .setUsesChronometer(true)

        // S4: one lock policy — strict or Hardcore hides the End action everywhere.
        if (!com.flowtap.app.session.SessionPolicy.isEndLocked(snap, hardcore)) {
            val endIntent = PendingIntent.getActivity(
                context,
                1,
                Intent(context, MainActivity::class.java)
                    .putExtra(EXTRA_ACTION, ACTION_END_SESSION)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_stat_focus,
                    context.getString(R.string.common_end_session),
                    endIntent,
                ).build(),
            )
        }
        return builder.build()
    }

    @SuppressLint("MissingPermission")
    fun showSessionComplete(context: Context, modeName: String, durationMs: Long) {
        if (!hasPermission(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_INFO)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle(context.getString(R.string.notif_complete_x, modeName))
            .setContentText(TimeUtil.formatDuration(durationMs))
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(COMPLETE_NOTIF_ID, notification) }
    }

    @SuppressLint("MissingPermission")
    fun showStrictBlock(context: Context, snap: ActiveSessionSnapshot) {
        if (!hasPermission(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_INFO)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle(
                context.getString(R.string.notif_strict_title_x, snap.tagName ?: snap.modeName),
            )
            .setContentText(context.getString(R.string.notif_strict_text_x, snap.modeName))
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(COMPLETE_NOTIF_ID + 1, notification) }
    }

    fun hasPermission(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()
}
