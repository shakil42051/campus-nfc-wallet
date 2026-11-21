package com.example.attendance.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.attendance.data.entity.AttendanceLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceLogDao {
    @Query("SELECT * FROM attendance_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AttendanceLogEntity>>

    @Query("SELECT * FROM attendance_logs WHERE logType = :logType ORDER BY timestamp DESC")
    fun getLogsByType(logType: String): Flow<List<AttendanceLogEntity>>

    @Query("SELECT * FROM attendance_logs ORDER BY timestamp DESC")
    suspend fun getAllLogsList(): List<AttendanceLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AttendanceLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<AttendanceLogEntity>): List<Long>

    @Query("DELETE FROM attendance_logs")
    suspend fun clearAllLogs()
}
