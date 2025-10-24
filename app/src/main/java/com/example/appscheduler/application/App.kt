package com.example.appscheduler.application

import android.app.Application
import com.example.appscheduler.data.database.AppDatabase
import com.example.appscheduler.data.repository.ScheduleRepository

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDatabase.getInstance(this)
        ScheduleRepository.getInstance(this)
    }
}
