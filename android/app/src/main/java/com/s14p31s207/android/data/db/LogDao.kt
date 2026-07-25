package com.s14p31s207.android.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// LogDao.kt
@Dao
interface LogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: LogEntity)

    @Query("SELECT * FROM logs WHERE ipcId = :ipcId ORDER BY timestamp DESC")
    fun getAll(ipcId: String): Flow<List<LogEntity>>

    @Query("""
        SELECT * FROM logs 
        WHERE ipcId = :ipcId 
        AND timestamp BETWEEN :start AND :end 
        ORDER BY timestamp DESC
    """)
    fun getLogsByPeriod(ipcId: String, start: String, end: String): Flow<List<LogEntity>>

    @Query("""
        SELECT * FROM logs 
        WHERE ipcId = :ipcId
        AND (timestamp BETWEEN :start AND :end) 
        AND (message LIKE '%' || :query || '%' 
             OR requestId LIKE '%' || :query || '%' 
             OR cmd LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
    """)
    fun searchLogs(ipcId: String, start: String, end: String, query: String): Flow<List<LogEntity>>

    @Query("SELECT COUNT(*) FROM logs WHERE ipcId = :ipcId")
    suspend fun count(ipcId: String): Int

    @Query("DELETE FROM logs WHERE ipcId = :ipcId")
    suspend fun clearAll(ipcId: String)
}