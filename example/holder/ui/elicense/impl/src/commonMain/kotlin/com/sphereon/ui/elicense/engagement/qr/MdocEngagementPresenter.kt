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

package com.sphereon.ui.elicense.engagement.qr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import com.sphereon.core.api.ISessionLogService
import com.sphereon.di.session.SureSessionScope
import com.sphereon.mdoc.IMdocSignService
import com.sphereon.mdoc.data.device.DeviceRequest
import com.sphereon.mdoc.engagement.EngagementEvent
import com.sphereon.mdoc.engagement.IEngagementInstance
import com.sphereon.mdoc.engagement.IMdocEngagementManager
import com.sphereon.mdoc.engagement.MdocEngagementState
import com.sphereon.mdoc.transfer.ITransferManager
import com.sphereon.mdoc.transfer.MapDrivenDocRequestSelector
import com.sphereon.ui.core.backstack.LocalBackstackScope
import com.sphereon.ui.elicense.engagement.consent.IMdocInformationRequestPresenter
import com.sphereon.ui.elicense.engagement.qr.IMdocEngagementQrPresenter.UiStateEvent

/**
 * MdocEngagementPresenter
 */
@Inject
@ContributesBinding(SureSessionScope::class)
class MdocEngagementPresenter(
    private val qrCodeGenerator: IQrGenerator,
    private val engagementManager: IMdocEngagementManager,
    private val infoRequestPresenter: IMdocInformationRequestPresenter,
    private val mdocSignService: IMdocSignService,
    private val log: ISessionLogService
) : IMdocEngagementQrPresenter {

    /**
     * The onStateEvent callback is because this is test code, allowing the UI to callback to update the state in the presenter.
     * It mainly exists because the button behavior is now part of the mdoc QR presentation. In production code that likely should be separated.
     */

    @Composable
    override fun present(input: IMdocEngagementQrPresenter.Input): IMdocEngagementQrPresenter.Model {
        // Long-lived scope that survives recompositions and is canceled only when this composable leaves composition
        val presenterScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
        DisposableEffect(Unit) {
            println("ENGAGEMENT QR DISPOSABLE EFFECT. CANCELING PRESENTER SCOPE")
            onDispose { presenterScope.cancel() }
        }

        var uiState by remember {
            mutableStateOf<UiStateEvent>(
                // Always start with Engagement state, we'll transition appropriately based on events
                UiStateEvent.Engagement.also {
                    if (input.existingEngagement != null) {
                        println("Starting with Engagement state for NFC engagement (will transition based on events)")
                    } else {
                        println("Starting with Engagement state for new engagement")
                    }
                }
            )
        }
        val engagementEvent = remember { MutableStateFlow<EngagementEvent?>(null) }
        val engagement = remember {
            mutableStateOf<IEngagementInstance?>(input.existingEngagement).also {
                println("=== ENGAGEMENT INITIALIZATION ===")
                println("Input existingEngagement: ${input.existingEngagement}")
                println("Input existingTransferManager: ${input.existingTransferManager}")
                println("Engagement instance set to: ${it.value}")
                println("=== END ENGAGEMENT INITIALIZATION ===")
            }
        }
        val qrImage = remember { mutableStateOf<ImageBitmap?>(null) }
        var showQr by remember { mutableStateOf(false) }
        val backstack = checkNotNull(LocalBackstackScope.current)
        val transferManager = remember { mutableStateOf<ITransferManager?>(input.existingTransferManager) }
        val holderDeviceRequest = remember { mutableStateOf<DeviceRequest?>(null) }

        val handledConnected = remember { mutableStateOf(false) }
        val started = remember { mutableStateOf(false) }
        val restartCounter = remember { mutableStateOf(0) }

        @Suppress("NewApi")
        fun cleanup() {
            qrImage.value = null

            // IMPORTANT: For NFC handover scenarios, we should NOT close the transfer manager
            // unless we're absolutely sure the transaction is complete or failed
            // The transfer manager was created by the NFC service and may still be needed
            if (input.existingTransferManager == null) {
                // Only close transfer manager if we created it ourselves (not from NFC)
                transferManager.value?.let { tm ->
                    try {
                        println("Closing self-created transfer manager")
                        tm.close()
                    } catch (e: Exception) {
                        println("Exception while closing transfer manager: ${e.message}")
                        // Handle close exception gracefully
                    }
                }
                transferManager.value = null
            } else {
                // For NFC handover, just clear the reference but don't close
                println("Clearing reference to NFC transfer manager (not closing)")
                transferManager.value = null
            }

            // Only close engagement if we created it (not from NFC)
            if (input.existingEngagement == null) {
                engagement.value?.let { eng ->
                    try {
                        println("Closing self-created engagement")
                        eng.close()
                    } catch (e: Exception) {
                        println("Exception while closing engagement: ${e.message}")
                        // Handle close exception gracefully
                    }
                }
                engagement.value = null
            } else {
                // For NFC handover, just clear the reference but don't close
                println("Clearing reference to NFC engagement (not closing)")
                engagement.value = null
            }

            engagementEvent.value = null
            holderDeviceRequest.value = null
            showQr = false
            handledConnected.value = false
            started.value = false
        }

        // Kick off long-running background work exactly once when we first enter Engagement state.
        // This survives recompositions and later uiState changes.
        LaunchedEffect(restartCounter.value) {
            if (!started.value && uiState is UiStateEvent.Engagement) {
                println("INITIALIZING ENGAGEMENT (one-time) because uiState is $uiState")
                started.value = true

                presenterScope.launch {
                    try {
                        if (engagement.value == null) {
                            // Create new engagement only if not provided via NFC
                            val engagementInstance = engagementManager.init {
                                engagement { qr {} }
                                retrieval {
                                    ble {
                                        centralClientMode = true
                                        peripheralServerMode = false
                                    }
                                }
                            }.value
                            engagement.value = engagementInstance

                            // Generate initial QR
                            qrImage.value = if ((engagementEvent.value?.state?.order ?: 0) <= MdocEngagementState.CONNECTING.order) {
                                println("Generating initial QR code for new engagement")
                                qrCodeGenerator.generateQr(engagement.value!!.getEngagementUri())
                            } else {
                                null
                            }

                            // Start engagement in the long-lived scope; this work continues across recompositions
                            presenterScope.launch {
                                val tm = engagementInstance.start()
                                transferManager.value = tm
                            }
                        }

                        // Listen to events from the engagement instance (whether new or existing)
                        val engagementInstance = engagement.value!!
                        engagementInstance.events
                            .onEach { engagementEvent.value = it }
                            .launchIn(presenterScope)

                        // If we have an existing engagement from NFC, immediately check its current state
                        if (input.existingEngagement != null) {
                            println("Checking current state of existing NFC engagement")
                            // Try to get the current state immediately
                            presenterScope.launch {
                                // For NFC engagements, we likely want to move quickly to connecting/connected state
                                val currentEvents = engagementInstance.events.replayCache
                                if (currentEvents.isNotEmpty()) {
                                    val latestEvent = currentEvents.last()
                                    println("Found cached event for NFC engagement: ${latestEvent.state}")
                                    engagementEvent.value = latestEvent

                                    // Update UI state based on engagement state
                                    when (latestEvent.state.order) {
                                        MdocEngagementState.CONNECTING.order -> {
                                            if (uiState != UiStateEvent.Connecting) {
                                                uiState = UiStateEvent.Connecting
                                            }
                                        }

                                        in MdocEngagementState.CONNECTED.order..Int.MAX_VALUE -> {
                                            if (uiState != UiStateEvent.Connecting) {
                                                uiState = UiStateEvent.Connecting
                                            }
                                        }
                                    }
                                } else {
                                    println("No cached events for NFC engagement, will wait for events")
                                }
                            }
                        }

                        // React to engagement state changes from a STABLE scope (no LaunchedEffect keyed by changing flows)
                        engagementInstance.events
                            .onEach { engagementEvent ->
                                val state = engagementEvent.state
                                println("STATE: $state")
                                println("EVENT: $engagementEvent")
                                println("Current transfer manager: ${transferManager.value}")
                                println("Transfer manager class: ${transferManager.value?.javaClass?.simpleName}")

                                if (state.order >= MdocEngagementState.CONNECTING.order) {
                                    println("Transfer manager available: ${transferManager.value != null}")

                                    // Hide QR once someone starts connecting
                                    qrImage.value = null
                                    showQr = false

                                    // Don't override Success state with Connecting state
                                    if (uiState !is UiStateEvent.Success && uiState !is UiStateEvent.SuccessComplete) {
                                        // Show intermediate Connecting screen until we transition to Select
                                        if (uiState !is UiStateEvent.Select) {
                                            uiState = UiStateEvent.Connecting
                                        }
                                    }

                                    if (state == MdocEngagementState.CONNECTED && !handledConnected.value) {
                                        handledConnected.value = true

                                        presenterScope.launch {
                                            // CRITICAL: For NFC handover, we MUST use the existing transfer manager
                                            // Creating a new one will break the BLE connection established during handover
                                            val tm = transferManager.value
                                            if (tm == null) {
                                                println("Transfer manager is null, cannot proceed")
                                                return@launch
                                            }
                                            println("Using existing transfer manager: ${tm.javaClass.simpleName}")
                                            println("Attempting to receive device request...")
                                            println("This call may block if no data is available in the BLE channel")

                                            // Add timeout wrapper around the receive call
                                            try {
                                                println("Starting receiveDeviceRequest() call...")
                                                val startTime = System.currentTimeMillis()

                                                val deviceRequestResult = tm.receiveDeviceRequest()

                                                val endTime = System.currentTimeMillis()
                                                val duration = endTime - startTime
                                                println("receiveDeviceRequest() completed in ${duration}ms")

                                                if (deviceRequestResult.isErr) {
                                                    println(
                                                        "Device request failed: ${deviceRequestResult.error.message.defaultMessage}"
                                                    )
                                                    println("Error details: ${deviceRequestResult.error.exception}")

                                                    presenterScope.cancel()
                                                    return@launch
                                                }

                                                val deviceRequest = deviceRequestResult.value
                                                println("Successfully received device request: $deviceRequest")
                                                println("Device request class: ${deviceRequest.javaClass.simpleName}")
                                                println("Device request details: $deviceRequest")

                                                holderDeviceRequest.value = deviceRequest
                                                uiState = UiStateEvent.Select
                                            } catch (e: Exception) {
                                                if (e is CancellationException) {
                                                    throw e
                                                } else {
                                                    println("EXCEPTION during receiveDeviceRequest(): ${e.message}")
                                                    e.printStackTrace()

                                                    // Reset the guard so we can retry if needed
                                                    handledConnected.value = false
                                                    throw e
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    println("$state STATE!")
                                }
                            }
                            .launchIn(presenterScope)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        println("ENGAGEMENT ERROR: ${e.message} ,$e")
                        e.printStackTrace()
                        println("Caught exception, cancelling presenterScope")
                        presenterScope.cancel()
                    }
                }
            } else {
                println("NOT INITIALIZING. started=${started.value}, uiState=$uiState")
            }
        }

        // Keep this to react to other uiState updates (no long-running work here).
        // We DO NOT kick off the long-lasting background work here anymore; that happens in the LaunchedEffect(Unit) above.
        LaunchedEffect(uiState) {
            println("UI STATE CHANGE: $uiState")
            when (uiState) {
                is UiStateEvent.Engagement -> {
                    println("UiStateEvent.Engagement observed (no-op if already started). started=${started.value}")
                    // Do not start engagement again; handled by the one-time LaunchedEffect(Unit).
                    // You can still perform light UI setup here if needed (do not start long suspending work).
                    // Only generate QR code if we don't have an existing engagement from NFC
                    if (input.existingEngagement == null && engagement.value != null && (engagementEvent.value?.state?.order ?: 0) <= MdocEngagementState.CONNECTING.order) {
                        println("Generating QR code for new engagement")
                        qrImage.value = qrCodeGenerator.generateQr(engagement.value!!.getEngagementUri())
                    } else if (input.existingEngagement != null) {
                        println("Skipping QR generation - using existing NFC engagement")
                        qrImage.value = null // Make sure no QR is shown for NFC
                        showQr = false
                    }
                }

                is UiStateEvent.Connecting -> {
                }

                is UiStateEvent.Initial -> {
                    qrImage.value = null
                    showQr = false
                }

                is UiStateEvent.ShowQr -> {
                    // Only show QR if we don't have existing engagement
                    if (input.existingEngagement == null) {
                        showQr = true
                    }
                }

                is UiStateEvent.Success -> {
                    // Success state - no delay here, renderer will control timing
                    // and trigger navigation when ready
                }

                is UiStateEvent.SuccessComplete -> {
                    // Navigation handled by onEvent callback
                }

                is UiStateEvent.Select -> {
                    // No special handling needed for Select state
                }

                is UiStateEvent.Stopped -> {
                    cleanup()
                }
            }
        }

        // expose latest event to Compose
        val event by engagementEvent.collectAsState()

        println("EVENT: $event")

        // IMPORTANT: We removed LaunchedEffect(event) that previously performed long-running work.
        // That logic is now in the stable presenterScope collector so recompositions won't cancel it.

        val onEvent: (UiStateEvent) -> Unit = { uiStateEvent ->
            println("ON_EVENT: Received event: $uiStateEvent, current uiState: $uiState")
            when (uiStateEvent) {
                UiStateEvent.Stopped -> {
                    println("ON_EVENT: Processing Stopped event")
                    cleanup()
                    uiState = UiStateEvent.Engagement
                    restartCounter.value += 1 // Trigger restart
                }

                UiStateEvent.Success -> {
                    println("ON_EVENT: Processing Success event - transitioning to Success state")
                    uiState = UiStateEvent.Success
                }

                UiStateEvent.SuccessComplete -> {
                    println("ON_EVENT: Processing SuccessComplete event - cleaning up and popping backstack")
                    cleanup()
                    backstack.pop() // Return to credential list
                }

                else -> {
                    println("ON_EVENT: Processing other event: $uiStateEvent")
                    uiState = uiStateEvent
                }
            }
        }

        return when (uiState) {
            is UiStateEvent.Initial -> {
                println("RENDERING: Initial state")
                IMdocEngagementQrPresenter.Model.Initial(
                    onStateEvent = onEvent
                )
            }

            is UiStateEvent.Engagement, is UiStateEvent.ShowQr -> {
                println("RENDERING: Engagement state (showQr=${uiState is UiStateEvent.ShowQr})")
                IMdocEngagementQrPresenter.Model.Engagement(
                    qrImage = qrImage.value,
                    engagementEvent = event,
                    showQr = showQr,
                    onStateEvent = onEvent
                )
            }

            is UiStateEvent.Stopped -> {
                println("RENDERING: Stopped state")
                IMdocEngagementQrPresenter.Model.Stopped(
                    engagementEvent = event,
                    onStateEvent = onEvent
                )
            }

            is UiStateEvent.Select -> {
                println("RENDERING: Select state")
                // TODO, move Event to class and simply have the devicerequest as param instead of as class property
                val deviceRequest =
                    checkNotNull(holderDeviceRequest.value) { "DeviceRequest must be available in Select state" }
                val onContinue: (MapDrivenDocRequestSelector) -> Unit = { docRequestSelector ->
                    println("SELECT: onContinue callback triggered")
                    val selectorWithDeviceSign = docRequestSelector.copy(
                        sessionTranscript = transferManager.value!!.getSessionTranscript().data(),
                        mdocDeviceSignService = mdocSignService
                    )
                    presenterScope.launch {
                        val tm = transferManager.value
                        if (tm == null) {
                            println("Transfer manager is null, cannot proceed")
                            return@launch
                        }
                        tm.registerCustomResponseSelectors(
                            requestResponseProcesser = selectorWithDeviceSign, // Use default
                            requestDocumentsSelector = selectorWithDeviceSign,
                            docRequestSingleDocumentSelector = selectorWithDeviceSign
                        )
                        val resp = tm.createDeviceResponse(deviceRequest, selectorWithDeviceSign) // Supplier not needed
                        if (resp.isOk) {
                            println("SUCCESS: About to send device response...")
                            val sendStartTime = System.currentTimeMillis()
                            tm.sendDeviceResponse(resp.value)
                            val sendEndTime = System.currentTimeMillis()
                            println("SUCCESS: Device response sent in ${sendEndTime - sendStartTime}ms")

                            // Transition to success state immediately after sending
                            println("SUCCESS: Transitioning to Success state")
                            uiState = UiStateEvent.Success
                        } else {
                            println("ERROR: Device response creation failed: ${resp.error}")
                            // Could surface error to UI; for now we reset to selecting
                            uiState = UiStateEvent.Select
                        }
                    }
                }
                val consent = infoRequestPresenter.present(
                    IMdocInformationRequestPresenter.Input(deviceRequest, onContinue)
                )
                IMdocEngagementQrPresenter.Model.Selecting(
                    consentModel = consent,
                    onStateEvent = onEvent
                )
            }

            is UiStateEvent.Connecting -> {
                println("RENDERING: Connecting state")
                if (event == null) {
                    println("Engagement event should not be null at this point")
                }
                IMdocEngagementQrPresenter.Model.Connecting(
                    engagementEvent = event!!,
                    onStateEvent = onEvent
                )
            }

            is UiStateEvent.Success -> {
                println("RENDERING: Success state")
                IMdocEngagementQrPresenter.Model.Success(
                    onStateEvent = onEvent
                )
            }

            is UiStateEvent.SuccessComplete -> {
                println("RENDERING: SuccessComplete state (showing Success model)")
                IMdocEngagementQrPresenter.Model.Success(
                    onStateEvent = onEvent
                )
            }
        }
    }

    @Inject
    class Factory(
        private val factory: (MoleculePresenter<IMdocEngagementQrPresenter.Input, *>) -> MdocEngagementPresenter
    ) {
        fun createTestAppTemplatePresenter(
            presenter: MoleculePresenter<IMdocEngagementQrPresenter.Input, *>,
        ): MdocEngagementPresenter {
            return factory(presenter)
        }
    }
}
