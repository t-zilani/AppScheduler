package com.example.appscheduler.presentation.viewmodel

import WorkManagerHelper
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.appscheduler.data.repository.ConflictException
import com.example.appscheduler.data.repository.ScheduleRepository
import com.example.appscheduler.data.entities.Schedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID


class ScheduleViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = ScheduleRepository.getInstance(application)

    val allSchedules: LiveData<List<Schedule>> = repo.getAllSchedulesLive()

    private val _executedSchedule = MutableLiveData<Schedule>()
    val executedSchedule: LiveData<Schedule> get() = _executedSchedule

//    fun markScheduleExecuted(schedule: Schedule) {
//        viewModelScope.launch {
//            val updatedSchedule = schedule.copy(isExecuted = true)
//            repo.createOrUpdateSchedule(updatedSchedule.packageName, updatedSchedule.label, updatedSchedule.scheduledEpochMs)
//            _executedSchedule.postValue(updatedSchedule)
//        }
//    }

    fun markScheduleExecutedByPackage(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val schedule = repo.getScheduleByPackage(packageName)
            schedule.let {
                repo.updateSchedule(it.copy(isExecuted = true))
            }
        }
    }


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

    fun cancelSchedule(packageName: String, scheduleId: String) {
        viewModelScope.launch {
            repo.cancelSchedule(packageName, scheduleId)
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
