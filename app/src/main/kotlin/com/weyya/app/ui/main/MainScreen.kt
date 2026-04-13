package com.weyya.app.ui.main

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.weyya.app.R
import com.weyya.app.domain.model.BlockingMode
import com.weyya.app.ui.components.BigToggle
import com.weyya.app.navigation.Routes
import com.weyya.app.ui.components.StatCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val isActive by viewModel.isActive.collectAsStateWithLifecycle()
    val mode by viewModel.blockingMode.collectAsStateWithLifecycle()
    val blockedToday by viewModel.blockedToday.collectAsStateWithLifecycle()
    val totalBlocked by viewModel.totalBlocked.collectAsStateWithLifecycle()
    val hasRole by viewModel.hasScreeningRole.collectAsStateWithLifecycle()
    val isWithinSchedule by viewModel.isWithinSchedule.collectAsStateWithLifecycle()
    val batteryDismissed by viewModel.batteryDismissed.collectAsStateWithLifecycle()

    val roleManager = context.getSystemService(RoleManager::class.java)
    val powerManager = context.getSystemService(PowerManager::class.java)

    // Permission state
    var contactsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var permissionRequested by remember { mutableStateOf(false) }
    var batteryOptimized by remember {
        mutableStateOf(!powerManager.isIgnoringBatteryOptimizations(context.packageName))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        contactsGranted = results[Manifest.permission.READ_CONTACTS] == true
        permissionRequested = true
    }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.setHasScreeningRole(
            roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING),
        )
    }

    // Re-check on resume (user may return from Settings with permission granted)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.setHasScreeningRole(
            roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING),
        )
        contactsGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
        batteryOptimized = !powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    // Request permissions after role is granted
    LaunchedEffect(hasRole) {
        if (hasRole && !contactsGranted && !permissionRequested) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.READ_PHONE_STATE),
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.PRIVACY) }) {
                        Icon(Icons.Filled.Shield, contentDescription = stringResource(R.string.privacy_dashboard))
                    }
                    IconButton(onClick = { navController.navigate(Routes.LOG) }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.call_log))
                    }
                    IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!hasRole) {
                RoleRequestCard(
                    onRequest = {
                        if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                            roleLauncher.launch(
                                roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING),
                            )
                        }
                    },
                )
            } else {
                Spacer(Modifier.height(32.dp))

                BigToggle(
                    isActive = isActive,
                    mode = mode,
                    isWithinSchedule = isWithinSchedule,
                    onToggle = viewModel::toggle,
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = when {
                        !isActive -> stringResource(R.string.status_off)
                        !isWithinSchedule -> stringResource(R.string.status_outside_schedule)
                        mode == BlockingMode.ALL_CALLERS -> stringResource(R.string.status_all)
                        else -> stringResource(R.string.status_unknown)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = if (isActive && !isWithinSchedule)
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(24.dp))

                ModeSelector(
                    selectedMode = mode,
                    isActive = isActive,
                    onModeSelected = { selectedMode ->
                        if (!isActive) {
                            viewModel.setBlockingMode(selectedMode)
                            viewModel.toggle()
                        } else if (selectedMode == mode) {
                            viewModel.toggle()
                        } else {
                            viewModel.setBlockingMode(selectedMode)
                        }
                    },
                )

                Spacer(Modifier.height(24.dp))

                StatCard(
                    blockedToday = blockedToday,
                    totalBlocked = totalBlocked,
                )

                if (!contactsGranted && permissionRequested) {
                    Spacer(Modifier.height(16.dp))
                    PermissionDeniedCard(context)
                }

                if (batteryOptimized && !batteryDismissed) {
                    Spacer(Modifier.height(16.dp))
                    BatteryOptimizationCard(
                        context = context,
                        onDismiss = { viewModel.dismissBattery() },
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun RoleRequestCard(onRequest: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.role_needed_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.role_needed_description),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onRequest) {
                Text(stringResource(R.string.role_needed_button))
            }
        }
    }
}

@Composable
private fun PermissionDeniedCard(context: Context) {
    val activity = context as? androidx.activity.ComponentActivity
    val canShowRationale = activity?.shouldShowRequestPermissionRationale(
        Manifest.permission.READ_CONTACTS,
    ) ?: false

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.permission_contacts_denied),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            if (canShowRationale) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { /* state updates on ON_RESUME */ }
                OutlinedButton(onClick = {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.READ_PHONE_STATE),
                    )
                }) {
                    Text(stringResource(R.string.permission_retry))
                }
            } else {
                OutlinedButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        },
                    )
                }) {
                    Text(stringResource(R.string.permission_open_settings))
                }
            }
        }
    }
}

@Composable
private fun BatteryOptimizationCard(context: Context, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.battery_optimization_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.padding(0.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.battery_optimization_description),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        },
                    )
                }) {
                    Text(stringResource(R.string.battery_optimization_button))
                }
                Text(
                    text = stringResource(R.string.battery_oem_help),
                    style = MaterialTheme.typography.bodySmall.copy(
                        textDecoration = TextDecoration.Underline,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://dontkillmyapp.com")),
                        )
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSelector(
    selectedMode: BlockingMode,
    isActive: Boolean,
    onModeSelected: (BlockingMode) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = isActive && selectedMode == BlockingMode.UNKNOWN_CALLERS,
            onClick = { onModeSelected(BlockingMode.UNKNOWN_CALLERS) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        ) {
            Text(stringResource(R.string.mode_unknown), maxLines = 1)
        }
        SegmentedButton(
            selected = isActive && selectedMode == BlockingMode.ALL_CALLERS,
            onClick = { onModeSelected(BlockingMode.ALL_CALLERS) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        ) {
            Text(stringResource(R.string.mode_all), maxLines = 1)
        }
    }
}
