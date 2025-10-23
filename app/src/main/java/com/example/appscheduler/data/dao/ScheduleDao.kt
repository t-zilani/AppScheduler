package com.example.appscheduler.data.dao

import androidx.room.*
import com.example.appscheduler.data.entities.Schedule
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM schedule ORDER BY scheduledEpochMs ASC")
    fun getAllSchedulesFlow(): Flow<List<Schedule>>

    @Query("SELECT * FROM schedule WHERE id = :id LIMIT 1")
    suspend fun getScheduleById(id: String): Schedule?

    @Query("SELECT * FROM schedule WHERE packageName = :packageName LIMIT 1")
    suspend fun getScheduleByPackage(packageName: String): Schedule?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(schedule: Schedule): Long

    @Update
    suspend fun update(schedule: Schedule): Int

    @Query("DELETE FROM schedule WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM schedule WHERE scheduledEpochMs BETWEEN :startMs AND :endMs LIMIT 1")
    suspend fun findAnyInRange(startMs: Long, endMs: Long): Schedule?

    @Delete
    suspend fun delete(schedule: Schedule): Int

}
