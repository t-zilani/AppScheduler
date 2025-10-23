package com.example.appscheduler.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "execution_logs")
data class ExecutionLog(
    @PrimaryKey val id: String,
    val scheduleId: String,
    val attemptedAtMs: Long,
    val success: Boolean,
    val reason: String? = null
)
