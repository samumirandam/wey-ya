package com.weyya.app.ui.log

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weyya.app.data.db.dao.BlockedCallDao
import com.weyya.app.data.db.dao.WhitelistDao
import com.weyya.app.data.db.entity.BlockedCallEntity
import com.weyya.app.data.db.entity.WhitelistEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class LogFilter(val daysBack: Int?) {
    TODAY(0),
    WEEK(7),
    MONTH(30),
    ALL(null),
}

@HiltViewModel
class LogViewModel @Inject constructor(
    private val blockedCallDao: BlockedCallDao,
    private val whitelistDao: WhitelistDao,
) : ViewModel() {

    val allCalls: StateFlow<List<BlockedCallEntity>> = blockedCallDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _filter = MutableStateFlow(LogFilter.ALL)
    val filter: StateFlow<LogFilter> = _filter.asStateFlow()

    fun setFilter(f: LogFilter) {
        _filter.value = f
    }

    fun addToWhitelist(phoneNumber: String) {
        viewModelScope.launch {
            whitelistDao.insert(WhitelistEntity(phoneNumber = phoneNumber))
        }
    }

    fun clearHistory() {
        viewModelScope.launch { blockedCallDao.deleteAll() }
    }

    private val _exportError = MutableSharedFlow<String>()
    val exportError: SharedFlow<String> = _exportError.asSharedFlow()

    fun exportCsv(context: Context, uri: Uri, calls: List<BlockedCallEntity>) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                        writer.write("phone_number,timestamp,date_time,attempt_count,was_allowed")
                        writer.newLine()
                        calls.forEach { call ->
                            val number = csvField(call.phoneNumber ?: "hidden")
                            val date = csvField(dateFormat.format(Date(call.timestamp)))
                            writer.write("$number,${call.timestamp},$date,${call.attemptCount},${call.wasEventuallyAllowed}")
                            writer.newLine()
                        }
                    }
                }
            } catch (e: Exception) {
                _exportError.emit(e.message ?: "Export failed")
            }
        }
    }

    private fun csvField(value: String): String =
        if (value.contains(Regex("[,\"\n\r]"))) "\"${value.replace("\"", "\"\"")}\"" else value
}
