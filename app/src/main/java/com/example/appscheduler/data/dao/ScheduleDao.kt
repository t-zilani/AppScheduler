package com.example.appscheduler.data.dao

import androidx.room.*
import com.example.appscheduler.data.entities.Schedule

@Dao
interface ScheduleDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(schedule: Schedule): Long

    @Update
    suspend fun update(schedule: Schedule): Int

    @Delete
    suspend fun delete(schedule: Schedule): Int

    @Query("DELETE FROM schedule WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
