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

package com.sphereon.ui.card

import androidx.compose.runtime.Immutable
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import com.sphereon.mdoc.data.device.Document
import com.sphereon.ui.core.appbar.BottomAppBarConfig
import com.sphereon.ui.core.appbar.IAppBarConfigModel
import com.sphereon.ui.core.appbar.TopAppBarConfig

interface ICredentialDetailsPresenter : MoleculePresenter<ICredentialDetailsPresenter.Input, ICredentialDetailsPresenter.Model> {

    @Immutable
    data class Input(
        val document: Document
    )

    @Immutable
    enum class Tab { VerifiedInfo, Activity }

    sealed interface Event {
        data class SelectTab(val tab: Tab) : Event
        data object Back : Event
        data object DeleteClicked : Event
        data object CancelDelete : Event
        data object ConfirmDelete : Event
        data object AttendedPresentation : Event
    }

    @Immutable
    data class VerifiedInfoItem(
        val namespace: String,
        val label: String,
        val value: String,
    )

    sealed interface Model : BaseModel, IAppBarConfigModel {
        val onEvent: (Event) -> Unit

        override fun topAppBarConfig(): TopAppBarConfig {
            return TopAppBarConfig(
                title = "License details",
                backArrowAction = { onEvent(Event.Back) },
                menuItems = listOf(
                    TopAppBarConfig.MenuItem("Delete") { onEvent(Event.DeleteClicked) }
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
        data class Content(
            val card: ICredentialCardPresenter.Model,
            val selectedTab: Tab,
            val verifiedItems: List<VerifiedInfoItem>,
            val showDeleteModal: Boolean,
            override val onEvent: (Event) -> Unit,
        ) : Model
    }
}
