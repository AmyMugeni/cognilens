package com.cognilens.app.domain.rules

import com.cognilens.app.domain.model.UserProfile
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

class RuleEngine {

    /**
     * Evaluates current session metadata against hard-coded circadian and schedule rules.
     *
     * @param currentTime Current timestamp (defaults to system time if omitted).
     * @param userProfile Configured profile containing wake/sleep boundaries and focus schedules.
     * @param sessionDurationMins Current active session duration in minutes.
     * @param reopenIntervalMins Time elapsed since target app was last closed.
     */
    fun evaluate(
        userProfile: UserProfile,
        currentTime: LocalDateTime = LocalDateTime.now(),
        sessionDurationMins: Float,
        reopenIntervalMins: Float
    ): RuleEvaluationResult {
        val time = currentTime.toLocalTime()
        val dayOfWeek = currentTime.dayOfWeek

        // -------------------------------------------------------------------
        // RULE 1: Morning Circadian Override (First thing upon waking up)
        // -------------------------------------------------------------------
        val wakeWindowEnd = userProfile.wakeUpTime.plusMinutes(userProfile.morningBufferMinutes)
        if (isTimeBetween(time, userProfile.wakeUpTime, wakeWindowEnd)) {
            return RuleEvaluationResult.TriggerIntervention(
                reason = "Circadian Override: App opened during ${userProfile.morningBufferMinutes}-min wake-up window."
            )
        }

        // -------------------------------------------------------------------
        // RULE 2: Sleep & Bedtime Override (Late night / Past bedtime)
        // -------------------------------------------------------------------
        val windDownStart = userProfile.sleepTime.minusMinutes(userProfile.bedtimeBufferMinutes)

        // Handles overnight transitions (e.g., 23:00 PM to 07:00 AM)
        if (isOvernightWindow(time, windDownStart, userProfile.wakeUpTime)) {
            return RuleEvaluationResult.TriggerIntervention(
                reason = "Circadian Override: Active during sleep or wind-down window."
            )
        }

        // -------------------------------------------------------------------
        // RULE 3: Extreme Session Loss of Control (Hard Time Limit Safety Net)
        // -------------------------------------------------------------------
        // Any continuous social session exceeding 45 minutes is a forced override
        if (sessionDurationMins >= 45f) {
            return RuleEvaluationResult.TriggerIntervention(
                reason = "Session Limit Override: Continuous usage exceeded 45 minutes."
            )
        }

        // -------------------------------------------------------------------
        // RULE 4: Rapid Relapse Loop (Re-opening app within 90 seconds)
        // -------------------------------------------------------------------
        if (reopenIntervalMins in 0.01f..1.5f && sessionDurationMins > 1f) {
            return RuleEvaluationResult.TriggerIntervention(
                reason = "Relapse Override: Immediate re-opening loop detected (< 90s)."
            )
        }

        // No hard rules tripped -> Pass telemetry to ONNX Gradient Boosting ML model
        return RuleEvaluationResult.PassToML
    }

    /**
     * Calculates whether current session falls into user's designated Work/Study schedule.
     * Used as a input feature ($X_4$) when evaluating the ML model.
     */
    fun isScheduleConflict(userProfile: UserProfile, currentTime: LocalDateTime = LocalDateTime.now()): Float {
        val day = currentTime.dayOfWeek
        val time = currentTime.toLocalTime()

        val isWorkDay = userProfile.workDays.contains(day)
        val isWorkHours = isTimeBetween(time, userProfile.workStartTime, userProfile.workEndTime)

        return if (isWorkDay && isWorkHours) 1.0f else 0.0f
    }

    // Helper functions for time handling across midnight boundaries
    private fun isTimeBetween(target: LocalTime, start: LocalTime, end: LocalTime): Boolean {
        return if (start.isBefore(end)) {
            !target.isBefore(start) && !target.isAfter(end)
        } else {
            !target.isBefore(start) || !target.isAfter(end)
        }
    }

    private fun isOvernightWindow(target: LocalTime, sleepStart: LocalTime, wakeTime: LocalTime): Boolean {
        return if (sleepStart.isAfter(wakeTime)) {
            // Standard overnight schedule (e.g. 22:30 to 07:00)
            target.isAfter(sleepStart) || target.isBefore(wakeTime)
        } else {
            target.isAfter(sleepStart) && target.isBefore(wakeTime)
        }
    }
}