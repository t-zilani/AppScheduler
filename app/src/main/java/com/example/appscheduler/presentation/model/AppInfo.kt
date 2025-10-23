package com.example.appscheduler.presentation.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable,
    var scheduledEpochMs: Long? = null,
    var scheduleId: String? = null
)
