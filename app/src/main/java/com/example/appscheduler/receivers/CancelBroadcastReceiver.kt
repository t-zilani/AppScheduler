package com.example.appscheduler.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.example.appscheduler.data.repository.ScheduleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class CancelScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val scheduleId = intent?.getStringExtra("scheduleId")
        val packageName = intent?.getStringExtra("packageName")
        val pendingResult = goAsync()
        if (!scheduleId.isNullOrEmpty()) {
            val uniqueWorkName = "schedule-$scheduleId"
            WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName)

            GlobalScope.launch(Dispatchers.Default) {
                try {
                    val repo = ScheduleRepository.getInstance(context)
                    scheduleId.let { repo.cancelSchedule(it) }
                } finally {
                    pendingResult.finish()
                }
            }

            val notificationId = (scheduleId.hashCode() and 0x7FFFFFFF)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.cancel(notificationId)
        }
    }
}
