package com.example.appscheduler.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
//import com.example.appscheduler.ScheduleRepository // optional: your repo to update DB

class CancelScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val scheduleId = intent?.getStringExtra("scheduleId")
        val packageName = intent?.getStringExtra("packageName")

        if (!scheduleId.isNullOrEmpty()) {
            // Cancel the WorkManager unique work
            val uniqueWorkName = "schedule-$scheduleId"
            WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName)

            // Optionally update Room DB to mark schedule as CANCELLED
            // If you have a Repository with synchronous/async API, call it here.
            // Example (non-blocking):
            // val repo = ScheduleRepository.getInstance(context)
            // repo.markCancelled(scheduleId)

            // Optionally cancel notification as well (if you used a notif id pattern)
            val notificationId = (scheduleId.hashCode() and 0x7FFFFFFF)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.cancel(notificationId)
        }
    }
}
