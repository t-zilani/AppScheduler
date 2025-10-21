package com.example.appscheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.example.appscheduler.receivers.CancelScheduleReceiver
import com.example.appscheduler.utils.APLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppLaunchWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "AppLaunchWorker"
        const val KEY_PACKAGE = "packageName"
        const val KEY_SCHEDULE_ID = "scheduleId"

        // Notification channel id
        private const val CHANNEL_ID = "app_scheduler_channel"
        private const val CHANNEL_NAME = "App Scheduler"
        private const val NOTIF_ID_BASE = 1000
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val packageName = inputData.getString(KEY_PACKAGE)
        val scheduleId = inputData.getString(KEY_SCHEDULE_ID)

        if (packageName.isNullOrBlank()) {
            APLog.d(TAG, "No package provided")
            return@withContext Result.failure(
                Data.Builder().putString("reason", "NO_PACKAGE").build()
            )
        }

        try {
            // Attempt to get launch intent
            val pm: PackageManager = applicationContext.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(packageName)

            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                try {
                    // Attempt to start activity (may be blocked by OS)
                    applicationContext.startActivity(launchIntent)

                    // Success — return success with details
                    return@withContext Result.success(
                        Data.Builder()
                            .putString("result", "LAUNCHED_OK")
                            .putString("package", packageName)
                            .putString("scheduleId", scheduleId)
                            .build()
                    )
                } catch (ex: Exception) {
                    // Starting activity failed (SecurityException or other)
                    APLog.d(TAG, "startActivity blocked or failed: ${ex.message}, $ex")
                    // Fallback to notification below
                }
            } else {
                // No launch intent — app likely cannot be launched directly
                APLog.d(TAG, "No launch intent for package: $packageName")
                // Fallback to notification below
            }

            // If we reach here: either no launchIntent or startActivity failed.
            // Build a notification with "Open app" action that user can tap.
            sendFallbackNotification(packageName, scheduleId!!)

            return@withContext Result.success(
                Data.Builder()
                    .putString("result", "FALLBACK_NOTIFICATION_POSTED")
                    .putString("package", packageName)
                    .putString("scheduleId", scheduleId)
                    .build()
            )
        } catch (t: Throwable) {
            APLog.d(TAG, "Worker failed: ${t.message}, $t")
            return@withContext Result.failure(
                Data.Builder().putString("reason", t.message ?: "UNKNOWN").build()
            )
        }
    }

    private fun sendFallbackNotification(packageName: String, scheduleId: String) {
        val context = applicationContext
        createChannelIfNeeded(context)

        val appLabel = getAppLabel(context, packageName)

        // --- Cancel button setup here ---
        val cancelIntent = Intent(context, CancelScheduleReceiver::class.java).apply {
            putExtra("scheduleId", scheduleId)
            putExtra("packageName", packageName)
        }
        val cancelPending = PendingIntent.getBroadcast(
            context,
            ("cancel-$scheduleId").hashCode(),
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // -------------------------------

        // Build main intent to open app
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val openPending = PendingIntent.getActivity(
            context,
            packageName.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$appLabel was scheduled")
            .setContentText("Tap to open or cancel the schedule.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .addAction(R.drawable.ic_open, "Open app", openPending)
            .addAction(R.drawable.ic_cancel, "Cancel", cancelPending) // ← Add here
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify((scheduleId.hashCode() and 0x7FFFFFFF), notification)
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
                "App Scheduler",
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
}
