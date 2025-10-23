package com.example.appscheduler.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.appscheduler.data.dao.ExecutionLogDao
import com.example.appscheduler.data.dao.ScheduleDao
import com.example.appscheduler.data.entities.ExecutionLog
import com.example.appscheduler.data.entities.Schedule

@Database(entities = [Schedule::class, ExecutionLog::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
    abstract fun executionLogDao(): ExecutionLogDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_scheduler_db"
                ).build().also { INSTANCE = it }
            }
    }
}
