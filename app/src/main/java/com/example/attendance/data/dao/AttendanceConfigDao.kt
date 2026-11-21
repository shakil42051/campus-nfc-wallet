package com.example.attendance.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.attendance.data.entity.AttendanceConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceConfigDao {
    @Query("SELECT * FROM attendance_config WHERE id = 1 LIMIT 1")
    fun getConfigFlow(): Flow<AttendanceConfigEntity?>

    @Query("SELECT * FROM attendance_config WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): AttendanceConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: AttendanceConfigEntity)

    @Update
    suspend fun updateConfig(config: AttendanceConfigEntity)

    @Query("DELETE FROM attendance_config")
    suspend fun clearConfig()
}
