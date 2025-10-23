package com.example.appscheduler.data

import android.content.Context
import com.example.appscheduler.data.entities.Schedule
import com.example.appscheduler.utils.APLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class ConflictException(message: String) : Exception(message)

class ScheduleRepository private constructor(
    private val db: AppDatabase,
    private val context: Context
) {
    private val scheduleDao = db.scheduleDao()
    private val executionLogDao = db.executionLogDao()

    suspend fun createOrUpdateSchedule(
        packageName: String,
        label: String,
        scheduledEpochMs: Long,
        conflictWindowMs: Long = 0L
    ): String = withContext(Dispatchers.IO) {
        val startRange = scheduledEpochMs - conflictWindowMs
        val endRange = scheduledEpochMs + conflictWindowMs
        val conflict = scheduleDao.findAnyInRange(startRange, endRange)
        if (conflict != null) {
            if (conflict.packageName != packageName) {
                APLog.d("REPO", "conflict happened $packageName, ${conflict.packageName}")
                throw ConflictException("Scheduling skipped, Conflicts with ${conflict.packageName}")
            }
        }

        val existing = scheduleDao.getScheduleByPackage(packageName)
        return@withContext if (existing != null) {
            val updated = existing.copy(
                scheduledEpochMs = scheduledEpochMs,
                label = label,
                status = "PENDING"
            )
            APLog.d("REPO", "package schedule already exists: $packageName, ${existing.packageName}")
            scheduleDao.update(updated)
            WorkManagerHelper.cancelScheduledWork(context, existing.id)
            WorkManagerHelper.scheduleWithWorkManager(context, existing.id, packageName, scheduledEpochMs)
            "Updated schedule for ${existing.packageName}"
        } else {
            val scheduleId = UUID.randomUUID().toString()
            val schedule = Schedule(
                id = scheduleId,
                packageName = packageName,
                label = label,
                scheduledEpochMs = scheduledEpochMs,
                createdAtMs = System.currentTimeMillis(),
                status = "PENDING"
            )
            scheduleDao.insert(schedule)
            WorkManagerHelper.scheduleWithWorkManager(context, scheduleId, packageName, scheduledEpochMs)
            "Schedule Confirmed For $packageName"
        }
    }

    suspend fun cancelSchedule(scheduleId: String) = withContext(Dispatchers.IO) {
        val s = scheduleDao.getScheduleById(scheduleId) ?: return@withContext
        val updated = s.copy(status = "CANCELLED")
        scheduleDao.update(updated)
        WorkManagerHelper.cancelScheduledWork(context, scheduleId)
    }

    suspend fun insertSchedule(schedule: Schedule) {
        withContext(Dispatchers.IO) {
            scheduleDao.insert(schedule)
        }
    }

    companion object {
        @Volatile private var INSTANCE: ScheduleRepository? = null

        fun getInstance(context: Context): ScheduleRepository =
            INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getInstance(context)
                INSTANCE ?: ScheduleRepository(db, context.applicationContext).also { INSTANCE = it }
            }
    }
}
