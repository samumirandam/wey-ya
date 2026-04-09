package com.weyya.app.ui.main

import android.app.role.RoleManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.weyya.app.R
import com.weyya.app.domain.model.BlockingMode
import com.weyya.app.ui.components.BigToggle
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

    val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.setHasScreeningRole(
            roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING),
        )
    }

    LaunchedEffect(Unit) {
        viewModel.setHasScreeningRole(
            roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
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
                    onToggle = viewModel::toggle,
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = when {
                        !isActive -> stringResource(R.string.status_off)
                        mode == BlockingMode.ALL_CALLERS -> stringResource(R.string.status_all)
                        else -> stringResource(R.string.status_unknown)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(24.dp))

                ModeSelector(
                    selectedMode = mode,
                    enabled = isActive,
                    onModeSelected = viewModel::setBlockingMode,
                )

                Spacer(Modifier.height(24.dp))

                StatCard(
                    blockedToday = blockedToday,
                    totalBlocked = totalBlocked,
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSelector(
    selectedMode: BlockingMode,
    enabled: Boolean,
    onModeSelected: (BlockingMode) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = selectedMode == BlockingMode.UNKNOWN_CALLERS,
            onClick = { onModeSelected(BlockingMode.UNKNOWN_CALLERS) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            enabled = enabled,
        ) {
            Text(stringResource(R.string.mode_unknown))
        }
        SegmentedButton(
            selected = selectedMode == BlockingMode.ALL_CALLERS,
            onClick = { onModeSelected(BlockingMode.ALL_CALLERS) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            enabled = enabled,
        ) {
            Text(stringResource(R.string.mode_all))
        }
    }
}
