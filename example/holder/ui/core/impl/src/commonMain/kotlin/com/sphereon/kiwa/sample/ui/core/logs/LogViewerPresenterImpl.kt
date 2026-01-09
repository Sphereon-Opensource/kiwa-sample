/*
 * © 2026 Sphereon International B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.sphereon.kiwa.sample.ui.core.logs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.sphereon.core.api.log.SessionLogManager
import com.sphereon.core.api.log.LogLevel
import com.sphereon.core.log.mobile.MobileLogEntry
import com.sphereon.core.log.mobile.MobileLogExportOptions
import com.sphereon.core.log.mobile.MobileLogFilter
import com.sphereon.core.log.mobile.MobileLogManager
import com.sphereon.di.session.SessionScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@Inject
@ContributesBinding(SessionScope::class, boundType = LogViewerPresenter::class)
class LogViewerPresenterImpl(
    private val mobileLogManager: MobileLogManager,
    logManager: SessionLogManager
) : LogViewerPresenter {

    private val log = logManager.withTag("LogViewerPresenter")

    @Composable
    override fun present(input: Unit): LogViewerPresenter.Model {
        var selectedLogLevel by remember { mutableStateOf(LogViewerPresenter.LogLevel.ALL) }
        var searchQuery by remember { mutableStateOf("") }
        var isSearchVisible by remember { mutableStateOf(false) }
        var exportMessage by remember { mutableStateOf<String?>(null) }

        val scope = rememberCoroutineScope()

        // Clear export message after delay
        SetupExportMessageClearEffect(exportMessage) { exportMessage = null }

        // Get logs directly from StateFlow - collectAsState() returns current value synchronously
        // No need for separate allLogs state variable which caused async update issues
        val allLogs by mobileLogManager.logsFlow.collectAsState()

        // Compute filtered logs synchronously
        val filteredLogs = remember(allLogs, selectedLogLevel, searchQuery) {
            applyFilters(allLogs, selectedLogLevel, searchQuery)
        }

        // isLoading is false once we have any logs in the flow
        val isLoading = allLogs.isEmpty()

        val onStateEvent = createStateEventHandler(
            scope = scope,
            mobileLogManager = mobileLogManager,
            allLogs = allLogs,
            filteredLogs = filteredLogs,
            selectedLogLevel = selectedLogLevel,
            searchQuery = searchQuery,
            isSearchVisible = isSearchVisible,
            onSelectedLogLevelChange = { selectedLogLevel = it },
            onSearchQueryChange = { searchQuery = it },
            onSearchVisibleChange = { isSearchVisible = it },
            onExportMessageChange = { exportMessage = it }
        )

        return LogViewerPresenter.Model.LogViewer(
            logs = allLogs.map { it.toLogEntry() },
            filteredLogs = filteredLogs.map { it.toLogEntry() },
            selectedLogLevel = selectedLogLevel,
            searchQuery = searchQuery,
            isSearchVisible = isSearchVisible,
            isLoading = isLoading,
            exportMessage = exportMessage,
            onStateEvent = onStateEvent
        )
    }

    @Composable
    private fun SetupExportMessageClearEffect(exportMessage: String?, onClear: () -> Unit) {
        LaunchedEffect(exportMessage) {
            exportMessage?.let {
                val delay = if (it.contains("paste") || it.contains("save")) {
                    EXPORT_MESSAGE_LONG_DELAY_MS
                } else {
                    EXPORT_MESSAGE_SHORT_DELAY_MS
                }
                delay(delay)
                onClear()
            }
        }
    }


    @Suppress("LongParameterList")
    private fun createStateEventHandler(
        scope: kotlinx.coroutines.CoroutineScope,
        mobileLogManager: MobileLogManager,
        allLogs: List<MobileLogEntry>,
        filteredLogs: List<MobileLogEntry>,
        selectedLogLevel: LogViewerPresenter.LogLevel,
        searchQuery: String,
        isSearchVisible: Boolean,
        onSelectedLogLevelChange: (LogViewerPresenter.LogLevel) -> Unit,
        onSearchQueryChange: (String) -> Unit,
        onSearchVisibleChange: (Boolean) -> Unit,
        onExportMessageChange: (String?) -> Unit
    ): (LogViewerPresenter.StateEvent) -> Unit = { event ->
        when (event) {
            is LogViewerPresenter.StateEvent.ClearLogs -> {
                handleClearLogs(scope, mobileLogManager)
            }

            is LogViewerPresenter.StateEvent.ExportLogs -> {
                handleExportLogs(
                    scope,
                    mobileLogManager,
                    allLogs,
                    filteredLogs,
                    selectedLogLevel,
                    searchQuery,
                    onExportMessageChange
                )
            }

            is LogViewerPresenter.StateEvent.RefreshLogs -> {
                // No-op: logs auto-update via StateFlow.collectAsState()
            }

            is LogViewerPresenter.StateEvent.SelectLogLevel -> {
                onSelectedLogLevelChange(event.level)
            }

            is LogViewerPresenter.StateEvent.ToggleSearch -> {
                val newVisibility = !isSearchVisible
                onSearchVisibleChange(newVisibility)
                if (!newVisibility) {
                    onSearchQueryChange("")
                }
            }

            is LogViewerPresenter.StateEvent.UpdateSearchQuery -> {
                onSearchQueryChange(event.query)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun handleClearLogs(
        scope: kotlinx.coroutines.CoroutineScope,
        mobileLogManager: MobileLogManager
    ) {
        scope.launch {
            try {
                mobileLogManager.clearLogs()
            } catch (e: Exception) {
                // Exception handled to prevent detekt violation - not logging to prevent recomposition loop
                // In production, you might want to show a user-facing error message instead
            }
        }
    }

    @Suppress("TooGenericExceptionCaught", "LongParameterList")
    private fun handleExportLogs(
        scope: kotlinx.coroutines.CoroutineScope,
        mobileLogManager: MobileLogManager,
        allLogs: List<MobileLogEntry>,
        filteredLogs: List<MobileLogEntry>,
        selectedLogLevel: LogViewerPresenter.LogLevel,
        searchQuery: String,
        onExportMessageChange: (String?) -> Unit
    ) {
        scope.launch {
            try {
                val exportOptions = MobileLogExportOptions(
                    includeTimestamps = true,
                    includeLevel = true,
                    includeTag = true,
                    includeContext = true,
                    includeStackTrace = true
                )

                val filter = MobileLogFilter(
                    level = selectedLogLevel.toMobileLogLevel(),
                    searchText = searchQuery.takeIf { it.isNotBlank() }
                )

                val exportedLogs = mobileLogManager.exportLogs(filter, exportOptions)

                val logFileContent =
                    buildExportContent(
                        allLogs,
                        filteredLogs,
                        selectedLogLevel,
                        searchQuery,
                        exportedLogs
                    )

                onExportMessageChange(logFileContent)
            } catch (e: Exception) {
                onExportMessageChange("Error exporting logs: ${e.message}")
            }
        }
    }


    private fun applyFilters(
        logs: List<MobileLogEntry>,
        level: LogViewerPresenter.LogLevel,
        query: String
    ): List<MobileLogEntry> {
        var filtered = logs

        if (level != LogViewerPresenter.LogLevel.ALL) {
            val mobileLogLevel = level.toMobileLogLevel()
            filtered = filtered.filter { it.level == mobileLogLevel }
        }

        if (query.isNotBlank()) {
            filtered = filtered.filter { log ->
                log.message.contains(query, ignoreCase = true) ||
                    log.tag?.contains(query, ignoreCase = true) == true ||
                    log.throwable?.contains(query, ignoreCase = true) == true
            }
        }

        return filtered.sortedByDescending { it.timestamp }
    }

    private fun LogViewerPresenter.LogLevel.toMobileLogLevel(): LogLevel? = when (this) {
        LogViewerPresenter.LogLevel.ALL -> null
        LogViewerPresenter.LogLevel.VERBOSE -> LogLevel.TRACE // Map VERBOSE to TRACE since VERBOSE doesn't exist
        LogViewerPresenter.LogLevel.DEBUG -> LogLevel.DEBUG
        LogViewerPresenter.LogLevel.INFO -> LogLevel.INFO
        LogViewerPresenter.LogLevel.WARN -> LogLevel.WARN
        LogViewerPresenter.LogLevel.ERROR -> LogLevel.ERROR
    }

    private fun MobileLogEntry.toLogEntry(): LogViewerPresenter.LogEntry {
        return LogViewerPresenter.LogEntry(
            timestamp = this.timestamp.toString(),
            level = this.level.toPresenterLogLevel(),
            tag = this.tag ?: "Unknown",
            message = this.message,
            exception = this.throwable
        )
    }

    private fun LogLevel.toPresenterLogLevel(): LogViewerPresenter.LogLevel = when (this) {
        LogLevel.TRACE -> LogViewerPresenter.LogLevel.VERBOSE // Map TRACE to VERBOSE for display
        LogLevel.DEBUG -> LogViewerPresenter.LogLevel.DEBUG
        LogLevel.INFO -> LogViewerPresenter.LogLevel.INFO
        LogLevel.WARN -> LogViewerPresenter.LogLevel.WARN
        LogLevel.ERROR -> LogViewerPresenter.LogLevel.ERROR
        LogLevel.OFF -> LogViewerPresenter.LogLevel.ERROR // Map OFF to ERROR as fallback
    }

    private fun buildExportContent(
        allLogs: List<MobileLogEntry>,
        filteredLogs: List<MobileLogEntry>,
        selectedLogLevel: LogViewerPresenter.LogLevel,
        searchQuery: String,
        exportedLogs: String
    ): String {
        val timestamp = Clock.System.now()
        return buildString {
            appendLine("=== Application Log Export ===")
            appendLine("Export Date: $timestamp")
            appendLine("App Version: [Add your app version here]")
            appendLine("Platform: [Add platform info here]")
            appendLine("Total Available Logs: ${allLogs.size}")
            appendLine("Exported Log Entries: ${filteredLogs.size}")
            appendLine("Log Level Filter: ${selectedLogLevel.displayName}")
            if (searchQuery.isNotBlank()) {
                appendLine("Search Query: \"$searchQuery\"")
            }
            appendLine(SEPARATOR_LINE)
            appendLine()

            val levelCounts = filteredLogs.groupingBy { it.level }.eachCount()
            appendLine("Log Level Summary:")
            levelCounts.forEach { (level, count) ->
                appendLine("  ${level.toPresenterLogLevel().displayName}: $count entries")
            }
            appendLine()
            appendLine(SEPARATOR_LINE)
            appendLine("LOG ENTRIES:")
            appendLine(SEPARATOR_LINE)
            appendLine()

            append(exportedLogs)
        }
    }

    companion object {
        private const val EXPORT_MESSAGE_LONG_DELAY_MS = 8000L
        private const val EXPORT_MESSAGE_SHORT_DELAY_MS = 3000L
        private const val SEPARATOR_LINE = "====================================="
    }
}
