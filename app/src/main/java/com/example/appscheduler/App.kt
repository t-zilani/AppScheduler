package com.example.appscheduler

import android.app.Application
import com.example.appscheduler.data.AppDatabase
import com.example.appscheduler.data.ScheduleRepository

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDatabase.getInstance(this)
        ScheduleRepository.getInstance(this)
    }
}
