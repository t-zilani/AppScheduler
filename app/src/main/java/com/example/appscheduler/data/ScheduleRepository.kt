package com.example.appscheduler.data

import android.content.Context
import com.example.appscheduler.data.entities.ExecutionLog
import com.example.appscheduler.data.entities.Schedule
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

    //fun getAllSchedulesFlow() = scheduleDao.getAllSchedulesFlow()

    //suspend fun getScheduleById(id: String) = scheduleDao.getScheduleById(id.toLong())

    /**
     * Insert schedule after checking conflict.
     * @param conflictWindowMs: consider a conflict if another schedule exists within +/- window
     * Throws ConflictException if conflict detected.
     */
    suspend fun insertSchedule(schedule: Schedule) {
        withContext(Dispatchers.IO) {
//            val start = schedule.scheduledEpochMs - conflictWindowMs
//            val end = schedule.scheduledEpochMs + conflictWindowMs
//            val conflict = scheduleDao.findAnyInRange(start, end)
//            if (conflict.isNotEmpty()) {
//                throw ConflictException("Conflicts with existing schedules.")
//            }
            scheduleDao.insert(schedule)
        }
    }

    suspend fun updateSchedule(schedule: Schedule) {
        withContext(Dispatchers.IO) {
            scheduleDao.update(schedule)
        }
    }

    suspend fun cancelSchedule(scheduleId: String) {
        withContext(Dispatchers.IO) {
            scheduleDao.deleteById(scheduleId.toLong())
        }
        WorkManagerHelper.cancelScheduledWork(context, scheduleId)
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
