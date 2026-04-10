package com.weyya.app.ui.privacy

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.weyya.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDashboardScreen(
    navController: NavController,
    viewModel: PrivacyDashboardViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val totalBlocked by viewModel.totalBlocked.collectAsStateWithLifecycle()
    val blockedThisMonth by viewModel.blockedThisMonth.collectAsStateWithLifecycle()
    val bypassCount by viewModel.bypassCount.collectAsStateWithLifecycle()
    val daysProtecting by viewModel.daysSinceFirstActivation.collectAsStateWithLifecycle()

    val contactsGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CONTACTS,
    ) == PackageManager.PERMISSION_GRANTED
    val phoneGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_PHONE_STATE,
    ) == PackageManager.PERMISSION_GRANTED

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_dashboard)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))

            // Hero
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.zero_bytes),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))

            // Stats
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StatRow(stringResource(R.string.total_blocked, totalBlocked, blockedThisMonth))
                    Spacer(Modifier.height(8.dp))
                    StatRow(stringResource(R.string.urgency_allowed, bypassCount))
                    Spacer(Modifier.height(8.dp))
                    StatRow(stringResource(R.string.days_protecting, daysProtecting))
                }
            }

            Spacer(Modifier.height(24.dp))

            // Permission audit
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PermissionRow(stringResource(R.string.permission_contacts), contactsGranted)
                    Spacer(Modifier.height(8.dp))
                    PermissionRow(stringResource(R.string.permission_phone), phoneGranted)
                    Spacer(Modifier.height(8.dp))
                    PermissionRow(stringResource(R.string.permission_no_internet), false)
                    Spacer(Modifier.height(8.dp))
                    PermissionRow(stringResource(R.string.permission_no_location), false)
                    Spacer(Modifier.height(8.dp))
                    PermissionRow(stringResource(R.string.permission_no_storage), false)
                    Spacer(Modifier.height(8.dp))
                    PermissionRow(stringResource(R.string.permission_no_camera), false)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatRow(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun PermissionRow(text: String, granted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = if (granted) Icons.Filled.Check else Icons.Filled.Close,
            contentDescription = null,
            tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp),
        )
    }
}
