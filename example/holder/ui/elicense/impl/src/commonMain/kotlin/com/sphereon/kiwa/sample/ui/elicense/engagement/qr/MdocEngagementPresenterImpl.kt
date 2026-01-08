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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.sphereon.core.api.log.SessionLogManager
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
    override fun present(input: Unit): MdocEngagementPresenter.Model {
        val backstack = checkNotNull(LocalBackstackScope.current)
        // Use rememberCoroutineScope() for proper Compose integration on iOS
        // This ensures the scope uses the correct dispatcher for the composition
        val presenterScope = rememberCoroutineScope()

        // Handle back button - close all engagements before navigating back
        BackHandlerPresenter(enabled = true) {
            log.debug("Back button pressed - closing all engagements")
            runBlocking {
                engagementManager.closeAll()
            }
            backstack.pop()
        }

        // State storage for session and engagement
        var sessionState by remember { mutableStateOf(engagementManager.eventHub.sessionState.value) }
        var activeEngagement by remember { mutableStateOf(engagementManager.activeEngagement.value) }

        // Collect session state - the renderer has a keepalive animation that keeps
        // CADisplayLink firing, ensuring state changes trigger Molecule recomposition
        LaunchedEffect(engagementManager) {
            engagementManager.eventHub.sessionState.collect { state ->
                sessionState = state
            }
        }

        // Collect active engagement
        LaunchedEffect(engagementManager) {
            engagementManager.activeEngagement.collect { eng ->
                activeEngagement = eng
            }
        }

        // Log the current values on every recomposition for debugging
        log.debug("!!! RECOMPOSITION - sessionState: phase=${sessionState.phase}, qrMode=${sessionState.qrMode}, activeEngagement: ${activeEngagement?.id}")

        // Track QR scanner mode - use rememberSaveable like BackstackChildPresenter for proper state tracking on iOS
        var showQrScanner by rememberSaveable { mutableStateOf(false) }

        // Track recomposition count for debugging - use rememberSaveable for consistency
        var recompositionCount by rememberSaveable { mutableStateOf(0) }
        recompositionCount++

        // Log state on every recomposition to understand state transitions
        log.debug("=== PRESENTER RECOMPOSITION #$recompositionCount ===")
        log.debug("Engagement Manager Instance: ${engagementManager.hashCode()}")
        log.debug("SessionUiState: phase=${sessionState.phase}, qrMode=${sessionState.qrMode}, nfcMode=${sessionState.nfcMode}, userInteractionRequired=${sessionState.userInteractionRequired}, terminalOutcome=${sessionState.terminalOutcome}")
        log.debug("ActiveEngagement: ${activeEngagement?.id ?: "null"}")
        log.debug("===============================")

        var isSharing by rememberSaveable { mutableStateOf(false) }

        // Reset isSharing when we reach terminal state
        DisposableEffect(sessionState.phase) {
            log.debug("Phase changed to: ${sessionState.phase}, terminalOutcome: ${sessionState.terminalOutcome}, isSharing: $isSharing")
            if (sessionState.phase == UiPhase.TERMINAL) {
                log.debug("Reached terminal phase - resetting isSharing to false")
                isSharing = false
            }
            onDispose { }
        }

        // Cleanup on dispose - note: presenterScope is managed by rememberCoroutineScope()
        DisposableEffect(Unit) {
            onDispose {
                log.debug("Disposing presenter")
                isSharing = false
            }
        }


        // Log engagement changes
        DisposableEffect(activeEngagement) {
            log.debug("Current engagement changed, now active: ${activeEngagement?.id}")
            onDispose {
            }
        }

        // Generate QR code when qrMode is DISPLAY and we have an engagement
        // Capture engagement in a local val to enable smart cast
        val currentEngagement = activeEngagement
        val shouldGenerateQr = sessionState.qrMode == QrMode.DISPLAY && currentEngagement != null
        log.debug("QR generation check: shouldGenerate=$shouldGenerateQr, qrMode=${sessionState.qrMode}, engagementId=${currentEngagement?.id}")

        val qrImage = if (shouldGenerateQr && currentEngagement != null) {
            // Only compute QR when conditions are met, keyed on engagement ID
            remember(currentEngagement.id) {
                log.debug("QR remember block EXECUTING for engagement: ${currentEngagement.id}")
                runCatching {
                    val uri = runBlocking { currentEngagement.getEngagementUri() }
                    log.debug("Generating QR for URI: $uri")
                    val image = qrCodeGenerator.generateQr(uri)
                    log.debug("QR code generated successfully for engagement ${currentEngagement.id}")
                    image
                }.getOrElse { e ->
                    log.error("QR generation failed: ${e.message}", exception = e)
                    null
                }
            }
        } else {
            log.debug("QR generation skipped: qrMode=${sessionState.qrMode}, engagement=${currentEngagement?.id}")
            null
        }

        // UI Event handlers - delegate to engagement manager (no remember - same pattern as CredentialListPresenterImpl)
        val onEvent: (UiStateEvent) -> Unit = { event ->
                log.debug("Handling UI event: $event")
                when (event) {
                    UiStateEvent.ShowQr -> {
                        log.debug("User clicked 'Show QR' - switching to Display QR mode")
                        // Direct state change like CredentialListPresenterImpl pattern
                        showQrScanner = false
                        log.debug("showQrScanner set to: $showQrScanner")
                        // Only async work needs to be in a coroutine
                        presenterScope.launch {
                            val engagementResult = engagementManager.createEngagement {
                                engagement { qr {} }
                                retrieval { ble { centralClientMode = true; peripheralServerMode = false } }
                            }
                            engagementResult.onSuccess { engagement ->
                                engagement.start()
                                log.debug("QR engagement created and started: ${engagement.id}")
                            }.onFailure { error ->
                                log.error("QR engagement creation failed: ${error.message}")
                            }
                        }
                    }

                    UiStateEvent.ShowQrScanner -> {
                        log.debug("User clicked 'Scan QR' - switching to Scan QR mode")
                        // Direct state change like CredentialListPresenterImpl does for pendingDelete
                        showQrScanner = true
                        log.debug("showQrScanner set to: $showQrScanner")
                    }

                    UiStateEvent.Stopped -> {
                        log.debug("Stopped event - closing all engagements and navigating back")
                        presenterScope.launch {
                            showQrScanner = false
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
        }

        // Handle QR code scanned for reverse engagement (no remember - same pattern as CredentialListPresenterImpl)
        val onQrScanned: (String) -> Unit = { scannedData ->
                log.info("📷 QR SCANNED! Length: ${scannedData.length}, First 30 chars: '${scannedData.take(30)}'")
                log.debug("Full scanned data: $scannedData")
                when {
                    // 18013-7 website via deeplink/QR
                    scannedData.startsWith("mdoc://") -> {
                        log.info("VALID mdoc:// URI - Initiating toApp with website retrieval (18013-7)")
                        showQrScanner = false
                        presenterScope.launch {
                            runCatching {
                                engagementManager.toApp(scannedData)
                                    .onSuccess {
                                        log.debug("toApp (website) initiated successfully")
                                    }
                                    .onFailure { error ->
                                        log.error(
                                            "toApp (website) failed: ${error.message}",
                                            exception = (error as? com.sphereon.core.api.error.IdkError)?.exception
                                        )
                                        showQrScanner = false
                                    }
                            }.onFailure { e ->
                                log.error("Exception calling toApp (website)", exception = e)
                                showQrScanner = false
                            }
                        }
                    }

                    // 18013-5 reverse engagement with BLE transfer
                    scannedData.startsWith("mdoc:") && !scannedData.startsWith("mdoc://") && !scannedData.startsWith("mdoc-openid4vp://") -> {
                        log.info("VALID mdoc: URI - Initiating toApp with BLE retrieval (18013-5)")
                        showQrScanner = false
                        presenterScope.launch {
                            runCatching {
                                engagementManager.toApp(scannedData)
                                    .onSuccess {
                                        log.debug("toApp (BLE) initiated successfully")
                                    }
                                    .onFailure { error ->
                                        log.error(
                                            "toApp (BLE) failed: ${error.message}",
                                            exception = (error as? com.sphereon.core.api.error.IdkError)?.exception
                                        )
                                        showQrScanner = false
                                    }
                            }.onFailure { e ->
                                log.error("Exception calling toApp (BLE)", exception = e)
                                showQrScanner = false
                            }
                        }
                    }

                    // OpenID4VP
                    scannedData.startsWith("mdoc-openid4vp://") -> {
                        log.info("VALID mdoc-openid4vp:// URI - Initiating toApp with OID4VP retrieval")
                        showQrScanner = false
                        presenterScope.launch {
                            runCatching {
                                engagementManager.toApp(scannedData)
                                    .onSuccess {
                                        log.debug("toApp (OID4VP) initiated successfully")
                                    }
                                    .onFailure { error ->
                                        log.error(
                                            "toApp (OID4VP) failed: ${error.message}",
                                            exception = (error as? com.sphereon.core.api.error.IdkError)?.exception
                                        )
                                        showQrScanner = false
                                    }
                            }.onFailure { e ->
                                log.error("Exception calling toApp (OID4VP)", exception = e)
                                showQrScanner = false
                            }
                        }
                    }

                    else -> {
                        log.warn("⚠️ Invalid QR code: Expected 'mdoc://', 'mdoc:', or 'mdoc-openid4vp://' - got prefix: ${scannedData.take(20)}")
                    }
                }
        }

        val onContinue: (MapDrivenDocRequestSelector) -> Unit = { selector ->
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
        }

        // Track if we've ever had an active engagement in this presenter session
        val hasHadEngagement = remember { mutableStateOf(false) }
        if (activeEngagement != null && !hasHadEngagement.value) {
            hasHadEngagement.value = true
        }

        // Debug: Log qrImage state on every recomposition
        log.debug("QR Image state: qrImage=${if (qrImage != null) "EXISTS (${qrImage.hashCode()})" else "NULL"}")

        // Build model based purely on SessionUiState - it's the source of truth
        // Special case: If TERMINAL state but no active engagement AND we never had an engagement,
        // this is stale state from a previous session - treat as INITIAL with toggle UI
        val model = when {
            sessionState.phase == UiPhase.TERMINAL && activeEngagement == null && !hasHadEngagement.value -> {
                log.debug(">>> Stale TERMINAL state detected (never had engagement in this session) - returning Engagement with toggle")
                MdocEngagementPresenter.Model.Engagement(
                    qrImage = null,
                    engagementEvent = null,
                    showQr = false,
                    showQrScanner = showQrScanner,
                    onQrScanned = onQrScanned,
                    onStateEvent = onEvent
                )
            }

            // If phase is ENGAGEMENT but there's no active engagement, it's stale state - treat as ready for engagement
            sessionState.phase == UiPhase.ENGAGEMENT && activeEngagement == null -> {
                log.debug(">>> Stale ENGAGEMENT state detected (no active engagement) - returning Engagement with toggle")
                MdocEngagementPresenter.Model.Engagement(
                    qrImage = null,
                    engagementEvent = null,
                    showQr = false,
                    showQrScanner = showQrScanner,
                    onQrScanned = onQrScanned,
                    onStateEvent = onEvent
                )
            }

            sessionState.phase == UiPhase.ENGAGEMENT -> {
                val shouldShowQr = sessionState.qrMode == QrMode.DISPLAY && !showQrScanner
                val qrForModel = if (shouldShowQr) qrImage else null
                log.debug(">>> Returning Model: ENGAGEMENT (showQr=$shouldShowQr, showQrScanner=$showQrScanner, qrForModel=${if (qrForModel != null) "EXISTS" else "NULL"})")
                // Always return Engagement model to avoid slide animations
                // When showQrScanner=true, prioritize scanner UI over Display QR
                MdocEngagementPresenter.Model.Engagement(
                    qrImage = qrForModel,
                    engagementEvent = null,
                    showQr = shouldShowQr,
                    showQrScanner = showQrScanner,
                    onQrScanned = onQrScanned,
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
                        log.debug(">>> Returning Model: ENGAGEMENT (phase=TERMINAL, terminalOutcome=null)")
                        MdocEngagementPresenter.Model.Engagement(
                            qrImage = null,
                            engagementEvent = null,
                            showQr = false,
                            showQrScanner = showQrScanner,
                            onQrScanned = onQrScanned,
                            onStateEvent = onEvent
                        )
                    }
                }
            }

            else -> {
                log.debug(">>> Returning Model: ENGAGEMENT (phase=${sessionState.phase})")
                MdocEngagementPresenter.Model.Engagement(
                    qrImage = null,
                    engagementEvent = null,
                    showQr = false,
                    showQrScanner = showQrScanner,
                    onQrScanned = onQrScanned,
                    onStateEvent = onEvent
                )
            }
        }

        return model
    }

    @Inject
    class Factory(
        private val factory: (MoleculePresenter<Unit, *>) -> MdocEngagementPresenterImpl
    ) {
        fun createTestAppTemplatePresenter(
            presenter: MoleculePresenter<Unit, *>,
        ): MdocEngagementPresenterImpl = factory(presenter)
    }

    private companion object {
        const val MIN_DOC_REQUESTS = 1
    }
}
