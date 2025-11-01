package com.example.appscheduler.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule")
data class Schedule(
    @PrimaryKey
    val id: String,
    val packageName: String,
    val label: String,
    val scheduledEpochMs: Long,
    val createdAtMs: Long,
    val status: String,
    val isExecuted: Boolean = false
)
