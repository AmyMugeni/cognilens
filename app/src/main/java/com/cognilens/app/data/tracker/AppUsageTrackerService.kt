package com.cognilens.app.data.tracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.cognilens.app.domain.model.UserProfile
import com.cognilens.app.domain.rules.RuleEngine
import com.cognilens.app.domain.rules.RuleEvaluationResult
import com.cognilens.app.ml.models.ModelEvaluator
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.LocalTime

class AppUsageTrackerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var ruleEngine: RuleEngine
    private lateinit var modelEvaluator: ModelEvaluator

    private var currentForegroundApp: String? = null
    private var sessionStartTime: Long = 0L
    private var lastClosedTime: Long = 0L

    private val monitoredApps = setOf(
        "com.instagram.android",
        "com.zhiliaoapp.musically", // TikTok
        "com.twitter.android",
        "com.snapchat.android"
    )

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        ruleEngine = RuleEngine()
        modelEvaluator = ModelEvaluator(applicationContext)

        startForeground(NOTIFICATION_ID, createNotification())
        startPollingLoop()
    }

    private fun startPollingLoop() {
        serviceScope.launch {
            while (isActive) {
                checkForegroundApp()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun checkForegroundApp() {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (POLL_INTERVAL_MS * 2)

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            // Detect App Launch
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                val packageName = event.packageName

                if (monitoredApps.contains(packageName) && packageName != currentForegroundApp) {
                    currentForegroundApp = packageName
                    val now = System.currentTimeMillis()

                    val reopenIntervalMins = if (lastClosedTime > 0) {
                        ((now - lastClosedTime) / 60000f)
                    } else {
                        60f
                    }

                    sessionStartTime = now
                    // FIX: Fully explicitly named arguments to prevent mixing positional/named errors
                    evaluateRealtimeSession(
                        packageName = packageName,
                        durationMins = 0f,
                        reopenIntervalMins = reopenIntervalMins
                    )
                }
            }

            // Detect App Close / Backgrounding
            if (event.eventType == UsageEvents.Event.ACTIVITY_PAUSED ||
                event.eventType == UsageEvents.Event.ACTIVITY_STOPPED) {
                if (event.packageName == currentForegroundApp) {
                    currentForegroundApp = null
                    lastClosedTime = System.currentTimeMillis()
                }
            }
        }

        // Active In-Session Progress Monitor
        currentForegroundApp?.let { app ->
            val activeDurationMins = ((System.currentTimeMillis() - sessionStartTime) / 60000f)
            val reopenIntervalMins = if (lastClosedTime > 0) {
                ((sessionStartTime - lastClosedTime) / 60000f)
            } else {
                60f
            }

            evaluateRealtimeSession(
                packageName = app,
                durationMins = activeDurationMins,
                reopenIntervalMins = reopenIntervalMins
            )
        }
    }

    private fun evaluateRealtimeSession(
        packageName: String,
        durationMins: Float,
        reopenIntervalMins: Float
    ) {
        val currentTime = LocalDateTime.now()

        // Placeholder profile until database repository is wired
        val userProfile = UserProfile(
            wakeUpTime = LocalTime.of(7, 0),
            sleepTime = LocalTime.of(23, 0),
            bsmasScore = 22
        )

        // 1. HARD RULE CHECK (Circadian Override)
        when (val ruleResult = ruleEngine.evaluate(userProfile, currentTime, durationMins, reopenIntervalMins)) {
            is RuleEvaluationResult.TriggerIntervention -> {
                triggerInterventionOverlay(packageName, reason = ruleResult.reason)
                return
            }
            is RuleEvaluationResult.PassToML -> {
                // 2. GRADIENT BOOSTING ML EVALUATION
                val scheduleConflictFeature = ruleEngine.isScheduleConflict(userProfile, currentTime)

                val isCompulsive = modelEvaluator.evaluateSession(
                    sessionDurationMins = durationMins,
                    reopenIntervalMins = reopenIntervalMins,
                    bsmasScore = userProfile.bsmasScore.toFloat(),
                    isScheduleConflict = scheduleConflictFeature
                )

                if (isCompulsive) {
                    triggerInterventionOverlay(packageName, reason = "ML Model: Compulsive pattern detected.")
                }
            }
        }
    }

    private fun triggerInterventionOverlay(packageName: String, reason: String) {
        println("🚨 INTERVENTION TRIGGERED for $packageName. Reason: $reason")
    }

    private fun createNotification(): Notification {
        val channelId = "cognilens_tracker_channel"
        val channelName = "CogniLens Active Protection"

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("CogniLens Active")
            .setContentText("Monitoring digital wellness in the background.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val POLL_INTERVAL_MS = 2000L
    }
}