import android.content.Context
import com.example.appscheduler.AppLaunchWorker

object WorkManagerHelper {
    fun scheduleWithWorkManager(context: Context, scheduleId: Long, packageName: String, scheduledEpochMs: Long) {
        val now = System.currentTimeMillis()
        var delayMs = scheduledEpochMs - now
        if (delayMs < 0) delayMs = 0L

        val input = androidx.work.workDataOf(
            AppLaunchWorker.KEY_PACKAGE to packageName,
            AppLaunchWorker.KEY_SCHEDULE_ID to scheduleId
        )

        val request = androidx.work.OneTimeWorkRequestBuilder<AppLaunchWorker>()
            .setInitialDelay(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setInputData(input)
            .addTag("schedule-$scheduleId")
            .build()

        val uniqueName = "schedule-$scheduleId"
        androidx.work.WorkManager.getInstance(context)
            .enqueueUniqueWork(uniqueName, androidx.work.ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelScheduledWork(context: Context, scheduleId: String) {
        val uniqueWorkName = "schedule-$scheduleId"
        androidx.work.WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName)
    }
}
