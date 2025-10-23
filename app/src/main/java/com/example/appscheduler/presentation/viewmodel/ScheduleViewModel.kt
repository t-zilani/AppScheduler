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

//    // observable schedules
//    val schedulesFlow = repo.getAllSchedulesFlow()
//        .map { it } // you can map to UI model if needed
//        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun createAndSchedule(appLabel: String, packageName: String, scheduledEpochMs: Long, onResult: (Boolean, Long?) -> Unit) {
        viewModelScope.launch {
            val scheduleId = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE
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

                // schedule WorkManager job
                WorkManagerHelper.scheduleWithWorkManager(getApplication(), scheduleId, packageName, scheduledEpochMs)
                onResult(true, scheduleId)
            } catch (ce: ConflictException) {
                onResult(false, -1)
            } catch (t: Throwable) {
                onResult(false, -1)
            }
        }
    }

    fun cancelSchedule(scheduleId: String) {
        viewModelScope.launch {
            repo.cancelSchedule(scheduleId)
        }
    }
}
