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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.sphereon.core.api.SessionLogManager
import com.sphereon.di.session.SessionScope
import com.sphereon.kiwa.sample.ui.core.backstack.LocalBackstackScope
import com.sphereon.kiwa.sample.ui.elicense.engagement.consent.MdocInformationRequestPresenter
import com.sphereon.kiwa.sample.ui.elicense.engagement.qr.MdocEngagementPresenter.UiStateEvent
import com.sphereon.mdoc.MdocSignService
import com.sphereon.mdoc.data.device.DeviceRequest
import com.sphereon.mdoc.engagement.EngagementEvent
import com.sphereon.mdoc.engagement.EngagementInstance
import com.sphereon.mdoc.engagement.MdocEngagementManager
import com.sphereon.mdoc.engagement.MdocEngagementState
import com.sphereon.mdoc.transfer.TransferManager
import com.sphereon.mdoc.transfer.MapDrivenDocRequestSelector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
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

    private val log = logManager.withTagSync("MdocEngagementPresenter")

    // Consolidated state
    private data class EngagementState(
        val engagement: EngagementInstance? = null,
        val transferManager: TransferManager? = null,
        val deviceRequest: DeviceRequest? = null,
        val phase: EngagementPhase = EngagementPhase.INITIALIZING,
        val showQr: Boolean = false,
        val qrImage: ImageBitmap? = null,
        val error: EngagementError? = null,
        val isCleaned: Boolean = false
    )

    // Explicit phases
    private enum class EngagementPhase {
        INITIALIZING, ENGAGING, CONNECTING, SELECTING, SHARING, SUCCESS, ERROR
    }

    // Error handling
    private sealed class EngagementError {
        object InitializationFailed : EngagementError()
        object DeviceRequestFailed : EngagementError()
        object SharingFailed : EngagementError()
    }

    @Composable
    override fun present(input: MdocEngagementPresenter.Input): MdocEngagementPresenter.Model {
        val backstack = checkNotNull(LocalBackstackScope.current)
        val presenterScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }

        var state by remember {
            mutableStateOf(
                EngagementState(
                    engagement = input.existingEngagement,
                    transferManager = input.existingTransferManager,
                    phase = if (input.existingEngagement != null) EngagementPhase.ENGAGING else EngagementPhase.INITIALIZING
                )
            )
        }

        val isNfcEngagement = input.existingEngagement != null

        // Resource cleanup - idempotent
        fun cleanupEngagement() {
            if (state.isCleaned) {
                log.debug("Engagement already cleaned up, skipping")
                return
            }

            log.debug("Cleaning up engagement resources")
            state.engagement?.let { engagement ->
                runCatching {
                    @Suppress("NewApi")
                    engagement.close()
                }.onFailure { e ->
                    log.debug("Error closing engagement: ${e.message}")
                }
            }

            // Mark as cleaned up to prevent double cleanup
            state = state.copy(isCleaned = true)
        }

        fun resetToInitializingState() {
            log.debug("Resetting to initial state")
            state = EngagementState(phase = EngagementPhase.INITIALIZING)
        }

        // Cleanup on dispose - only if not already cleaned up
        DisposableEffect(Unit) {
            onDispose {
                log.debug("Disposing presenter")
                presenterScope.cancel()
                if (!state.isCleaned) {
                    log.debug("Cleanup on dispose")
                    runCatching { cleanupEngagement() }
                } else {
                    log.debug("Skipping cleanup on dispose - already cleaned up")
                }
            }
        }

        // Engagement initialization hook
        UseEngagementInitialization(
            shouldInitialize = state.engagement == null,
            engagementManager = engagementManager,
            presenterScope = presenterScope,
            onEngagementCreated = { engagement ->
                log.debug("Engagement created successfully")
                state = state.copy(engagement = engagement, phase = EngagementPhase.ENGAGING)
            },
            onTransferManagerReady = { transferManager ->
                log.debug("Transfer manager ready")
                state = state.copy(transferManager = transferManager)
            },
            onError = { error ->
                log.debug("Engagement initialization failed: ${error.message}")
                state = state.copy(error = EngagementError.InitializationFailed, phase = EngagementPhase.ERROR)
            }
        )

        // Engagement events
        val engagementEvent by state.engagement?.events?.collectAsState(initial = null) ?: remember { mutableStateOf(null) }

        // QR generation hook
        UseQrGeneration(
            engagement = state.engagement,
            showQr = state.showQr,
            qrCodeGenerator = qrCodeGenerator,
            onQrGenerated = { qrImage ->
                log.debug("QR code generated")
                state = state.copy(qrImage = qrImage)
            },
            onQrCleared = {
                state = state.copy(qrImage = null)
            }
        )

        // Auto-hide QR when connecting hook
        UseQrAutoHide(
            engagementEvent = engagementEvent,
            currentShowQr = state.showQr,
            onHideQr = {
                log.debug("Auto-hiding QR due to connection state")
                state = state.copy(showQr = false)
            }
        )

        // Device request handling hook
        UseDeviceRequestHandler(
            engagementEvent = engagementEvent,
            transferManager = state.transferManager,
            hasDeviceRequest = state.deviceRequest != null,
            presenterScope = presenterScope,
            onDeviceRequestReceived = { deviceRequest ->
                log.debug("Device request received")
                state = state.copy(deviceRequest = deviceRequest, phase = EngagementPhase.SELECTING)
            },
            onError = { error ->
                log.debug("Device request failed: ${error.message}")
                state = state.copy(error = EngagementError.DeviceRequestFailed, phase = EngagementPhase.ERROR)
            }
        )

        // Update phase based on engagement events
        LaunchedEffect(engagementEvent?.state) {
            val newPhase = when (val currentState = engagementEvent?.state) {
                MdocEngagementState.CONNECTING, MdocEngagementState.CONNECTED -> {
                    log.debug("Engagement state changed to: $currentState")
                    EngagementPhase.CONNECTING
                }
                else -> state.phase
            }
            if (newPhase != state.phase && state.deviceRequest == null) {
                state = state.copy(phase = newPhase)
            }
        }

        // Memoized event handlers
        val onEvent = remember {
            { event: UiStateEvent ->
                log.debug("Handling UI event: $event")
                when (event) {
                    UiStateEvent.ShowQr -> {
                        if (!isNfcEngagement) {
                            log.debug("Showing QR code")
                            state = state.copy(showQr = true)
                        }
                    }
                    UiStateEvent.Stopped -> {
                        log.debug("Stopped event - cleaning up and resetting")
                        cleanupEngagement()
                        resetToInitializingState()
                    }
                    UiStateEvent.Success -> {
                        log.debug("Engagement completed successfully")
                        state = state.copy(phase = EngagementPhase.SUCCESS)
                    }
                    UiStateEvent.SuccessComplete -> {
                        log.debug("Success flow complete - cleaning up and navigating back")
                        cleanupEngagement()
                        backstack.pop()
                    }
                    else -> {
                        log.debug("Unhandled event: $event")
                    }
                }
                Unit
            }
        }

        val onContinue = remember {
            { selector: MapDrivenDocRequestSelector ->
                log.debug("Document selection confirmed, starting sharing")
                state = state.copy(phase = EngagementPhase.SHARING)

                presenterScope.launch {
                    runCatching {
                        val tm = checkNotNull(state.transferManager) { "Transfer manager not available" }
                        val dr = checkNotNull(state.deviceRequest) { "Device request not available" }

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
                            log.debug("Device response sent successfully")
                            onEvent(UiStateEvent.Success)
                        } else {
                            log.debug("Device response creation failed: ${response.error}")
                            throw Exception("Failed to create device response: ${response.error}")
                        }
                    }.onFailure { e ->
                        log.debug("Document sharing failed: ${e.message}")
                        state = state.copy(error = EngagementError.SharingFailed, phase = EngagementPhase.ERROR)
                        cleanupEngagement()
                    }
                }
                Unit
            }
        }

        // Derive UI state from phase
        val uiState = when (state.phase) {
            EngagementPhase.SUCCESS -> UiStateEvent.Success
            EngagementPhase.SHARING -> UiStateEvent.Sharing
            EngagementPhase.SELECTING -> UiStateEvent.Select
            EngagementPhase.CONNECTING -> UiStateEvent.Connecting
            EngagementPhase.ERROR -> UiStateEvent.Stopped
            else -> UiStateEvent.Engagement
        }

        // Build model
        return when (uiState) {
            UiStateEvent.Initial -> MdocEngagementPresenter.Model.Initial(onEvent)

            UiStateEvent.Engagement, UiStateEvent.ShowQr -> MdocEngagementPresenter.Model.Engagement(
                qrImage = if (state.showQr) state.qrImage else null,
                engagementEvent = engagementEvent,
                showQr = state.showQr,
                onStateEvent = onEvent
            )

            UiStateEvent.Connecting -> MdocEngagementPresenter.Model.Connecting(
                engagementEvent = checkNotNull(engagementEvent) { "Engagement event required for connecting state" },
                onStateEvent = onEvent
            )

            UiStateEvent.Select -> {
                val consent = infoRequestPresenter.present(
                    MdocInformationRequestPresenter.Input(
                        checkNotNull(state.deviceRequest) { "Device request required for select state" },
                        onContinue
                    )
                )
                MdocEngagementPresenter.Model.Selecting(consent, onEvent)
            }

            UiStateEvent.Sharing -> MdocEngagementPresenter.Model.Sharing(onEvent)

            UiStateEvent.Success, UiStateEvent.SuccessComplete -> MdocEngagementPresenter.Model.Success(onEvent)

            UiStateEvent.Stopped -> MdocEngagementPresenter.Model.Stopped(engagementEvent, onEvent)
        }
    }

    // Custom hooks for side effects
    @Composable
    private fun UseEngagementInitialization(
        shouldInitialize: Boolean,
        engagementManager: MdocEngagementManager,
        presenterScope: CoroutineScope,
        onEngagementCreated: (EngagementInstance) -> Unit,
        onTransferManagerReady: (TransferManager) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        LaunchedEffect(shouldInitialize) {
            if (shouldInitialize) {
                runCatching {
                    val engagement = engagementManager.newInstance {
                        engagement { qr {} }
                        retrieval { ble { centralClientMode = true; peripheralServerMode = false } }
                    }
                    onEngagementCreated(engagement)

                    presenterScope.launch {
                        runCatching {
                            val transferManager = engagement.start()
                            onTransferManagerReady(transferManager)
                        }.onFailure(onError)
                    }
                }.onFailure(onError)
            }
        }
    }

    @Composable
    private fun UseQrGeneration(
        engagement: EngagementInstance?,
        showQr: Boolean,
        qrCodeGenerator: QrGenerator,
        onQrGenerated: (ImageBitmap) -> Unit,
        onQrCleared: () -> Unit
    ) {
        LaunchedEffect(engagement, showQr) {
            if (showQr && engagement != null) {
                runCatching {
                    val qrImage = qrCodeGenerator.generateQr(engagement.getEngagementUri())
                    onQrGenerated(qrImage)
                }.onFailure { e ->
                    log.debug("QR generation failed: ${e.message}")
                    onQrCleared()
                }
            } else {
                onQrCleared()
            }
        }
    }

    @Composable
    private fun UseQrAutoHide(
        engagementEvent: EngagementEvent?,
        currentShowQr: Boolean,
        onHideQr: () -> Unit
    ) {
        LaunchedEffect(engagementEvent?.state) {
            if (currentShowQr && engagementEvent?.state?.let { it >= MdocEngagementState.CONNECTING } == true) {
                onHideQr()
            }
        }
    }

    @Composable
    private fun UseDeviceRequestHandler(
        engagementEvent: EngagementEvent?,
        transferManager: TransferManager?,
        hasDeviceRequest: Boolean,
        presenterScope: CoroutineScope,
        onDeviceRequestReceived: (DeviceRequest) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        LaunchedEffect(engagementEvent?.state, transferManager) {
            if (engagementEvent?.state == MdocEngagementState.CONNECTED &&
                transferManager != null &&
                !hasDeviceRequest
            ) {
                presenterScope.launch {
                    runCatching {
                        val result = transferManager.tryOps().receiveDeviceRequest()
                        if (result.isOk) {
                            onDeviceRequestReceived(result.value)
                        } else {
                            throw Exception("Device request failed: ${result.error.message.defaultMessage}")
                        }
                    }.onFailure(onError)
                }
            }
        }
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
