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

interface CredentialDetailsPresenter : MoleculePresenter<CredentialDetailsPresenter.Input, CredentialDetailsPresenter.Model> {

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
        data class ViewImage(val imageData: ByteArray, val label: String) : Event
        data object CloseImage : Event
    }

    @Immutable
    data class VerifiedInfoItem(
        val namespace: String,
        val label: String,
        val value: String?,
        val children: List<VerifiedInfoItem>? = null,
        val imageData: ByteArray? = null,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is VerifiedInfoItem) return false

            if (namespace != other.namespace) return false
            if (label != other.label) return false
            if (value != other.value) return false
            if (children != other.children) return false
            if (imageData != null) {
                if (other.imageData == null) return false
                if (!imageData.contentEquals(other.imageData)) return false
            } else if (other.imageData != null) return false

            return true
        }

        override fun hashCode(): Int {
            var result = namespace.hashCode()
            result = 31 * result + label.hashCode()
            result = 31 * result + (value?.hashCode() ?: 0)
            result = 31 * result + (children?.hashCode() ?: 0)
            result = 31 * result + (imageData?.contentHashCode() ?: 0)
            return result
        }
    }

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
            val card: CredentialCardPresenter.Model,
            val selectedTab: Tab,
            val verifiedItems: List<VerifiedInfoItem>,
            val showDeleteModal: Boolean,
            val fullScreenImage: Pair<ByteArray, String>?,
            override val onEvent: (Event) -> Unit,
        ) : Model
    }
}
