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

package com.sphereon.kiwa.sample.ui.core.logs

import androidx.compose.runtime.Immutable
import com.sphereon.kiwa.sample.ui.core.appbar.IAppBarConfigModel
import com.sphereon.kiwa.sample.ui.core.appbar.TopAppBarConfig
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter

/**
 * Log viewer presenter interface for displaying, filtering, and exporting application logs.
 */
interface LogViewerPresenter : MoleculePresenter<Unit, LogViewerPresenter.Model> {

    sealed interface StateEvent {
        object ClearLogs : StateEvent
        object ExportLogs : StateEvent
        object ToggleSearch : StateEvent
        data class UpdateSearchQuery(val query: String) : StateEvent
        data class SelectLogLevel(val level: LogLevel) : StateEvent
        object RefreshLogs : StateEvent
    }

    enum class LogLevel(val displayName: String) {
        ALL("All"),
        VERBOSE("Verbose"),
        DEBUG("Debug"),
        INFO("Info"),
        WARN("Warning"),
        ERROR("Error")
    }

    /**
     * Represents a single log entry.
     */
    @Immutable
    data class LogEntry(
        val timestamp: String,
        val level: LogLevel,
        val tag: String,
        val message: String,
        val exception: String? = null
    )

    sealed interface Model : BaseModel, IAppBarConfigModel {
        val onStateEvent: (event: StateEvent) -> Unit

        override fun topAppBarConfig(): TopAppBarConfig {
            return TopAppBarConfig(
                title = "Application Logs",
                menuItems = listOf(
                    TopAppBarConfig.MenuItem("Refresh", action = { onStateEvent(StateEvent.RefreshLogs) }),
                    TopAppBarConfig.MenuItem("Export", action = { onStateEvent(StateEvent.ExportLogs) }),
                    TopAppBarConfig.MenuItem("Clear", action = { onStateEvent(StateEvent.ClearLogs) })
                )
            )
        }

        @Immutable
        data class LogViewer(
            val logs: List<LogEntry>,
            val filteredLogs: List<LogEntry>,
            val selectedLogLevel: LogLevel,
            val searchQuery: String,
            val isSearchVisible: Boolean,
            val isLoading: Boolean,
            val exportMessage: String? = null,
            override val onStateEvent: (event: StateEvent) -> Unit
        ) : Model
    }
}
