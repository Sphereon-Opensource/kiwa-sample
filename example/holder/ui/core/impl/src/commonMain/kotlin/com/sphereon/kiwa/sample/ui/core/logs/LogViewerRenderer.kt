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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sphereon.kiwa.sample.ui.core.theme.AppColors
import kotlinx.datetime.Clock
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.renderer.ComposeRenderer

@ContributesRenderer(modelType = LogViewerPresenter.Model::class)
@Suppress("TooManyFunctions")
class LogViewerRenderer : ComposeRenderer<LogViewerPresenter.Model>() {

    @Composable
    override fun Compose(model: LogViewerPresenter.Model) {
        when (model) {
            is LogViewerPresenter.Model.LogViewer -> LogViewerScreen(model)
        }
    }

    @Composable
    private fun LogViewerScreen(model: LogViewerPresenter.Model.LogViewer) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.Screen.background)
            ) {
                LogControls(model)

                if (model.isLoading) {
                    LoadingIndicator()
                } else {
                    LogList(model)
                }
            }

            ExportFeedbackMessage(model.exportMessage)
        }
    }

    @Composable
    private fun LoadingIndicator() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AppColors.Accent.primary)
        }
    }

    @Composable
    private fun BoxScope.ExportFeedbackMessage(exportMessage: String?) {
        exportMessage?.let { message ->
            val clipboardManager = LocalClipboardManager.current
            val isExportedLogs = message.startsWith("=== Application Log Export ===")

            // Only copy to clipboard once when exported logs are first shown
            // Use a stable key to prevent unnecessary recompositions
            LaunchedEffect(message.take(EXPORT_MESSAGE_STABLE_KEY_LENGTH)) { // Use first chars as stable key
                if (isExportedLogs) {
                    clipboardManager.setText(AnnotatedString(message))
                }
            }

            ExportFeedbackCard(message, isExportedLogs)
        }
    }

    @Composable
    private fun BoxScope.ExportFeedbackCard(message: String, isExportedLogs: Boolean) {
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (message.contains(ERROR_TEXT)) {
                    Color(COLOR_ERROR_RED)
                } else {
                    AppColors.Accent.primary
                }
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                ExportFeedbackHeader(message)
                Spacer(modifier = Modifier.height(8.dp))
                ExportFeedbackContent(message, isExportedLogs)

                if (!message.contains(ERROR_TEXT)) {
                    ExportFeedbackTip()
                }
            }
        }
    }

    @Composable
    private fun ExportFeedbackHeader(message: String) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (message.contains(ERROR_TEXT)) {
                    "⚠️"
                } else {
                    "✅"
                },
                fontSize = 16.sp,
                modifier = Modifier.padding(end = 8.dp)
            )

            Text(
                text = if (message.contains(ERROR_TEXT)) {
                    "Export Failed"
                } else {
                    "Export Successful"
                },
                color = AppColors.Screen.foreground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    @Composable
    private fun ExportFeedbackContent(message: String, isExportedLogs: Boolean) {
        Text(
            text = if (isExportedLogs) {
                val timestamp = Clock.System.now().toString().take(TIMESTAMP_DATE_LENGTH)
                "Logs exported to clipboard!\nPaste into a text file and save as 'app-logs-$timestamp.txt' to share with support."
            } else {
                message
            },
            color = AppColors.Screen.foreground,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }

    @Composable
    private fun ExportFeedbackTip() {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tip: Open any text editor app, paste (Ctrl+V), and save the file.",
            color = AppColors.Screen.foreground.copy(alpha = 0.8f),
            fontSize = 10.sp,
            fontStyle = FontStyle.Italic
        )
    }

    @Composable
    private fun LogControls(model: LogViewerPresenter.Model.LogViewer) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.List.rowDark)
                .padding(16.dp)
        ) {
            LogLevelFilters(model)

            if (model.isSearchVisible) {
                Spacer(modifier = Modifier.height(12.dp))
                SearchBar(model)
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                LogCountAndSearchButton(model)
            }
        }
    }

    @Composable
    private fun LogLevelFilters(model: LogViewerPresenter.Model.LogViewer) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(LogViewerPresenter.LogLevel.values()) { level ->
                LogLevelFilterChip(level, model)
            }
        }
    }

    @Composable
    private fun LogLevelFilterChip(
        level: LogViewerPresenter.LogLevel,
        model: LogViewerPresenter.Model.LogViewer
    ) {
        FilterChip(
            onClick = { model.onStateEvent(LogViewerPresenter.StateEvent.SelectLogLevel(level)) },
            label = {
                Text(
                    text = level.displayName,
                    fontSize = 12.sp,
                    color = if (level == model.selectedLogLevel) {
                        AppColors.Screen.background
                    } else {
                        AppColors.Screen.foreground
                    }
                )
            },
            selected = level == model.selectedLogLevel,
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AppColors.Accent.primary,
                containerColor = Color.Transparent,
                selectedLabelColor = AppColors.Screen.background,
                labelColor = AppColors.Screen.foreground
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = level == model.selectedLogLevel,
                borderColor = AppColors.Accent.primary,
                selectedBorderColor = AppColors.Accent.primary
            )
        )
    }

    @Composable
    private fun SearchBar(model: LogViewerPresenter.Model.LogViewer) {
        // Use a stable remember with the current search query as key
        var localSearchQuery by remember(key1 = model.searchQuery) {
            mutableStateOf(model.searchQuery)
        }

        OutlinedTextField(
            value = localSearchQuery,
            onValueChange = {
                localSearchQuery = it
                model.onStateEvent(LogViewerPresenter.StateEvent.UpdateSearchQuery(it))
            },
            placeholder = {
                Text(
                    text = SEARCH_LOGS_TEXT,
                    color = AppColors.Misc.mutedText
                )
            },
            trailingIcon = {
                IconButton(
                    onClick = {
                        localSearchQuery = ""
                        model.onStateEvent(LogViewerPresenter.StateEvent.ToggleSearch)
                    }
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close search",
                        tint = AppColors.Screen.foreground
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = AppColors.Screen.foreground,
                unfocusedTextColor = AppColors.Screen.foreground,
                focusedBorderColor = AppColors.Accent.primary,
                unfocusedBorderColor = AppColors.Misc.mutedText,
                cursorColor = AppColors.Accent.primary
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }

    @Composable
    private fun LogCountAndSearchButton(model: LogViewerPresenter.Model.LogViewer) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${model.filteredLogs.size} of ${model.logs.size} logs",
                color = AppColors.Misc.mutedText,
                fontSize = 12.sp
            )

            IconButton(
                onClick = { model.onStateEvent(LogViewerPresenter.StateEvent.ToggleSearch) }
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = SEARCH_LOGS_TEXT,
                    tint = AppColors.Screen.foreground
                )
            }
        }
    }

    @Composable
    private fun LogList(model: LogViewerPresenter.Model.LogViewer) {
        if (model.filteredLogs.isEmpty()) {
            EmptyState(model)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(model.filteredLogs) { logEntry ->
                    LogEntryItem(logEntry)
                }
            }
        }
    }

    @Composable
    private fun EmptyState(model: LogViewerPresenter.Model.LogViewer) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (model.searchQuery.isNotBlank() || model.selectedLogLevel != LogViewerPresenter.LogLevel.ALL) {
                        "No logs match your filters"
                    } else {
                        "No logs available"
                    },
                    color = AppColors.Screen.foreground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (model.searchQuery.isNotBlank() || model.selectedLogLevel != LogViewerPresenter.LogLevel.ALL) {
                        "Try adjusting your search or filter criteria"
                    } else {
                        "Start using the app to generate logs"
                    },
                    color = AppColors.Misc.mutedText,
                    fontSize = 14.sp
                )

                if (model.searchQuery.isNotBlank() || model.selectedLogLevel != LogViewerPresenter.LogLevel.ALL) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            model.onStateEvent(
                                LogViewerPresenter.StateEvent.SelectLogLevel(LogViewerPresenter.LogLevel.ALL)
                            )
                            model.onStateEvent(LogViewerPresenter.StateEvent.UpdateSearchQuery(""))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Accent.primary,
                            contentColor = AppColors.Screen.foreground
                        )
                    ) {
                        Text("Clear Filters")
                    }
                }
            }
        }
    }

    @Composable
    private fun LogEntryItem(logEntry: LogViewerPresenter.LogEntry) {
        // Use log entry timestamp as stable key for remember
        var expanded by remember(key1 = logEntry.timestamp) { mutableStateOf(false) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            colors = CardDefaults.cardColors(
                containerColor = getLogLevelColor(logEntry.level).copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(0.dp) // Flat cards for better density
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                LogEntryHeader(logEntry)
                Spacer(modifier = Modifier.height(6.dp))
                LogEntryMessage(logEntry, expanded)
                LogEntryException(logEntry, expanded)
            }
        }
    }

    @Composable
    private fun LogEntryHeader(logEntry: LogViewerPresenter.LogEntry) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            LogEntryLevelAndTag(logEntry)
            LogEntryTimestamp(logEntry)
        }
    }

    @Composable
    private fun RowScope.LogEntryLevelAndTag(logEntry: LogViewerPresenter.LogEntry) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            LogLevelIndicator(logEntry.level)
            Spacer(modifier = Modifier.width(8.dp))
            LogLevelBadge(logEntry.level)
            Spacer(modifier = Modifier.width(8.dp))
            LogEntryTag(logEntry.tag)
        }
    }

    @Composable
    private fun LogLevelIndicator(level: LogViewerPresenter.LogLevel) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    getLogLevelColor(level),
                    shape = RoundedCornerShape(4.dp)
                )
        )
    }

    @Composable
    private fun LogLevelBadge(level: LogViewerPresenter.LogLevel) {
        Text(
            text = level.displayName.uppercase(),
            color = getLogLevelColor(level),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.background(
                getLogLevelColor(level).copy(alpha = 0.2f),
                RoundedCornerShape(4.dp)
            ).padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }

    @Composable
    private fun LogEntryTag(tag: String) {
        Text(
            text = "[$tag]",
            color = AppColors.Misc.mutedText,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }

    @Composable
    private fun LogEntryTimestamp(logEntry: LogViewerPresenter.LogEntry) {
        Text(
            text = formatTimestamp(logEntry.timestamp),
            color = AppColors.Misc.mutedText,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }

    @Composable
    private fun LogEntryMessage(logEntry: LogViewerPresenter.LogEntry, expanded: Boolean) {
        Text(
            text = logEntry.message,
            color = AppColors.Screen.foreground,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = if (expanded) {
                Int.MAX_VALUE
            } else {
                2
            },
            overflow = TextOverflow.Ellipsis
        )
    }

    @Composable
    private fun LogEntryException(logEntry: LogViewerPresenter.LogEntry, expanded: Boolean) {
        if (expanded && logEntry.exception != null && !logEntry.exception!!.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = AppColors.List.rowDark
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = "Exception:",
                        color = Color(COLOR_ERROR_RED),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = logEntry.exception!!,
                        color = AppColors.Screen.foreground,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }

    private fun getLogLevelColor(level: LogViewerPresenter.LogLevel): Color = when (level) {
        LogViewerPresenter.LogLevel.ERROR -> Color(COLOR_ERROR_RED)
        LogViewerPresenter.LogLevel.WARN -> Color(COLOR_WARNING_ORANGE)
        LogViewerPresenter.LogLevel.INFO -> Color(COLOR_INFO_BLUE)
        LogViewerPresenter.LogLevel.DEBUG -> Color(COLOR_DEBUG_GREEN)
        LogViewerPresenter.LogLevel.VERBOSE -> Color(COLOR_VERBOSE_PURPLE)
        LogViewerPresenter.LogLevel.ALL -> AppColors.Misc.mutedText
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun formatTimestamp(timestamp: String): String {
        // Simple timestamp formatting - in production you might want more sophisticated formatting
        return try {
            timestamp.substringAfter('T').substringBefore('.').take(TIMESTAMP_DISPLAY_LENGTH) // HH:MM:SS
        } catch (e: Exception) {
            timestamp.takeLast(TIMESTAMP_FALLBACK_LENGTH) // Fallback to last 20 characters
        }
    }

    companion object {
        private const val COLOR_ERROR_RED = 0xFFE53E3E
        private const val COLOR_WARNING_ORANGE = 0xFFD69E2E
        private const val COLOR_INFO_BLUE = 0xFF3182CE
        private const val COLOR_DEBUG_GREEN = 0xFF38A169
        private const val COLOR_VERBOSE_PURPLE = 0xFF805AD5
        private const val TIMESTAMP_DISPLAY_LENGTH = 8
        private const val TIMESTAMP_FALLBACK_LENGTH = 20
        private const val SEARCH_LOGS_TEXT = "Search logs"
        private const val TIMESTAMP_DATE_LENGTH = 10
        private const val ERROR_TEXT = "Error"
        private const val EXPORT_MESSAGE_STABLE_KEY_LENGTH = 50
    }
}
