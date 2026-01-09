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

package com.sphereon.kiwa.sample.ui.elicense.engagement.consent

import androidx.compose.runtime.Immutable
import com.sphereon.kiwa.sample.ui.card.CredentialCardPresenter
import com.sphereon.mdoc.data.device.DeviceRequest
import com.sphereon.mdoc.transfer.MapDrivenDocRequestSelector
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter

/**
 * Information Request (OID4VP DCQL) consent screen contract.
 * This screen presents one or more DCQL sections. Each section contains matching minisized
 * credential cards and, when a card is selected, a details view.
 */
interface MdocInformationRequestPresenter : MoleculePresenter<MdocInformationRequestPresenter.Input, MdocInformationRequestPresenter.Model> {

    @Immutable
    data class Input(
        val deviceRequest: DeviceRequest,
        val onContinue: (selector: MapDrivenDocRequestSelector) -> Unit = {}
    )

    @Immutable
    data class DocRequestSection(
        val id: String,
        val title: String,
        val description: String? = null,
        val cards: List<CredentialCardPresenter.Model>,
        val selectedIndex: Int? = null,
        val details: List<DetailRow> = emptyList(),
    )

    @Immutable
    data class DetailRow(
        val label: String,
        val value: String,
    )

    sealed interface Event {
        data class OnCardSelected(val sectionIndex: Int, val cardIndex: Int) : Event
        data object OnShare : Event
        data object OnDecline : Event
    }

    sealed interface Model : BaseModel {
        val docRequestSections: List<DocRequestSection>
        val canContinue: Boolean
        val onEvent: (Event) -> Unit

        @Immutable
        data class Content(
            override val docRequestSections: List<DocRequestSection>,
            override val canContinue: Boolean,
            override val onEvent: (Event) -> Unit,
        ) : Model
    }
}
