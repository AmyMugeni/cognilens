package com.cognilens.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.cognilens.app.data.local.entity.TimetableEntry
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TimetableDao {
    @Query("SELECT * FROM timetable_entries ORDER BY dayOfWeek, startMinutes")
    abstract fun observeAll(): Flow<List<TimetableEntry>>

    @Query("SELECT COUNT(*) FROM timetable_entries")
    abstract fun observeCount(): Flow<Int>

    @Query("DELETE FROM timetable_entries")
    abstract suspend fun clear()

    @Insert
    abstract suspend fun insertAll(items: List<TimetableEntry>)

    @Transaction
    open suspend fun replaceAll(items: List<TimetableEntry>) {
        clear()
        insertAll(items)
    }
}
