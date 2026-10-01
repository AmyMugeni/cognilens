package com.cognilens.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timetable_entries")
data class TimetableEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: Int,       // 1 = Monday ... 7 = Sunday (java.time.DayOfWeek.value)
    val startMinutes: Int,    // minutes since midnight, e.g. 10:30 = 630
    val endMinutes: Int,
    val courseCode: String,
    val courseName: String,
    val location: String
)