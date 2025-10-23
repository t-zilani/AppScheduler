package com.example.appscheduler.presentation.viewmodel

import WorkManagerHelper
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.appscheduler.data.ConflictException
import com.example.appscheduler.data.ScheduleRepository
import com.example.appscheduler.data.entities.Schedule
import kotlinx.coroutines.launch
import java.util.UUID


class ScheduleViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = ScheduleRepository.getInstance(application)

    fun createOrUpdateSchedule(
        appLabel: String,
        packageName: String,
        scheduledEpochMs: Long,
        conflictWindowMs: Long = 0L,
        onResult: (success: Boolean, scheduleIdOrMsg: String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val scheduleId = repo.createOrUpdateSchedule(packageName, appLabel, scheduledEpochMs, conflictWindowMs)
                onResult(true, scheduleId)
            } catch (ce: ConflictException) {
                onResult(false, ce.message ?: "Conflict")
            } catch (t: Throwable) {
                onResult(false, t.message ?: "Error")
            }
        }
    }

    fun cancelSchedule(scheduleId: String) {
        viewModelScope.launch {
            repo.cancelSchedule(scheduleId)
        }
    }

    fun createAndSchedule(appLabel: String, packageName: String, scheduledEpochMs: Long, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val scheduleId = UUID.randomUUID().toString()
            val schedule = Schedule(
                id = scheduleId,
                packageName = packageName,
                label = appLabel,
                scheduledEpochMs = scheduledEpochMs,
                createdAtMs = System.currentTimeMillis(),
                status = "PENDING"
            )
            try {
                repo.insertSchedule(schedule)
                WorkManagerHelper.scheduleWithWorkManager(getApplication(), scheduleId, packageName, scheduledEpochMs)
                onResult(true, scheduleId)
            } catch (ce: ConflictException) {
                onResult(false, "$ce")
            } catch (t: Throwable) {
                onResult(false, "$t")
            }
        }
    }
}
