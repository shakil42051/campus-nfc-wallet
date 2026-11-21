package com.example.attendance.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.attendance.data.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records ORDER BY timestamp DESC")
    suspend fun getAllRecordsList(): List<AttendanceEntity>

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId ORDER BY timestamp DESC")
    fun getRecordsForStudent(studentId: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records WHERE syncStatus = 'PENDING' OR syncStatus = 'FAILED'")
    suspend fun getUnsyncedRecords(): List<AttendanceEntity>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE studentId = :studentId AND status = 'PRESENT'")
    fun getPresentCount(studentId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE studentId = :studentId AND status = 'LATE'")
    fun getLateCount(studentId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE studentId = :studentId AND status = 'ABSENT'")
    fun getAbsentCount(studentId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE studentId = :studentId AND status = 'SICK'")
    fun getSickCount(studentId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE studentId = :studentId AND (status = 'EXCUSED' OR status = 'LEAVE')")
    fun getExcusedCount(studentId: String): Flow<Int>

    @Query("UPDATE attendance_records SET status = :newStatus WHERE id = :id")
    suspend fun updateRecordStatus(id: Long, newStatus: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<AttendanceEntity>)

    @Update
    suspend fun updateRecord(record: AttendanceEntity)

    @Query("DELETE FROM attendance_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("DELETE FROM attendance_records")
    suspend fun clearAllRecords()
}
