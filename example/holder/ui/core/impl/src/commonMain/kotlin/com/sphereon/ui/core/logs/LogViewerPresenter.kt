/*
 * © 2025 Sphereon International B.V.
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

package com.sphereon.ui.core.logs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import com.sphereon.core.api.LogLevel
import com.sphereon.core.log.mobile.IMobileLogManager
import com.sphereon.core.log.mobile.MobileLogEntry
import com.sphereon.core.log.mobile.MobileLogExportOptions
import com.sphereon.core.log.mobile.MobileLogFilter
import com.sphereon.di.session.SureSessionScope

@Inject
@ContributesBinding(SureSessionScope::class, boundType = ILogViewerPresenter::class)
class LogViewerPresenter(
    private val mobileLogManager: IMobileLogManager
) : ILogViewerPresenter {

    @Composable
    override fun present(input: Unit): ILogViewerPresenter.Model {
        var allLogs by remember { mutableStateOf<List<MobileLogEntry>>(emptyList()) }
        var filteredLogs by remember { mutableStateOf<List<MobileLogEntry>>(emptyList()) }
        var selectedLogLevel by remember { mutableStateOf(ILogViewerPresenter.LogLevel.ALL) }
        var searchQuery by remember { mutableStateOf("") }
        var isSearchVisible by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(true) }
        var exportMessage by remember { mutableStateOf<String?>(null) }

        val scope = rememberCoroutineScope()

        // Clear export message after 8 seconds (longer for export messages with instructions)
        LaunchedEffect(exportMessage) {
            exportMessage?.let {
                val delay = if (it.contains("paste") || it.contains("save")) 8000L else 3000L
                kotlinx.coroutines.delay(delay)
                exportMessage = null
            }
        }

        // Collect logs from flow for real-time updates
        val logsFromFlow by mobileLogManager.logsFlow.collectAsState()

        // Update local state when flow emits new logs
        LaunchedEffect(logsFromFlow) {
            allLogs = logsFromFlow
        }

        // Apply filtering whenever logs, level, or search query changes
        LaunchedEffect(allLogs, selectedLogLevel, searchQuery) {
            println(
                "LogViewerPresenter: Applying filters - allLogs.size=${allLogs.size}, selectedLogLevel=$selectedLogLevel, searchQuery='$searchQuery'"
            )

            // Debug: Print log level distribution
            val logLevelCounts = allLogs.groupingBy { it.level }.eachCount()
            println("LogViewerPresenter: Log level distribution: $logLevelCounts")

            filteredLogs = applyFilters(allLogs, selectedLogLevel, searchQuery)

            println("LogViewerPresenter: After filtering - filteredLogs.size=${filteredLogs.size}")
            isLoading = false
        }

        // Initial load
        LaunchedEffect(Unit) {
            try {
                isLoading = true
                allLogs = mobileLogManager.getAllLogs()

                // Debug: Add some test logs if no logs exist
                if (allLogs.isEmpty()) {
                    println("LogViewerPresenter: No logs found, checking if we can add test logs...")
                    try {
                        // Try to add some test logs through the log manager
                        // This is just for debugging - remove in production
                        println("LogViewerPresenter: Consider using your app to generate some logs for testing")
                    } catch (e: Exception) {
                        println("LogViewerPresenter: Could not add test logs: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                // Handle error - in production you'd want proper error handling
                println("Error loading logs: ${e.message}")
            } finally {
                isLoading = false
            }
        }

        val onStateEvent: (ILogViewerPresenter.StateEvent) -> Unit = { event ->
            when (event) {
                is ILogViewerPresenter.StateEvent.ClearLogs -> {
                    scope.launch {
                        try {
                            mobileLogManager.clearLogs()
                            // Flow will automatically update
                        } catch (e: Exception) {
                            println("Error clearing logs: ${e.message}")
                        }
                    }
                }

                is ILogViewerPresenter.StateEvent.ExportLogs -> {
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

                            // Create a formatted log file content with header information
                            val timestamp = Clock.System.now()
                            val logFileContent = buildString {
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
                                appendLine("=====================================")
                                appendLine()

                                // Add log level summary
                                val levelCounts = filteredLogs.groupingBy { it.level }.eachCount()
                                appendLine("Log Level Summary:")
                                levelCounts.forEach { (level, count) ->
                                    appendLine("  ${level.toPresenterLogLevel().displayName}: $count entries")
                                }
                                appendLine()
                                appendLine("=====================================")
                                appendLine("LOG ENTRIES:")
                                appendLine("=====================================")
                                appendLine()

                                append(exportedLogs)
                            }

                            // Instead of copying to clipboard, set the exportMessage to contain the exported logs
                            exportMessage = logFileContent

                            // Print for debugging
                            println("Exported logs:\n$logFileContent")

                            // TODO: In the next iteration, implement proper file sharing or clipboard in the UI layer.
                        } catch (e: Exception) {
                            println("Error exporting logs: ${e.message}")
                            exportMessage = "Error exporting logs: ${e.message}"
                        }
                    }
                }

                is ILogViewerPresenter.StateEvent.RefreshLogs -> {
                    scope.launch {
                        try {
                            isLoading = true
                            allLogs = mobileLogManager.getAllLogs()
                        } catch (e: Exception) {
                            println("Error refreshing logs: ${e.message}")
                        } finally {
                            isLoading = false
                        }
                    }
                }

                is ILogViewerPresenter.StateEvent.SelectLogLevel -> {
                    selectedLogLevel = event.level
                }

                is ILogViewerPresenter.StateEvent.ToggleSearch -> {
                    isSearchVisible = !isSearchVisible
                    if (!isSearchVisible) {
                        searchQuery = ""
                    }
                }

                is ILogViewerPresenter.StateEvent.UpdateSearchQuery -> {
                    searchQuery = event.query
                }
            }
        }

        return ILogViewerPresenter.Model.LogViewer(
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

    private fun applyFilters(
        logs: List<MobileLogEntry>,
        level: ILogViewerPresenter.LogLevel,
        query: String
    ): List<MobileLogEntry> {
        var filtered = logs

        println("LogViewerPresenter.applyFilters: Starting with ${logs.size} logs")

        // Apply level filter
        if (level != ILogViewerPresenter.LogLevel.ALL) {
            val mobileLogLevel = level.toMobileLogLevel()
            println("LogViewerPresenter.applyFilters: Filtering by level $level -> $mobileLogLevel")
            filtered = filtered.filter { it.level == mobileLogLevel }
            println("LogViewerPresenter.applyFilters: After level filter: ${filtered.size} logs")
        }

        // Apply search filter
        if (query.isNotBlank()) {
            println("LogViewerPresenter.applyFilters: Applying search filter for query: '$query'")
            filtered = filtered.filter { log ->
                log.message.contains(query, ignoreCase = true) ||
                    log.tag?.contains(query, ignoreCase = true) == true ||
                    log.throwable?.contains(query, ignoreCase = true) == true
            }
            println("LogViewerPresenter.applyFilters: After search filter: ${filtered.size} logs")
        }

        val result = filtered.sortedByDescending { it.timestamp }
        println("LogViewerPresenter.applyFilters: Final result: ${result.size} logs")
        return result
    }

    private fun ILogViewerPresenter.LogLevel.toMobileLogLevel(): LogLevel? = when (this) {
        ILogViewerPresenter.LogLevel.ALL -> null
        ILogViewerPresenter.LogLevel.VERBOSE -> LogLevel.TRACE // Map VERBOSE to TRACE since VERBOSE doesn't exist
        ILogViewerPresenter.LogLevel.DEBUG -> LogLevel.DEBUG
        ILogViewerPresenter.LogLevel.INFO -> LogLevel.INFO
        ILogViewerPresenter.LogLevel.WARN -> LogLevel.WARN
        ILogViewerPresenter.LogLevel.ERROR -> LogLevel.ERROR
    }

    private fun MobileLogEntry.toLogEntry(): ILogViewerPresenter.LogEntry {
        return ILogViewerPresenter.LogEntry(
            timestamp = this.timestamp.toString(),
            level = this.level.toPresenterLogLevel(),
            tag = this.tag ?: "Unknown",
            message = this.message,
            exception = this.throwable
        )
    }

    private fun LogLevel.toPresenterLogLevel(): ILogViewerPresenter.LogLevel = when (this) {
        LogLevel.TRACE -> ILogViewerPresenter.LogLevel.VERBOSE // Map TRACE to VERBOSE for display
        LogLevel.DEBUG -> ILogViewerPresenter.LogLevel.DEBUG
        LogLevel.INFO -> ILogViewerPresenter.LogLevel.INFO
        LogLevel.WARN -> ILogViewerPresenter.LogLevel.WARN
        LogLevel.ERROR -> ILogViewerPresenter.LogLevel.ERROR
        LogLevel.OFF -> ILogViewerPresenter.LogLevel.ERROR // Map OFF to ERROR as fallback
    }
}
