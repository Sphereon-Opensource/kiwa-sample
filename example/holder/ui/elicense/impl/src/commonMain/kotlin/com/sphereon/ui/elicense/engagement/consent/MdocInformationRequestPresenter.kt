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

package com.sphereon.ui.elicense.engagement.consent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import com.sphereon.di.session.SureSessionScope
import com.sphereon.mdoc.data.device.DocRequest
import com.sphereon.mdoc.data.device.IDocumentWithKeyAlias
import com.sphereon.mdoc.transfer.MapDrivenDocRequestSelector
import com.sphereon.ui.card.ICredentialCardPresenter
import com.sphereon.ui.elicense.engagement.consent.IMdocInformationRequestPresenter.DocRequestSection
import com.sphereon.ui.elicense.engagement.consent.IMdocInformationRequestPresenter.Event
import com.sphereon.ui.elicense.engagement.consent.IMdocInformationRequestPresenter.Input
import com.sphereon.ui.elicense.engagement.consent.IMdocInformationRequestPresenter.Model
import com.sphereon.ui.elicense.store.ISimpleMdocStore

/**
 * Stateful presenter mapping a set of DCQL doc requests into selectable mini credentials.
 */
@Inject
@ContributesBinding(SureSessionScope::class)
class MdocInformationRequestPresenter(
    private val credentialCardPresenter: ICredentialCardPresenter,
    private val storage: ISimpleMdocStore
) : IMdocInformationRequestPresenter {

    private data class DocRequestSectionState(
        val docRequest: DocRequest,
        val matchingDocs: List<IDocumentWithKeyAlias>,
        var selectedIndex: Int?
    )

    @Composable
    override fun present(input: Input): Model {
        val deviceRequest = input.deviceRequest

        // State for our sections, loaded asynchronously
        var docRequestSectionStates by remember(
            deviceRequest
        ) { mutableStateOf<List<DocRequestSectionState>>(emptyList()) }

        // Load documents and initialize section states
        LaunchedEffect(deviceRequest) {
            val allEntries = storage.getDocuments()
            docRequestSectionStates = deviceRequest.docRequests.orEmpty().map { req ->
                val matching = allEntries.filter { it.document.docType.toString() == req.itemsRequest.docType.toString() }.map {
                    // Note this does not perform device signing at this point. We are simply applying disclosures
                    it.document.issuerSigned.nameSpaces?.forEach { entry ->
                        entry.value.forEach { item -> println("${entry.key}: ${item.data().elementIdentifier}=>${item.data().elementValue}") }
                    }
                    val selectiveDisclosedDoc = it.document.limitDisclosures(req).toDocument()
                    selectiveDisclosedDoc.issuerSigned.nameSpaces?.forEach { entry ->
                        entry.value.forEach { item -> println("${entry.key}: ${item.data().elementIdentifier}=>${item.data().elementValue}") }
                    }
                    it.copy(document = selectiveDisclosedDoc)
                }
                DocRequestSectionState(
                    docRequest = req,
                    matchingDocs = matching,
                    selectedIndex = if (matching.size == 1) 0 else null // Auto-select
                )
            }
        }

        val docRequestDocRequestSections: List<DocRequestSection> = docRequestSectionStates.map { docRequestSectionState ->
            val cards = docRequestSectionState.matchingDocs.map { mdoc ->
                credentialCardPresenter.present(
                    ICredentialCardPresenter.Input.fromDocument(
                        mdoc.document,
                        variant = ICredentialCardPresenter.Variant.Mini
                    )
                )
            }
            with(docRequestSectionState) {
                val details: MutableList<IMdocInformationRequestPresenter.DetailRow> = mutableListOf()
                if (selectedIndex != null) {
                    this.matchingDocs[selectedIndex!!].document.issuerSigned.nameSpaces?.forEach { ns ->
                        ns.value.forEach { item ->
                            val itemData = item.data()
                            details.add(
                                IMdocInformationRequestPresenter.DetailRow(
                                    label = itemData.elementIdentifier.toString(),
                                    value = itemData.elementValue.toString()
                                )
                            )
                        }
                    }
                }

                DocRequestSection(
                    id = docRequest.itemsRequest.docType.toString(),
                    title = docRequest.itemsRequest.docType.toString(),
                    description = docRequest.itemsRequest.requestInfo?.toString() ?: "Please provide info",
                    cards = cards,
                    selectedIndex = selectedIndex,
                    details = details

                )
            }
        }

        val canContinue = docRequestSectionStates.isNotEmpty() && docRequestSectionStates.all { it.selectedIndex != null }

        val onEvent: (Event) -> Unit = { ev ->
            when (ev) {
                is Event.OnCardSelected -> {
                    docRequestSectionStates = docRequestSectionStates.mapIndexed { index, sectionState ->
                        if (index == ev.sectionIndex) {
                            val newIndex = if (sectionState.selectedIndex == ev.cardIndex) null else ev.cardIndex
                            sectionState.copy(selectedIndex = newIndex)
                        } else {
                            sectionState
                        }
                    }
                }

                Event.OnShare -> {
                    if (canContinue) {
                        val selections = docRequestSectionStates.associate { state ->
                            state.docRequest to state.matchingDocs[state.selectedIndex!!]
                        }
                        input.onContinue(MapDrivenDocRequestSelector(selections))
                    }
                }

                Event.OnDecline -> {}
            }
        }
        return Model.Content(
            docRequestSections = docRequestDocRequestSections,
            canContinue = canContinue,
            onEvent = onEvent
        )
    }
}
