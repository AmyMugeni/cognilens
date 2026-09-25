package com.cognilens.app.ui.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cognilens.app.data.tracker.AppUsageTrackerService
import com.cognilens.app.data.tracker.PermissionUtils
import kotlinx.coroutines.launch
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = viewModel(),
    onOnboardingComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()
    val userProfile by viewModel.userProfile.collectAsState()
    val bsmasAnswers by viewModel.bsmasAnswers.collectAsState()

    Scaffold(
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pagerState.currentPage > 0) {
                        OutlinedButton(onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }) {
                            Text("Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(80.dp))
                    }

                    // Page Indicator Dots
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(4) { page ->
                            val color = if (pagerState.currentPage == page)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outlineVariant
                            Box(
                                modifier = Modifier
                                    .size(if (pagerState.currentPage == page) 12.dp else 8.dp)
                                    .background(color, shape = RoundedCornerShape(50))
                            )
                        }
                    }

                    if (pagerState.currentPage < 3) {
                        Button(onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }) {
                            Text("Next")
                        }
                    } else {
                        Button(onClick = {
                            viewModel.completeOnboarding {
                                onOnboardingComplete()
                            }
                        }) {
                            Text("Finish")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (page) {
                0 -> WelcomeStep()
                1 -> ScheduleStep(
                    userProfile = userProfile,
                    onWakeTimeChanged = viewModel::updateWakeUpTime,
                    onSleepTimeChanged = viewModel::updateSleepTime
                )
                2 -> BSMASStep(
                    answers = bsmasAnswers,
                    onAnswerSelected = viewModel::updateBsmasAnswer
                )
                3 -> PermissionsStep()
            }
        }
    }
}

// -------------------------------------------------------------------
// STEP 1: WELCOME & MISSION
// -------------------------------------------------------------------
@Composable
fun WelcomeStep() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to CogniLens",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Mindful, Intentional Digital Habits",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(32.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "100% On-Device & Private",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "CogniLens detects compulsive usage loops entirely on your phone using standard system APIs. No personal data ever leaves your device.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// -------------------------------------------------------------------
// STEP 2: CIRCADIAN & SCHEDULE SETUP
// -------------------------------------------------------------------
@Composable
fun ScheduleStep(
    userProfile: com.cognilens.app.domain.model.UserProfile,
    onWakeTimeChanged: (LocalTime) -> Unit,
    onSleepTimeChanged: (LocalTime) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Set Your Boundaries",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "CogniLens uses these hours as a zero-latency guardrail to protect your mornings and evenings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Wake Up Time Selector (Simple increment controls for demo/preview)
        TimePickerCard(
            title = "Wake-Up Time",
            time = userProfile.wakeUpTime,
            onHourChange = { hour -> onWakeTimeChanged(LocalTime.of(hour, userProfile.wakeUpTime.minute)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Bedtime Selector
        TimePickerCard(
            title = "Target Bedtime",
            time = userProfile.sleepTime,
            onHourChange = { hour -> onSleepTimeChanged(LocalTime.of(hour, userProfile.sleepTime.minute)) }
        )
    }
}

@Composable
fun TimePickerCard(
    title: String,
    time: LocalTime,
    onHourChange: (Int) -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = String.format("%02d:%02d", time.hour, time.minute),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onHourChange((time.hour - 1 + 24) % 24) }) {
                    Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Text("Hour", modifier = Modifier.padding(horizontal = 4.dp))
                IconButton(onClick = { onHourChange((time.hour + 1) % 24) }) {
                    Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------------
// STEP 3: BSMAS QUESTIONNAIRE
// -------------------------------------------------------------------
private val bsmasQuestions = listOf(
    "1. You spend a lot of time thinking about social media or planning to use it.",
    "2. You feel an urge to use social media more and more.",
    "3. You use social media to forget about personal problems.",
    "4. You have tried to cut down on social media without success.",
    "5. You become restless or troubled if prohibited from social media.",
    "6. You use social media so much that it has had a negative impact on your work/studies."
)

@Composable
fun BSMASStep(
    answers: IntArray,
    onAnswerSelected: (Int, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Baseline Assessment",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Rate each statement (1 = Very Rarely, 5 = Very Often) to calibrate the ML model.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        bsmasQuestions.forEachIndexed { index, question ->
            Text(text = question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (1..5).forEach { score ->
                    FilterChip(
                        selected = answers[index] == score,
                        onClick = { onAnswerSelected(index, score) },
                        label = { Text(score.toString()) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// -------------------------------------------------------------------
// STEP 4: PERMISSIONS GRANT
// -------------------------------------------------------------------
@Composable
fun PermissionsStep() {
    val context = LocalContext.current
    var hasUsagePermission by remember { mutableStateOf(PermissionUtils.hasUsageStatsPermission(context)) }
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Grant System Access",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Two permissions are required for CogniLens to monitor session velocity and show interventions.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Permission 1: Usage Access
        PermissionCard(
            title = "1. Usage Access Permission",
            description = "Allows CogniLens to detect when social media apps open in the foreground.",
            isGranted = hasUsagePermission,
            onRequestPermission = {
                PermissionUtils.openUsageStatsSettings(context)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Permission 2: Display Over Other Apps
        PermissionCard(
            title = "2. System Alert Window",
            description = "Allows CogniLens to display in-session micro-interventions over target apps.",
            isGranted = hasOverlayPermission,
            onRequestPermission = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Auto-Start Tracking Service Button if permissions granted
        if (hasUsagePermission) {
            Button(
                onClick = {
                    val serviceIntent = Intent(context, AppUsageTrackerService::class.java)
                    ContextCompat.startForegroundService(context, serviceIntent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Protection Service Now")
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(12.dp))
            if (isGranted) {
                Text(" Granted", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            } else {
                Button(onClick = onRequestPermission) {
                    Text("Enable in Settings")
                }
            }
        }
    }
}