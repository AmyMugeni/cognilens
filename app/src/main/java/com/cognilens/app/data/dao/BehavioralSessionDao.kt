package com.cognilens.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cognilens.app.data.entity.BehavioralSession
import kotlinx.coroutines.flow.Flow

@Dao
interface BehavioralSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: BehavioralSession): Long

    @Query("SELECT * FROM behavioral_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<BehavioralSession>>

    @Query("SELECT * FROM behavioral_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): BehavioralSession?

    @Query("SELECT COUNT(*) FROM behavioral_sessions WHERE timestamp >= :startOfDayTimestamp")
    suspend fun getSessionCountToday(startOfDayTimestamp: Long): Int

    @Delete
    suspend fun deleteSession(session: BehavioralSession)

    @Query("DELETE FROM behavioral_sessions")
    suspend fun deleteAllSessions()
}
