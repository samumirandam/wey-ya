package com.weyya.app.ui.log

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.weyya.app.R
import com.weyya.app.data.db.entity.BlockedCallEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    navController: NavController,
    viewModel: LogViewModel = hiltViewModel(),
) {
    val allCalls by viewModel.allCalls.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val filteredCalls by remember(allCalls, filter) {
        derivedStateOf {
            val cutoff = filter.daysBack?.let { days ->
                val cal = Calendar.getInstance()
                if (days == 0) {
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                } else {
                    cal.add(Calendar.DAY_OF_YEAR, -days)
                }
                cal.timeInMillis
            }
            if (cutoff != null) allCalls.filter { it.timestamp >= cutoff } else allCalls
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportCsv(context, uri, filteredCalls)
            Toast.makeText(context, "CSV exportado", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.call_log)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        csvLauncher.launch("weyya_log.csv")
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.export_csv))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChipItem(stringResource(R.string.filter_today), filter == LogFilter.TODAY) {
                    viewModel.setFilter(LogFilter.TODAY)
                }
                FilterChipItem(stringResource(R.string.filter_week), filter == LogFilter.WEEK) {
                    viewModel.setFilter(LogFilter.WEEK)
                }
                FilterChipItem(stringResource(R.string.filter_month), filter == LogFilter.MONTH) {
                    viewModel.setFilter(LogFilter.MONTH)
                }
                FilterChipItem(stringResource(R.string.filter_all), filter == LogFilter.ALL) {
                    viewModel.setFilter(LogFilter.ALL)
                }
            }

            Spacer(Modifier.height(8.dp))

            if (filteredCalls.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_log_entries),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    items(filteredCalls, key = { it.id }) { call ->
                        CallLogItem(
                            call = call,
                            onAddToWhitelist = {
                                call.phoneNumber?.let { viewModel.addToWhitelist(it) }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
    )
}

@Composable
private fun CallLogItem(
    call: BlockedCallEntity,
    onAddToWhitelist: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    val dateStr = remember(call.timestamp) { dateFormat.format(Date(call.timestamp)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (call.wasEventuallyAllowed)
                MaterialTheme.colorScheme.tertiaryContainer
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
                    text = call.phoneNumber ?: stringResource(R.string.hidden_number),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "$dateStr — ${stringResource(R.string.attempts_format, call.attemptCount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (call.phoneNumber != null) {
                TextButton(onClick = onAddToWhitelist) {
                    Text(
                        text = stringResource(R.string.add_whitelist_action),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}
