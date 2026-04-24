package com.weyya.app.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.weyya.app.R
import com.weyya.app.data.db.entity.ScheduleEntity
import com.weyya.app.data.db.entity.WhitelistEntity
import com.weyya.app.data.telephony.SimInfo
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val threshold by viewModel.attemptThreshold.collectAsStateWithLifecycle()
    val windowMinutes by viewModel.timeWindowMinutes.collectAsStateWithLifecycle()
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val whitelist by viewModel.whitelist.collectAsStateWithLifecycle()
    val activeSims by viewModel.activeSims.collectAsStateWithLifecycle()

    val dayNames = stringArrayResource(R.array.day_abbreviations).toList()

    val context = androidx.compose.ui.platform.LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<ScheduleEntity?>(null) }
    var showAddWhitelistDialog by remember { mutableStateOf(false) }
    var phonePermissionGranted by remember { mutableStateOf(viewModel.hasPhonePermission()) }

    val phonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        phonePermissionGranted = granted
        viewModel.refreshSims()
    }

    val isDualSim = viewModel.hasDualSim

    // Re-check permission on resume so external grants/revocations (system settings) are picked up.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        phonePermissionGranted = viewModel.hasPhonePermission()
    }

    // Refresh on every permission transition so a revocation clears the stale SIM list
    // (refreshSims returns an empty list when the permission is missing).
    LaunchedEffect(phonePermissionGranted) {
        if (isDualSim) {
            viewModel.refreshSims()
        }
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact(),
    ) { uri ->
        if (uri != null) {
            val contactId = context.contentResolver.query(
                uri, arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME),
                null, null, null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)) ?: ""
                    Pair(id, name)
                } else null
            }
            if (contactId != null) {
                val phoneCursor = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId.first),
                    null,
                )
                phoneCursor?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val phone = cursor.getString(
                            cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER),
                        )
                        if (!phone.isNullOrBlank()) {
                            viewModel.addToWhitelist(phone.trim(), contactId.second)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // --- Persistence section ---
            item {
                SectionHeader(stringResource(R.string.persistence_section))
            }

            item {
                var localThreshold by remember { mutableIntStateOf(threshold) }
                LaunchedEffect(threshold) { localThreshold = threshold }
                Text(
                    text = stringResource(R.string.threshold_label, localThreshold),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Slider(
                    value = localThreshold.toFloat(),
                    onValueChange = { localThreshold = it.roundToInt() },
                    onValueChangeFinished = { viewModel.setThreshold(localThreshold) },
                    valueRange = 2f..5f,
                    steps = 2,
                )
            }

            item {
                var localWindow by remember { mutableIntStateOf(windowMinutes) }
                LaunchedEffect(windowMinutes) { localWindow = windowMinutes }
                Text(
                    text = stringResource(R.string.window_label, localWindow),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Slider(
                    value = localWindow.toFloat(),
                    onValueChange = { localWindow = it.roundToInt() },
                    onValueChangeFinished = { viewModel.setWindowMinutes(localWindow) },
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

            if (isDualSim && !phonePermissionGranted) {
                item {
                    DualSimPermissionCard(
                        onGrant = { phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE) },
                    )
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
                    dayNames = dayNames,
                    activeSims = activeSims,
                    onToggle = { viewModel.toggleSchedule(schedule) },
                    onDelete = { viewModel.deleteSchedule(schedule) },
                    onEdit = { editingSchedule = schedule },
                )
            }

            // --- Whitelist section ---
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionHeader(stringResource(R.string.whitelist_section))
                    Row {
                        IconButton(onClick = { contactPickerLauncher.launch(null) }) {
                            Icon(Icons.Filled.Contacts, contentDescription = stringResource(R.string.add_from_contacts))
                        }
                        IconButton(onClick = { showAddWhitelistDialog = true }) {
                            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_to_whitelist))
                        }
                    }
                }
            }

            if (whitelist.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_whitelist_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(whitelist, key = { it.phoneNumber }) { entry ->
                WhitelistItem(
                    entry = entry,
                    onDelete = { viewModel.removeFromWhitelist(entry.phoneNumber) },
                )
            }

            // --- About section ---
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader(stringResource(R.string.about_section))
            }

            item {
                AboutLink(
                    title = stringResource(R.string.about_report_bug),
                    subtitle = stringResource(R.string.about_report_bug_desc),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/samumirandam/wey-ya/issues"))
                        context.startActivity(intent)
                    },
                )
            }

            item {
                AboutLink(
                    title = stringResource(R.string.about_rate),
                    subtitle = stringResource(R.string.about_rate_desc),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.weyya.app"))
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.weyya.app")),
                            )
                        }
                    },
                )
            }

            item {
                AboutLink(
                    title = stringResource(R.string.about_github),
                    subtitle = stringResource(R.string.about_github_desc),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/samumirandam/wey-ya"))
                        context.startActivity(intent)
                    },
                )
            }

            item {
                val packageInfo = remember {
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
                Text(
                    text = stringResource(R.string.about_version, packageInfo.versionName ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Para mi madre, con todo el amor del mundo mundial \u2764\uFE0F",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    textAlign = TextAlign.Center,
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showAddDialog) {
        ScheduleDialog(
            title = stringResource(R.string.add_schedule),
            dayNames = dayNames,
            activeSims = activeSims,
            onDismiss = { showAddDialog = false },
            onConfirm = { entity ->
                viewModel.addSchedule(entity)
                showAddDialog = false
            },
        )
    }

    if (showAddWhitelistDialog) {
        AddWhitelistDialog(
            onDismiss = { showAddWhitelistDialog = false },
            onConfirm = { number, label ->
                viewModel.addToWhitelist(number, label)
                showAddWhitelistDialog = false
            },
        )
    }

    editingSchedule?.let { schedule ->
        ScheduleDialog(
            title = stringResource(R.string.edit_schedule),
            dayNames = dayNames,
            activeSims = activeSims,
            initialDays = schedule.daysList().toSet(),
            // Defensive parsing: fall back to 0 if a legacy/corrupt row stored a non-"HH:mm" value.
            initialStartHour = schedule.startTime.substringBefore(":").toIntOrNull() ?: 0,
            initialStartMinute = schedule.startTime.substringAfter(":").toIntOrNull() ?: 0,
            initialEndHour = schedule.endTime.substringBefore(":").toIntOrNull() ?: 0,
            initialEndMinute = schedule.endTime.substringAfter(":").toIntOrNull() ?: 0,
            initialSimSlot = schedule.simSlot,
            onDismiss = { editingSchedule = null },
            onConfirm = { updated ->
                viewModel.updateSchedule(schedule.copy(
                    daysOfWeek = updated.daysOfWeek,
                    startTime = updated.startTime,
                    endTime = updated.endTime,
                    simSlot = updated.simSlot,
                ))
                editingSchedule = null
            },
        )
    }
}

@Composable
private fun DualSimPermissionCard(onGrant: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.dual_sim_permission_rationale),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onGrant) {
                Text(stringResource(R.string.dual_sim_permission_cta))
            }
        }
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
    dayNames: List<String>,
    activeSims: List<SimInfo>,
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
                    text = schedule.daysList()
                        .map { dayNames.getOrElse(it - 1) { "?" } }
                        .joinToString(", "),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "${schedule.startTime} – ${schedule.endTime}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                // Only render when dual-SIM and this schedule targets a specific slot
                if (activeSims.size >= 2 && schedule.simSlot != null) {
                    val label = simLabel(schedule.simSlot, activeSims)
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
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
    dayNames: List<String>,
    activeSims: List<SimInfo>,
    initialDays: Set<Int> = emptySet(),
    initialStartHour: Int = 22,
    initialStartMinute: Int = 0,
    initialEndHour: Int = 7,
    initialEndMinute: Int = 0,
    initialSimSlot: Int? = null,
    onDismiss: () -> Unit,
    onConfirm: (ScheduleEntity) -> Unit,
) {
    val selectedDays = remember { initialDays.toMutableStateList() }
    var startHour by remember { mutableIntStateOf(initialStartHour) }
    var startMinute by remember { mutableIntStateOf(initialStartMinute) }
    var endHour by remember { mutableIntStateOf(initialEndHour) }
    var endMinute by remember { mutableIntStateOf(initialEndMinute) }
    var selectedSimSlot by remember { mutableStateOf(initialSimSlot) }

    val crossesMidnight = endHour < startHour || (endHour == startHour && endMinute < startMinute)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                // Day selector — multi-select chips
                Text(
                    text = stringResource(R.string.schedule_days),
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
                                    if (isSelected) selectedDays.remove(day)
                                    else selectedDays.add(day)
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

                // Start time — inverted track: color goes from thumb to right ("from here on")
                Text(stringResource(R.string.schedule_start), style = MaterialTheme.typography.labelLarge)
                TimeSliders(
                    hour = startHour,
                    minute = startMinute,
                    onHourChange = { startHour = it },
                    onMinuteChange = { startMinute = it },
                    invertedTrack = true,
                )

                // End time — normal track: color goes from left to thumb ("until here")
                Text(stringResource(R.string.schedule_end), style = MaterialTheme.typography.labelLarge)
                TimeSliders(
                    hour = endHour,
                    minute = endMinute,
                    onHourChange = { endHour = it },
                    onMinuteChange = { endMinute = it },
                    invertedTrack = false,
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

                // SIM selector — show when dual-SIM is detected, OR when the schedule being
                // edited already has a simSlot set (so the user can always clear the restriction
                // with "Ambos" even if the permission is denied or the original SIM is gone).
                val showSimSelector = activeSims.size >= 2 || selectedSimSlot != null
                if (showSimSelector) {
                    Text(
                        text = stringResource(R.string.schedule_applies_to),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SimChip(
                            label = stringResource(R.string.schedule_sim_both),
                            selected = selectedSimSlot == null,
                            onClick = { selectedSimSlot = null },
                        )
                        activeSims.forEach { sim ->
                            SimChip(
                                label = sim.carrierName.ifBlank {
                                    stringResource(R.string.schedule_sim_fallback, sim.slotIndex + 1)
                                },
                                selected = selectedSimSlot == sim.slotIndex,
                                onClick = { selectedSimSlot = sim.slotIndex },
                            )
                        }
                        // Stored slot no longer present in activeSims (SIM removed or permission
                        // denied): render a read-only chip so the user can see and change it.
                        val stored = selectedSimSlot
                        if (stored != null && activeSims.none { it.slotIndex == stored }) {
                            SimChip(
                                label = stringResource(R.string.schedule_sim_fallback, stored + 1),
                                selected = true,
                                onClick = {},
                                enabled = false,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        ScheduleEntity(
                            daysOfWeek = ScheduleEntity.daysToString(selectedDays),
                            startTime = "%02d:%02d".format(startHour, startMinute),
                            endTime = "%02d:%02d".format(endHour, endMinute),
                            simSlot = selectedSimSlot,
                        ),
                    )
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
private fun WhitelistItem(
    entry: WhitelistEntity,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
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
                    text = entry.phoneNumber,
                    style = MaterialTheme.typography.titleSmall,
                )
                if (entry.label.isNotBlank()) {
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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
private fun AddWhitelistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var number by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }

    val isValidPhone = remember(number) {
        if (number.isBlank()) false
        else number.trim().replace(Regex("[\\s\\-()]"), "").matches(Regex("^\\+?\\d{4,15}$"))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_whitelist)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text(stringResource(R.string.whitelist_number_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = number.isNotBlank() && !isValidPhone,
                    supportingText = if (number.isNotBlank() && !isValidPhone) {
                        { Text(stringResource(R.string.whitelist_invalid_number)) }
                    } else null,
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.whitelist_label_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(number.trim(), label.trim()) },
                enabled = isValidPhone,
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
private fun AboutLink(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SimChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun simLabel(slotIndex: Int, activeSims: List<SimInfo>): String {
    val match = activeSims.firstOrNull { it.slotIndex == slotIndex }
    return match?.carrierName?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.schedule_sim_fallback, slotIndex + 1)
}

@Composable
private fun TimeSliders(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    invertedTrack: Boolean = false,
) {
    val defaultColors = SliderDefaults.colors()
    val colors = if (invertedTrack) {
        SliderDefaults.colors(
            activeTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            inactiveTrackColor = MaterialTheme.colorScheme.primary,
            activeTickColor = MaterialTheme.colorScheme.onSurfaceVariant,
            inactiveTickColor = MaterialTheme.colorScheme.onPrimary,
        )
    } else {
        defaultColors
    }

    Column {
        Text("%02d:%02d".format(hour, minute), style = MaterialTheme.typography.headlineSmall)
        Slider(
            value = hour.toFloat(),
            onValueChange = { onHourChange(it.roundToInt()) },
            valueRange = 0f..23f,
            steps = 22,
            colors = colors,
        )
        Slider(
            value = minute.toFloat(),
            onValueChange = { onMinuteChange(it.roundToInt()) },
            valueRange = 0f..55f,
            steps = 10,
            colors = colors,
        )
    }
}
