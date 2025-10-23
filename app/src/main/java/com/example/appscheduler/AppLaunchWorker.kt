package com.example.appscheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.appscheduler.receivers.CancelScheduleReceiver
import com.example.appscheduler.utils.APLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import java.util.*

class AppLaunchWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "AppLaunchWorker"
        const val KEY_PACKAGE = "packageName"
        const val KEY_SCHEDULE_ID = "scheduleId"

        private const val CHANNEL_ID = "app_scheduler_channel"
        private const val CHANNEL_NAME = "App Scheduler"
        private const val NOTIF_ID_BASE = 1000
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val packageName = inputData.getString(KEY_PACKAGE) ?: return@withContext Result.failure()
        val scheduleId = inputData.getLong(KEY_SCHEDULE_ID, 0) ?: UUID.randomUUID().timestamp()

        val pm = applicationContext.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)

        if (launchIntent != null && isAppInForeground()) {
            try {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                applicationContext.startActivity(launchIntent)
                return@withContext Result.success()
            } catch (e: Exception) {
                APLog.d(TAG, "Foreground launch failed: ${e.message}")
            }
        }

        sendFallbackNotification(packageName, scheduleId)
        return@withContext Result.success()
    }

    private fun sendFallbackNotification(packageName: String, scheduleId: Long) {
        val context = applicationContext
        createChannelIfNeeded(context)

        val appLabel = getAppLabel(context, packageName)

        val cancelIntent = Intent(context, CancelScheduleReceiver::class.java).apply {
            putExtra(KEY_SCHEDULE_ID, scheduleId)
            putExtra(KEY_PACKAGE, packageName)
        }
        val cancelPending = PendingIntent.getBroadcast(
            context,
            ("cancel-$scheduleId").hashCode(),
            cancelIntent,
            getPendingIntentFlags()
        )

        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        val targetIntent = launchIntent ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))

        targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val openPending = PendingIntent.getActivity(
            context,
            packageName.hashCode(),
            targetIntent,
            getPendingIntentFlags()
        )

        val notificationId = NOTIF_ID_BASE + (scheduleId.hashCode() and 0x7FFFFFFF)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$appLabel was scheduled")
            .setContentText("Tap to open $appLabel now, or cancel the schedule.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openPending)
            .addAction(R.drawable.ic_open, "Open app", openPending)
            .addAction(R.drawable.ic_cancel, "Cancel", cancelPending)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId, notification)

        APLog.d(TAG, "Posted fallback notification for $packageName (notifId=$notificationId)")
    }

    private fun getAppLabel(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for scheduled app launches"
                enableVibration(true)
            }

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun getPendingIntentFlags(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    }

    private fun isAppInForeground(): Boolean {
        return ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }
}
