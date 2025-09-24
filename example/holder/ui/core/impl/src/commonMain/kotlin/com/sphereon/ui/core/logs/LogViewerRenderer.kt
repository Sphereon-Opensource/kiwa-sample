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
import kotlinx.datetime.Clock
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.renderer.ComposeRenderer
import com.sphereon.ui.core.theme.AppColors

@ContributesRenderer(modelType = ILogViewerPresenter.Model::class)
class LogViewerRenderer : ComposeRenderer<ILogViewerPresenter.Model>() {

    @Composable
    override fun Compose(model: ILogViewerPresenter.Model) {
        when (model) {
            is ILogViewerPresenter.Model.LogViewer -> LogViewerScreen(model)
        }
    }

    @Composable
    private fun LogViewerScreen(model: ILogViewerPresenter.Model.LogViewer) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.Screen.background)
            ) {
                // Search and filter controls
                LogControls(model)

                // Log list
                if (model.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppColors.Accent.primary)
                    }
                } else {
                    LogList(model)
                }
            }

            // Export feedback message
            model.exportMessage?.let { message ->
                val clipboardManager = LocalClipboardManager.current
                val isExportedLogs = message.startsWith("=== Application Log Export ===")

                // Copy to clipboard if this is exported log content
                LaunchedEffect(message) {
                    if (isExportedLogs) {
                        clipboardManager.setText(AnnotatedString(message))
                    }
                }

                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (message.contains("Error")) Color(0xFFE53E3E) else AppColors.Accent.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (message.contains("Error")) "⚠️" else "✅",
                                fontSize = 16.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )

                            Text(
                                text = if (message.contains("Error")) "Export Failed" else "Export Successful",
                                color = AppColors.Screen.foreground,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isExportedLogs) {
                                val timestamp = Clock.System.now().toString().take(10)
                                "Logs exported to clipboard!\nPaste into a text file and save as 'app-logs-$timestamp.txt' to share with support."
                            } else {
                                message
                            },
                            color = AppColors.Screen.foreground,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        if (!message.contains("Error")) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "💡 Tip: Open any text editor app, paste (Ctrl+V), and save the file.",
                                color = AppColors.Screen.foreground.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun LogControls(model: ILogViewerPresenter.Model.LogViewer) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.List.rowDark)
                .padding(16.dp)
        ) {
            // Level filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ILogViewerPresenter.LogLevel.values()) { level ->
                    FilterChip(
                        onClick = { model.onStateEvent(ILogViewerPresenter.StateEvent.SelectLogLevel(level)) },
                        label = {
                            Text(
                                text = level.displayName,
                                fontSize = 12.sp,
                                color = if (level == model.selectedLogLevel) AppColors.Screen.background else AppColors.Screen.foreground
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
            }

            // Search bar
            if (model.isSearchVisible) {
                Spacer(modifier = Modifier.height(12.dp))

                // Use local state to prevent character duplication issues
                var localSearchQuery by remember(model.searchQuery) { mutableStateOf(model.searchQuery) }

                OutlinedTextField(
                    value = localSearchQuery,
                    onValueChange = {
                        localSearchQuery = it
                        model.onStateEvent(ILogViewerPresenter.StateEvent.UpdateSearchQuery(it))
                    },
                    placeholder = {
                        Text(
                            text = "Search logs...",
                            color = AppColors.Misc.mutedText
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                localSearchQuery = ""
                                model.onStateEvent(ILogViewerPresenter.StateEvent.ToggleSearch)
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
            } else {
                Spacer(modifier = Modifier.height(8.dp))
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
                        onClick = { model.onStateEvent(ILogViewerPresenter.StateEvent.ToggleSearch) }
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search logs",
                            tint = AppColors.Screen.foreground
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun LogList(model: ILogViewerPresenter.Model.LogViewer) {
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
    private fun EmptyState(model: ILogViewerPresenter.Model.LogViewer) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (model.searchQuery.isNotBlank() || model.selectedLogLevel != ILogViewerPresenter.LogLevel.ALL) {
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
                    text = if (model.searchQuery.isNotBlank() || model.selectedLogLevel != ILogViewerPresenter.LogLevel.ALL) {
                        "Try adjusting your search or filter criteria"
                    } else {
                        "Start using the app to generate logs"
                    },
                    color = AppColors.Misc.mutedText,
                    fontSize = 14.sp
                )

                if (model.searchQuery.isNotBlank() || model.selectedLogLevel != ILogViewerPresenter.LogLevel.ALL) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            model.onStateEvent(
                                ILogViewerPresenter.StateEvent.SelectLogLevel(ILogViewerPresenter.LogLevel.ALL)
                            )
                            model.onStateEvent(ILogViewerPresenter.StateEvent.UpdateSearchQuery(""))
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
    private fun LogEntryItem(logEntry: ILogViewerPresenter.LogEntry) {
        var expanded by remember { mutableStateOf(false) }

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
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Level and tag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Level indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    getLogLevelColor(logEntry.level),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = logEntry.level.displayName.uppercase(),
                            color = getLogLevelColor(logEntry.level),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.background(
                                getLogLevelColor(logEntry.level).copy(alpha = 0.2f),
                                RoundedCornerShape(4.dp)
                            ).padding(horizontal = 6.dp, vertical = 2.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "[${logEntry.tag}]",
                            color = AppColors.Misc.mutedText,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Timestamp
                    Text(
                        text = formatTimestamp(logEntry.timestamp),
                        color = AppColors.Misc.mutedText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Message
                Text(
                    text = logEntry.message,
                    color = AppColors.Screen.foreground,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Exception (if present and expanded)
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
                                color = Color.Red,
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
        }
    }

    private fun getLogLevelColor(level: ILogViewerPresenter.LogLevel): Color = when (level) {
        ILogViewerPresenter.LogLevel.ERROR -> Color(0xFFE53E3E) // Red
        ILogViewerPresenter.LogLevel.WARN -> Color(0xFFD69E2E) // Orange
        ILogViewerPresenter.LogLevel.INFO -> Color(0xFF3182CE) // Blue
        ILogViewerPresenter.LogLevel.DEBUG -> Color(0xFF38A169) // Green
        ILogViewerPresenter.LogLevel.VERBOSE -> Color(0xFF805AD5) // Purple
        ILogViewerPresenter.LogLevel.ALL -> AppColors.Misc.mutedText // Gray
    }

    private fun formatTimestamp(timestamp: String): String {
        // Simple timestamp formatting - in production you might want more sophisticated formatting
        return try {
            timestamp.substringAfter('T').substringBefore('.').take(8) // HH:MM:SS
        } catch (e: Exception) {
            timestamp.takeLast(20) // Fallback to last 20 characters
        }
    }
}
