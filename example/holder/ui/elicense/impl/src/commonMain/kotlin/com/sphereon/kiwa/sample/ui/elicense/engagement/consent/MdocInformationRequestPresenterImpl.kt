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

package com.sphereon.kiwa.sample.ui.elicense.engagement.consent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sphereon.di.session.SessionScope
import com.sphereon.kiwa.sample.ui.card.CredentialCardPresenter
import com.sphereon.kiwa.sample.ui.elicense.engagement.consent.MdocInformationRequestPresenter.DocRequestSection
import com.sphereon.kiwa.sample.ui.elicense.engagement.consent.MdocInformationRequestPresenter.Event
import com.sphereon.kiwa.sample.ui.elicense.engagement.consent.MdocInformationRequestPresenter.Input
import com.sphereon.kiwa.sample.ui.elicense.engagement.consent.MdocInformationRequestPresenter.Model
import com.sphereon.kiwa.sample.ui.elicense.store.SimpleMdocStore
import com.sphereon.mdoc.data.device.DeviceRequest
import com.sphereon.mdoc.data.device.DocRequest
import com.sphereon.mdoc.data.device.DocumentWithKeyAlias
import com.sphereon.mdoc.transfer.MapDrivenDocRequestSelector
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

/**
 * Stateful presenter mapping a set of DCQL doc requests into selectable mini credentials.
 */
@Inject
@ContributesBinding(SessionScope::class)
class MdocInformationRequestPresenterImpl(
    private val credentialCardPresenter: CredentialCardPresenter,
    private val storage: SimpleMdocStore
) : MdocInformationRequestPresenter {

    private data class DocRequestSectionState(
        val docRequest: DocRequest,
        val matchingDocs: List<DocumentWithKeyAlias>,
        val selectedIndex: Int?
    )

    @Composable
    override fun present(input: Input): Model {
        val deviceRequest = input.deviceRequest

        // State for our sections, loaded asynchronously
        var docRequestSectionStates by remember(deviceRequest) {
            mutableStateOf<List<DocRequestSectionState>>(emptyList())
        }

        // Load documents and initialize section states
        LaunchedEffect(deviceRequest) {
            val sections = loadDocRequestSections(deviceRequest)
            docRequestSectionStates = sections
        }

        val docRequestDocRequestSections: List<DocRequestSection> = buildDocRequestSections(docRequestSectionStates)

        val canContinue = docRequestSectionStates.isNotEmpty()

        val onEvent: (Event) -> Unit = { ev ->
            docRequestSectionStates = handleEvent(ev, docRequestSectionStates, canContinue, input)
        }

        return Model.Content(
            docRequestSections = docRequestDocRequestSections,
            canContinue = canContinue,
            onEvent = onEvent
        )
    }

    private suspend fun loadDocRequestSections(deviceRequest: DeviceRequest): List<DocRequestSectionState> {
        val allEntries = storage.getDocuments()
        return deviceRequest.docRequests.orEmpty().map { req ->
            val matching = allEntries.filter {
                it.document.docType.toString() == req.itemsRequest.docType.toString()
            }.map {
                // Note this does not perform device signing at this point. We are simply applying disclosures
                it.document.issuerSigned.nameSpaces?.forEach { entry ->
                    entry.value.forEach { item ->
                        println("${entry.key}: ${item.data().elementIdentifier}=>${item.data().elementValue}")
                    }
                }
                val selectiveDisclosedDoc = it.document.limitDisclosures(req).toDocument()
                selectiveDisclosedDoc.issuerSigned.nameSpaces?.forEach { entry ->
                    entry.value.forEach { item ->
                        println("${entry.key}: ${item.data().elementIdentifier}=>${item.data().elementValue}")
                    }
                }
                it.copy(document = selectiveDisclosedDoc)
            }
            // Auto-select if only one match
            val selectedIndex = if (matching.size == 1) {
                0
            } else {
                null
            }

            DocRequestSectionState(
                docRequest = req,
                matchingDocs = matching,
                selectedIndex = selectedIndex
            )
        }
    }

    @Composable
    private fun buildDocRequestSections(
        docRequestSectionStates: List<DocRequestSectionState>
    ): List<DocRequestSection> {
        return docRequestSectionStates.map { docRequestSectionState ->
            val cards = docRequestSectionState.matchingDocs.map { mdoc ->
                credentialCardPresenter.present(
                    CredentialCardPresenter.Input.fromDocument(
                        mdoc.document,
                        variant = CredentialCardPresenter.Variant.Mini
                    )
                )
            }
            with(docRequestSectionState) {
                val details = buildDetailRows(this)

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
    }

    private fun buildDetailRows(sectionState: DocRequestSectionState): List<MdocInformationRequestPresenter.DetailRow> {
        val details: MutableList<MdocInformationRequestPresenter.DetailRow> = mutableListOf()
        if (sectionState.selectedIndex != null) {
            sectionState.matchingDocs[sectionState.selectedIndex].document.issuerSigned.nameSpaces?.forEach { ns ->
                ns.value.forEach { item ->
                    val itemData = item.data()
                    details.add(
                        MdocInformationRequestPresenter.DetailRow(
                            label = itemData.elementIdentifier.toString(),
                            value = itemData.elementValue.toString()
                        )
                    )
                }
            }
        }
        return details
    }

    private fun handleEvent(
        event: Event,
        currentStates: List<DocRequestSectionState>,
        canContinue: Boolean,
        input: Input
    ): List<DocRequestSectionState> {
        return when (event) {
            is Event.OnCardSelected -> {
                currentStates.mapIndexed { index, sectionState ->
                    if (index == event.sectionIndex) {
                        val newIndex = if (sectionState.selectedIndex == event.cardIndex) {
                            sectionState.selectedIndex
                        } else {
                            event.cardIndex
                        }
                        sectionState.copy(selectedIndex = newIndex)
                    } else {
                        sectionState
                    }
                }
            }

            Event.OnShare -> {
                if (canContinue) {
                    val selections = currentStates
                        .mapNotNull { state ->
                            val index = state.selectedIndex
                            if (index != null) {
                                state.docRequest to state.matchingDocs[index]
                            } else {
                                null
                            }
                        }
                        .toMap()
                    println("Selected ${selections.size}: $selections")
                    input.onContinue(
                        MapDrivenDocRequestSelector(
                            selections
                        )
                    )
                }
                currentStates
            }
            Event.OnDecline -> {
                // No action on decline for now
                currentStates
            }
        }
    }
}
