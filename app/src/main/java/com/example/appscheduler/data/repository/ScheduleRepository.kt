package com.example.appscheduler.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.appscheduler.data.database.AppDatabase
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

    fun getAllSchedulesLive(): LiveData<List<Schedule>> = scheduleDao.getAllSchedulesLive()

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
            existing.id
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
            scheduleId
        }
    }

    suspend fun getScheduleByPackage(packageName: String) : Schedule {
        return scheduleDao.getScheduleByPackage(packageName)!!
    }

    suspend fun updateSchedule(schedule: Schedule): Int {
        return scheduleDao.update(schedule)
    }

    suspend fun cancelSchedule(packageName: String, scheduleId: String) = withContext(Dispatchers.IO) {
        val s = scheduleDao.getScheduleByPackage(packageName)
        s?.let { scheduleDao.delete(it) }
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
