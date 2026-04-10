package com.weyya.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.weyya.app.R
import com.weyya.app.data.db.entity.ScheduleEntity
import kotlin.math.roundToInt

private val dayNames = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val threshold by viewModel.attemptThreshold.collectAsStateWithLifecycle()
    val windowMinutes by viewModel.timeWindowMinutes.collectAsStateWithLifecycle()
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<ScheduleEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // --- Persistence section ---
            item {
                SectionHeader(stringResource(R.string.persistence_section))
            }

            item {
                Text(
                    text = stringResource(R.string.threshold_label, threshold),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Slider(
                    value = threshold.toFloat(),
                    onValueChange = { viewModel.setThreshold(it.roundToInt()) },
                    valueRange = 2f..5f,
                    steps = 2,
                )
            }

            item {
                Text(
                    text = stringResource(R.string.window_label, windowMinutes),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Slider(
                    value = windowMinutes.toFloat(),
                    onValueChange = { viewModel.setWindowMinutes(it.roundToInt()) },
                    valueRange = 3f..10f,
                    steps = 6,
                )
            }

            // --- Schedules section ---
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionHeader(stringResource(R.string.schedules_section))
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_schedule))
                    }
                }
            }

            if (schedules.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_schedules_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(schedules, key = { it.id }) { schedule ->
                ScheduleItem(
                    schedule = schedule,
                    onToggle = { viewModel.toggleSchedule(schedule) },
                    onDelete = { viewModel.deleteSchedule(schedule) },
                    onEdit = { editingSchedule = schedule },
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showAddDialog) {
        ScheduleDialog(
            title = stringResource(R.string.add_schedule),
            onDismiss = { showAddDialog = false },
            onConfirm = { entities ->
                viewModel.addSchedules(entities)
                showAddDialog = false
            },
        )
    }

    editingSchedule?.let { schedule ->
        ScheduleDialog(
            title = stringResource(R.string.edit_schedule),
            initialDays = setOf(schedule.dayOfWeek),
            initialStartHour = schedule.startTime.substringBefore(":").toInt(),
            initialStartMinute = schedule.startTime.substringAfter(":").toInt(),
            initialEndHour = schedule.endTime.substringBefore(":").toInt(),
            initialEndMinute = schedule.endTime.substringAfter(":").toInt(),
            singleEditMode = true,
            onDismiss = { editingSchedule = null },
            onConfirm = { entities ->
                val updated = entities.first()
                viewModel.updateSchedule(schedule.copy(
                    dayOfWeek = updated.dayOfWeek,
                    startTime = updated.startTime,
                    endTime = updated.endTime,
                ))
                editingSchedule = null
            },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun ScheduleItem(
    schedule: ScheduleEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(
            containerColor = if (schedule.enabled)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dayNames.getOrElse(schedule.dayOfWeek - 1) { "?" },
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "${schedule.startTime} – ${schedule.endTime}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Switch(
                checked = schedule.enabled,
                onCheckedChange = { onToggle() },
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ScheduleDialog(
    title: String,
    initialDays: Set<Int> = emptySet(),
    initialStartHour: Int = 22,
    initialStartMinute: Int = 0,
    initialEndHour: Int = 7,
    initialEndMinute: Int = 0,
    singleEditMode: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (List<ScheduleEntity>) -> Unit,
) {
    val selectedDays = remember { initialDays.toMutableStateList() }
    var startHour by remember { mutableIntStateOf(initialStartHour) }
    var startMinute by remember { mutableIntStateOf(initialStartMinute) }
    var endHour by remember { mutableIntStateOf(initialEndHour) }
    var endMinute by remember { mutableIntStateOf(initialEndMinute) }

    val crossesMidnight = endHour < startHour || (endHour == startHour && endMinute < startMinute)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Day selector — multi-select chips
                Text(
                    text = if (singleEditMode) stringResource(R.string.schedule_day)
                    else stringResource(R.string.schedule_days),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    dayNames.forEachIndexed { index, name ->
                        val day = index + 1
                        val isSelected = day in selectedDays
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                )
                                .clickable {
                                    if (singleEditMode) {
                                        selectedDays.clear()
                                        selectedDays.add(day)
                                    } else {
                                        if (isSelected) selectedDays.remove(day)
                                        else selectedDays.add(day)
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = name.take(2),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }

                // Start time
                Text(stringResource(R.string.schedule_start), style = MaterialTheme.typography.labelLarge)
                TimeSliders(
                    hour = startHour,
                    minute = startMinute,
                    onHourChange = { startHour = it },
                    onMinuteChange = { startMinute = it },
                )

                // End time
                Text(stringResource(R.string.schedule_end), style = MaterialTheme.typography.labelLarge)
                TimeSliders(
                    hour = endHour,
                    minute = endMinute,
                    onHourChange = { endHour = it },
                    onMinuteChange = { endMinute = it },
                )

                // Midnight-crossing hint
                if (crossesMidnight) {
                    Text(
                        text = stringResource(R.string.schedule_crosses_midnight),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val entities = selectedDays.map { day ->
                        ScheduleEntity(
                            dayOfWeek = day,
                            startTime = "%02d:%02d".format(startHour, startMinute),
                            endTime = "%02d:%02d".format(endHour, endMinute),
                        )
                    }
                    onConfirm(entities)
                },
                enabled = selectedDays.isNotEmpty(),
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun TimeSliders(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
) {
    Column {
        Text("%02d:%02d".format(hour, minute), style = MaterialTheme.typography.headlineSmall)
        Slider(
            value = hour.toFloat(),
            onValueChange = { onHourChange(it.roundToInt()) },
            valueRange = 0f..23f,
            steps = 22,
        )
        Slider(
            value = minute.toFloat(),
            onValueChange = { onMinuteChange(it.roundToInt()) },
            valueRange = 0f..55f,
            steps = 10,
        )
    }
}
