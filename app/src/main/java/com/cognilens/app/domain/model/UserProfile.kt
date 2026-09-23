package com.cognilens.app.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

data class UserProfile(
    val wakeUpTime: LocalTime = LocalTime.of(7, 0),
    val sleepTime: LocalTime = LocalTime.of(23, 0),
    val morningBufferMinutes: Long = 30L, // Window after wake-up marked as immediate override
    val bedtimeBufferMinutes: Long = 30L, // Window before bedtime marked as wind-down zone
    val workDays: Set<DayOfWeek> = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    ),
    val workStartTime: LocalTime = LocalTime.of(9, 0),
    val workEndTime: LocalTime = LocalTime.of(17, 0),
    val bsmasScore: Int = 18 // Baseline score (6-30)
)