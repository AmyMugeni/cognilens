package com.cognilens.app.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cognilens.app.data.preferences.ImportState
import com.cognilens.app.data.preferences.TimetableImportViewModel
import com.cognilens.app.domain.model.UserProfile
import java.time.LocalTime

private val bsmasQuestions = listOf(
    "1. You spend a lot of time thinking about social media or planning to use it.",
    "2. You feel an urge to use social media more and more.",
    "3. You use social media to forget about personal problems.",
    "4. You have tried to cut down on social media without success.",
    "5. You become restless or troubled if prohibited from social media.",
    "6. You use social media so much that it has had a negative impact on your work/studies."
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupHubScreen(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState(initial = UserProfile())
    val bsmasAnswers by viewModel.bsmasAnswers.collectAsState(initial = IntArray(6) { 3 })
    val context = LocalContext.current
    val importVm: TimetableImportViewModel = viewModel(factory = TimetableImportViewModel.factory(context))
    val importState by importVm.state.collectAsState()
    val classCount by importVm.entryCount.collectAsState(initial = 0)
    
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importVm.import(uri, context.contentResolver) { 
                viewModel.updateTimetableStatus(true) 
            }
        }
    }

    val progress = viewModel.calculateCompletionPercentage()
    val isComplete = progress >= 1.0f
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Completion header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = if (isComplete) colors.primaryContainer else colors.surfaceContainerLow
            ),
            border = BorderStroke(
                1.dp,
                if (isComplete) colors.primary else colors.outlineVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Profile Completion",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isComplete) colors.onPrimaryContainer else colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isComplete) "All requirements met!" else "Complete all 4 tasks to unlock CogniLens",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isComplete) colors.onPrimaryContainer else colors.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(64.dp),
                        color = colors.primary,
                        trackColor = if (isComplete) colors.surface else colors.surfaceVariant,
                        strokeWidth = 7.dp,
                        strokeCap = StrokeCap.Round
                    )
                    if (isComplete) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 1. Username
        SectionHeader(number = 1, title = "Personal Identification")
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = userProfile.username,
            onValueChange = viewModel::updateUsername,
            label = { Text("Username / Student ID") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surfaceContainerLow,
                unfocusedContainerColor = colors.surfaceContainerLow,
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.onSurfaceVariant.copy(alpha = 0.5f),
                focusedLabelColor = colors.primary,
                unfocusedLabelColor = colors.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 2. Sleep schedule
        SectionHeader(number = 2, title = "Circadian Boundaries")
        Spacer(modifier = Modifier.height(12.dp))
        TimePickerCard(
            title = "Wake-Up Time",
            time = userProfile.wakeUpTime,
            onTimeChange = { viewModel.updateWakeUpTime(it) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        TimePickerCard(
            title = "Target Bedtime",
            time = userProfile.sleepTime,
            onTimeChange = { viewModel.updateSleepTime(it) }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 3. Timetable
        SectionHeader(number = 3, title = "Academic Timetable")
        Spacer(modifier = Modifier.height(12.dp))
        TimetableImportCard(
            configured = userProfile.isTimetableConfigured,
            state = importState,
            classCount = classCount,
            onChoose = { picker.launch(arrayOf("application/pdf", "image/*")) },
            onMarkReadyWithoutFile = { viewModel.updateTimetableStatus(true) }
        )
        Spacer(modifier = Modifier.height(28.dp))
        // 4. BSMAS
        SectionHeader(
            number = 4,
            title = "Baseline Assessment (BSMAS)",
            subtitle = "Rate each statement (1 = Very Rarely, 5 = Very Often)"
        )
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
            border = BorderStroke(1.dp, colors.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column {
                bsmasQuestions.forEachIndexed { index, question ->
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Text(
                            text = question,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            (1..5).forEach { score ->
                                FilterChip(
                                    selected = bsmasAnswers.getOrElse(index) { 3 } == score,
                                    onClick = { viewModel.updateBsmasAnswer(index, score) },
                                    label = {
                                        Text(
                                            text = score.toString(),
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.primary,
                                        selectedLabelColor = colors.onPrimary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp)
                                )
                            }
                        }
                    }
                    if (index < bsmasQuestions.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = colors.outlineVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Final button
        Button(
            onClick = {
                viewModel.completeOnboarding {
                    onComplete()
                }
            },
            enabled = isComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                disabledContainerColor = colors.surfaceVariant,
                disabledContentColor = colors.onSurfaceVariant.copy(alpha = 0.6f)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text(
                text = if (isComplete) "Complete Setup & Launch" else "Complete All 4 Sections (${(progress * 100).toInt()}%)",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SectionHeader(
    number: Int,
    title: String,
    subtitle: String? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TimetableImportCard(
    configured: Boolean,
    state: ImportState,
    classCount: Int,
    onChoose: () -> Unit,
    onMarkReadyWithoutFile: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val working = state is ImportState.Working
    val error = (state as? ImportState.Failed)?.message

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (configured && error == null) colors.primaryContainer.copy(alpha = 0.35f)
            else colors.surfaceContainerLow
        ),
        border = BorderStroke(
            width = if (configured || error != null) 1.5.dp else 1.dp,
            color = when {
                error != null -> colors.error
                configured -> colors.primary
                else -> colors.outlineVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (configured && !working) colors.primary else colors.surfaceVariant,
                            CircleShape
                        )
                ) {
                    when {
                        working -> CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = colors.primary
                        )
                        configured -> Icon(
                            Icons.Filled.Check, contentDescription = null,
                            tint = colors.onPrimary, modifier = Modifier.size(20.dp)
                        )
                        else -> Icon(
                            Icons.Filled.DateRange, contentDescription = null,
                            tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Timetable", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                    Text(
                        text = when {
                            working -> "Reading your timetable..."
                            error != null -> error
                            configured && classCount > 0 -> "$classCount classes saved"
                            configured -> "Marked as ready"
                            else -> "Not configured"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = when {
                            error != null -> colors.error
                            configured && !working -> colors.primary
                            else -> colors.onSurfaceVariant
                        }
                    )
                }
            }

            if (!configured && !working) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Upload a PDF or a clear photo of your timetable. It is read automatically and turned into your weekly schedule.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }

            if (!working) {
                Spacer(modifier = Modifier.height(16.dp))
                if (configured) {
                    OutlinedButton(
                        onClick = onChoose,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.height(44.dp)
                    ) { Text("Replace timetable", style = MaterialTheme.typography.labelLarge) }
                } else {
                    FilledTonalButton(
                        onClick = onChoose,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = colors.primaryContainer,
                            contentColor = colors.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (error != null) "Try again" else "Choose PDF or photo",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    TextButton(
                        onClick = onMarkReadyWithoutFile,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Mark ready without a file", style = MaterialTheme.typography.labelLarge) }
                    Text(
                        text = "The file is sent to an AI service to be read. The app keeps only the extracted classes, on your device.",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerCard(
    title: String,
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var showDialog by remember { mutableStateOf(false) }

    Card(
        onClick = { showDialog = true },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        border = BorderStroke(1.dp, colors.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                Text(
                    text = String.format("%02d:%02d", time.hour, time.minute),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.primary
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(44.dp).background(colors.primaryContainer, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Change $title",
                    tint = colors.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showDialog) {
        val state = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = state)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onTimeChange(LocalTime.of(state.hour, state.minute))
                    showDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }
}
