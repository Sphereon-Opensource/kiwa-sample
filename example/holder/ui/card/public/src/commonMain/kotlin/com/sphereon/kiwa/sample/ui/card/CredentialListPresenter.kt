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

package com.sphereon.kiwa.sample.ui.card

import androidx.compose.runtime.Immutable
import com.sphereon.kiwa.sample.ui.core.appbar.BottomAppBarConfig
import com.sphereon.kiwa.sample.ui.core.appbar.IAppBarConfigModel
import com.sphereon.kiwa.sample.ui.core.appbar.TopAppBarConfig
import com.sphereon.mdoc.data.device.Document
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter

/**
 * Credential list presenter: provides Inputs and Models for the list of credential items.
 */
interface CredentialListPresenter : MoleculePresenter<Unit, CredentialListPresenter.Model> {

    sealed interface StateEvent {
        object CreateNewCredential : StateEvent
        object AssignLicense : StateEvent
        data class OpenCredentialDetails(val document: Document) : StateEvent
        data class ConfirmDelete(val document: Document) : StateEvent
        object CancelDelete : StateEvent
        object AttendedPresentation : StateEvent
        object ShowQrPresentation : StateEvent // New event for QR presentation
        object GoToHome : StateEvent // New event for home navigation
        object SubscriptionKey : StateEvent // New event for subscription key
        object ViewLogs : StateEvent // New event for viewing application logs
        object Logout : StateEvent // New event for logout
        object ClearLicenses : StateEvent // New event for clearing all licenses
    }

    sealed interface Model : BaseModel, IAppBarConfigModel {
        val onStateEvent: (event: StateEvent) -> Unit
        override fun topAppBarConfig(): TopAppBarConfig {
            return TopAppBarConfig(
                title = "Licenses",
                menuItems = listOf(
                    TopAppBarConfig.MenuItem("Sample PID", action = { onStateEvent(StateEvent.CreateNewCredential) }),
                    TopAppBarConfig.MenuItem("Assign license", action = { onStateEvent(StateEvent.AssignLicense) }),
                    TopAppBarConfig.MenuItem("Clear licenses", action = { onStateEvent(StateEvent.ClearLicenses) }),
                    TopAppBarConfig.MenuItem("Subscription key", action = { onStateEvent(StateEvent.SubscriptionKey) }),
                    TopAppBarConfig.MenuItem("View Logs", action = { onStateEvent(StateEvent.ViewLogs) }),
                    TopAppBarConfig.MenuItem("Logout", action = { onStateEvent(StateEvent.Logout) })
//                    TopAppBarConfig.MenuItem("Attended presentation", action = { onStateEvent(StateEvent.AttendedPresentation) })
                )
            )
        }

        override fun bottomAppBarConfig(): BottomAppBarConfig? {
            return BottomAppBarConfig(
                show = true
                // Note: Custom bottom navigation will be handled in the renderer
            )
        }

        @Immutable
        data class CredentialList(
            val items: List<CredentialListItemPresenter.Model.Row>,
            val pendingDelete: Document? = null,
            override val onStateEvent: (event: StateEvent) -> Unit
        ) : Model
    }
}
