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

package com.sphereon.kiwa.sample.ui.elicense.engagement.qr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.sphereon.core.api.SessionLogManager
import com.sphereon.di.session.SessionScope
import com.sphereon.kiwa.sample.ui.core.backstack.LocalBackstackScope
import com.sphereon.kiwa.sample.ui.elicense.engagement.consent.MdocInformationRequestPresenter
import com.sphereon.kiwa.sample.ui.elicense.engagement.qr.MdocEngagementPresenter.UiStateEvent
import com.sphereon.mdoc.MdocSignService
import com.sphereon.mdoc.data.device.DeviceRequest
import com.sphereon.mdoc.engagement.MdocEngagementManager
import com.sphereon.mdoc.engagement.QrMode
import com.sphereon.mdoc.engagement.TerminalOutcome
import com.sphereon.mdoc.engagement.UiPhase
import com.sphereon.mdoc.transfer.MapDrivenDocRequestSelector
import com.sphereon.mdoc.transfer.TransferManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.presenter.molecule.backgesture.BackHandlerPresenter
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

/**
 * MdocEngagementPresenter
 */
@Inject
@ContributesBinding(SessionScope::class)
class MdocEngagementPresenterImpl(
    private val qrCodeGenerator: QrGenerator,
    private val engagementManager: MdocEngagementManager,
    private val infoRequestPresenter: MdocInformationRequestPresenter,
    private val mdocSignService: MdocSignService,
    logManager: SessionLogManager
) : MdocEngagementPresenter {

    private val log = logManager.withTag("EngagePresenter")

    @Composable
    override fun present(input: MdocEngagementPresenter.Input): MdocEngagementPresenter.Model {
        val backstack = checkNotNull(LocalBackstackScope.current)
        val presenterScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }

        // Handle back button - close all engagements before navigating back
        BackHandlerPresenter(enabled = true) {
            log.debug("Back button pressed - closing all engagements")
            runBlocking {
                engagementManager.closeAll()
            }
            backstack.pop()
        }

        // Collect SessionUiState from the engagement manager's eventHub - this is our source of truth
        val sessionState by engagementManager.eventHub.sessionState.collectAsState()

        // Collect active engagement from manager - single source of truth
        val activeEngagement by engagementManager.activeEngagement.collectAsState()

        // Log state on every recomposition to understand state transitions
        log.debug("=== PRESENTER RECOMPOSITION ===")
        log.debug("Engagement Manager Instance: ${engagementManager.hashCode()}")
        log.debug("SessionUiState: phase=${sessionState.phase}, qrMode=${sessionState.qrMode}, nfcMode=${sessionState.nfcMode}, userInteractionRequired=${sessionState.userInteractionRequired}, terminalOutcome=${sessionState.terminalOutcome}")
        log.debug("ActiveEngagement: ${activeEngagement?.id ?: "null"}")
        log.debug("===============================")

        var isSharing by remember { mutableStateOf(false) }

        // Reset isSharing when we reach terminal state
        DisposableEffect(sessionState.phase) {
            log.debug("Phase changed to: ${sessionState.phase}, terminalOutcome: ${sessionState.terminalOutcome}, isSharing: $isSharing")
            if (sessionState.phase == UiPhase.TERMINAL) {
                log.debug("Reached terminal phase - resetting isSharing to false")
                isSharing = false
            }
            onDispose { }
        }

        // Cleanup on dispose
        DisposableEffect(Unit) {
            onDispose {
                log.debug("Disposing presenter - cleaning up scope")
                isSharing = false
                presenterScope.cancel()
            }
        }


        // Log engagement changes
        DisposableEffect(activeEngagement) {
            log.debug("Current engagement changed, now active: ${activeEngagement?.id}")
            onDispose {
            }
        }

        // Generate QR code when qrMode is DISPLAY and we have an engagement
        val qrImage by produceState<ImageBitmap?>(initialValue = null, sessionState.qrMode, activeEngagement) {
            log.debug("QR image produceState triggered: qrMode=${sessionState.qrMode}, engagement=${activeEngagement?.id}")
            val engagement = activeEngagement ?: return@produceState
            if (sessionState.qrMode != QrMode.DISPLAY) {
                value = null
                return@produceState
            }

            runCatching {
                val uri = engagement.getEngagementUri()
                log.debug("Generating QR for URI: $uri")
                val image = qrCodeGenerator.generateQr(uri)
                value = image
                log.debug("QR code generated successfully for engagement ${engagement.id}")
            }.onFailure { e ->
                log.error("QR generation failed: ${e.message}", exception = e)
                value = null
            }
        }

        // UI Event handlers - delegate to engagement manager
        val onEvent = remember(presenterScope) {
            { event: UiStateEvent ->
                log.debug("Handling UI event: $event")
                when (event) {
                    UiStateEvent.ShowQr -> {
                        log.debug("User clicked 'Show QR' - creating QR engagement via manager")
                        presenterScope.launch {
                            val engagementResult = engagementManager.createEngagement {
                                engagement { qr {} }
                                retrieval { ble { centralClientMode = false; peripheralServerMode = true } }
                            }
                            engagementResult.onSuccess { engagement ->
                                engagement.start()
                                log.debug("QR engagement created and started: ${engagement.id}")
                            }.onFailure { error ->
                                log.error("QR engagement creation failed: ${error.message}")
                            }
                        }
                    }

                    UiStateEvent.Stopped -> {
                        log.debug("Stopped event - closing all engagements and navigating back")
                        presenterScope.launch {
                            engagementManager.closeAll()
                            backstack.pop()
                        }
                    }

                    UiStateEvent.SuccessComplete -> {
                        log.debug("Success complete - closing any active engagements and navigating back")
                        presenterScope.launch {
                            // Close all engagements to ensure clean state
                            engagementManager.closeAll()
                            backstack.pop()
                        }
                    }

                }
                Unit
            }
        }

        val onContinue = remember(presenterScope) {
            { selector: MapDrivenDocRequestSelector ->
                log.debug("Document selection confirmed, starting sharing")
                isSharing = true

                presenterScope.launch {
                    runCatching {
                        // Get transfer manager from the active engagement
                        val engagement = activeEngagement
                            ?: throw IllegalStateException("No active engagement")
                        // The engagement already has a transfer manager after it transitioned to TRANSFER phase
                        val tm: TransferManager = engagement.transferInstance.manager

                        val dr = checkNotNull(sessionState.deviceRequest?.let { DeviceRequest.Decoder.decodeCbor(it) }) {
                            "Device request not available"
                        }

                        val selectorWithSign = selector.copy(
                            sessionTranscript = tm.getSessionTranscript().data(),
                            mdocDeviceSignService = mdocSignService,
                            minDocRequests = MIN_DOC_REQUESTS
                        )

                        tm.registerCustomResponseSelectors(
                            requestResponseProcesser = selectorWithSign,
                            requestDocumentsSelector = selectorWithSign,
                            docRequestSingleDocumentSelector = selectorWithSign
                        )

                        val response = tm.createDeviceResponse(dr, selectorWithSign)
                        if (response.isOk) {
                            tm.sendDeviceResponse(response.value)
                            log.debug("Device response sent successfully - engagement manager will transition to SUCCESS")
                            // Don't set local state - engagement manager will transition to TERMINAL/SUCCESS
                        } else {
                            log.error("Device response creation failed: ${response.error}")
                            isSharing = false
                            throw Exception("Failed to create device response: ${response.error}")
                        }
                    }.onFailure { e ->
                        log.error("Document sharing failed: ${e.message}")
                        isSharing = false
                    }
                }
                Unit
            }
        }

        // Track if we've ever had an active engagement in this presenter session
        val hasHadEngagement = remember { mutableStateOf(false) }
        if (activeEngagement != null && !hasHadEngagement.value) {
            hasHadEngagement.value = true
        }

        // Build model based purely on SessionUiState - it's the source of truth
        // Special case: If TERMINAL state but no active engagement AND we never had an engagement,
        // this is stale state from a previous session - treat as INITIAL
        val model = when {
            sessionState.phase == UiPhase.TERMINAL && activeEngagement == null && !hasHadEngagement.value -> {
                log.debug(">>> Stale TERMINAL state detected (never had engagement in this session) - treating as INITIAL")
                MdocEngagementPresenter.Model.Initial(onEvent)
            }

            // If phase is ENGAGEMENT but there's no active engagement, it's stale state - treat as INITIAL
            sessionState.phase == UiPhase.ENGAGEMENT && activeEngagement == null -> {
                log.debug(">>> Stale ENGAGEMENT state detected (no active engagement) - treating as INITIAL")
                MdocEngagementPresenter.Model.Initial(onEvent)
            }

            sessionState.phase == UiPhase.ENGAGEMENT -> {
                log.debug(">>> Returning Model: ENGAGEMENT (showQr=${sessionState.qrMode == QrMode.DISPLAY})")
                // Show QR or NFC prompt based on SessionUiState
                MdocEngagementPresenter.Model.Engagement(
                    qrImage = if (sessionState.qrMode == QrMode.DISPLAY) qrImage else null,
                    engagementEvent = null,
                    showQr = sessionState.qrMode == QrMode.DISPLAY,
                    onStateEvent = onEvent
                )
            }

            sessionState.phase == UiPhase.TRANSFER -> {
                when {
                    // Show sharing state when user has clicked Share
                    isSharing -> {
                        log.debug(">>> Returning Model: SHARING")
                        MdocEngagementPresenter.Model.Sharing(onEvent)
                    }

                    // Show consent dialog when user interaction is required
                    sessionState.userInteractionRequired -> {
                        log.debug(">>> Returning Model: SELECTING (consent)")
                        val deviceRequest = remember(sessionState.deviceRequest) {
                            checkNotNull(sessionState.deviceRequest?.let { DeviceRequest.Decoder.decodeCbor(it) }) {
                                "Device request should be available when userInteractionRequired=true"
                            }
                        }
                        val consent = infoRequestPresenter.present(
                            MdocInformationRequestPresenter.Input(deviceRequest, onContinue)
                        )
                        MdocEngagementPresenter.Model.Selecting(consent, onEvent)
                    }

                    // Show connecting or sharing state - engagement manager tracks this via phase
                    else -> {
                        log.debug(">>> Returning Model: CONNECTING")
                        val eng = activeEngagement
                        if (sessionState.deviceRequest == null && eng != null && eng.transferInstance.transmissionTypeSelected != null) {
                            LaunchedEffect(sessionState.deviceRequest) {
                                log.debug("Retrieving device request from engagement")
                                eng.transferInstance.manager.receiveDeviceRequest()
                                log.debug("Retrieved device request from engagement")
                            }
                        }

                        MdocEngagementPresenter.Model.Connecting(
                            engagementEvent = null,
                            onStateEvent = onEvent
                        )
                    }
                }
            }

            sessionState.phase == UiPhase.TERMINAL -> {
                when (sessionState.terminalOutcome) {
                    TerminalOutcome.SUCCESS -> {
                        log.debug(">>> Returning Model: SUCCESS (terminalOutcome=SUCCESS)")
                        MdocEngagementPresenter.Model.Success(onEvent)
                    }
                    TerminalOutcome.DECLINED,
                    TerminalOutcome.CANCELED,
                    TerminalOutcome.ERROR,
                        /*TerminalOutcome.TERMINATED*/
                        -> {
                        log.debug(">>> Returning Model: STOPPED (terminalOutcome=${sessionState.terminalOutcome})")
                        MdocEngagementPresenter.Model.Stopped(
                            engagementEvent = null,
                            onStateEvent = onEvent
                        )
                    }

                    null -> {
                        log.debug(">>> Returning Model: INITIAL (phase=TERMINAL, terminalOutcome=null)")
                        MdocEngagementPresenter.Model.Initial(onEvent)
                    }
                }
            }

            else -> {
                log.debug(">>> Returning Model: INITIAL (phase=${sessionState.phase})")
                MdocEngagementPresenter.Model.Initial(onEvent)
            }
        }

        return model
    }

    @Inject
    class Factory(
        private val factory: (MoleculePresenter<MdocEngagementPresenter.Input, *>) -> MdocEngagementPresenterImpl
    ) {
        fun createTestAppTemplatePresenter(
            presenter: MoleculePresenter<MdocEngagementPresenter.Input, *>,
        ): MdocEngagementPresenterImpl = factory(presenter)
    }

    private companion object {
        const val MIN_DOC_REQUESTS = 1
    }
}
