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

package com.sphereon.kiwa.sample.app

import com.sphereon.core.api.SessionLogService
import com.sphereon.di.session.SessionContext
import com.sphereon.di.session.SessionScope
import com.sphereon.kiwa.sample.ui.elicense.engagement.qr.MdocEngagementPresenter
import com.sphereon.mdoc.engagement.EngagementEvent
import com.sphereon.mdoc.engagement.EngagementInstance
import com.sphereon.mdoc.engagement.MdocEngagementManager
import com.sphereon.mdoc.transfer.TransferManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

/**
 * Service responsible for managing NFC engagement navigation and coordination.
 *
 * This service implements [NfcEngagementNavigationService] to handle the coordination
 * between NFC engagement events and application navigation. It monitors NFC engagement
 * states and manages the transition from NFC connection establishment to document
 * transfer operations.
 *
 * Key responsibilities:
 * - Monitor NFC engagement events from the engagement manager
 * - Coordinate navigation when NFC engagements are established
 * - Handle transfer manager provisioning and lifecycle
 * - Provide integration points for external navigation systems
 * - Manage coroutine-based monitoring lifecycle
 *
 * The service operates in session scope, ensuring it has access to the current
 * user session context and can coordinate with other session-scoped services.
 *
 * @param log Session-scoped logging service for operation tracking
 * @param context Session context providing access to principal and session state
 * @param engagementManager Manager for handling mDoc engagements and their lifecycle
 * @param mdocEngagementPresenter Presenter for handling mDoc engagement UI
 */
@Inject
@ContributesBinding(SessionScope::class)
class NfcEngagementNavigationServiceImpl(
    log: SessionLogService,
    val context: SessionContext,
    private val engagementManager: MdocEngagementManager,
    private val nfcNavigationTrigger: INfcNavigationTrigger,
) : NfcEngagementNavigationService {

    /** Logger instance tagged with the service name for tracking operations. */
    private val log = log.logManager.withTagSync("NfcEngagementNavigationService")

    /** Coroutine job for managing the engagement monitoring lifecycle. */
    private var monitoringJob: Job? = null

    /**
     * Service-scoped coroutine context using the main dispatcher with supervisor job.
     *
     * Uses SupervisorJob to ensure that failure in one child coroutine doesn't
     * cancel other operations, and Dispatchers.Main.immediate for UI-related operations.
     */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * Reactive flow of pending navigation data for NFC engagements.
     * This allows UI components to observe and react to NFC engagement state changes.
     */
    private val _pendingNavigation = MutableStateFlow<NfcEngagementNavigationService.PendingNavigation?>(null)
    override val pendingNavigationFlow: StateFlow<NfcEngagementNavigationService.PendingNavigation?> = _pendingNavigation

    /** Stores the pending navigation data when NFC engagement is complete */
    @Volatile
    private var localPendingNavigation: NfcEngagementNavigationService.PendingNavigation? = null

    init {
        this.log.info("NFCNAV: NfcEngagementNavigationService initialized")
        println("NFCNAV DEBUG: NfcEngagementNavigationService created in context: $context")
        println("NFCNAV DEBUG: Context is anonymous: ${context.isAnonymous()}")
        println("NFCNAV DEBUG: Service instance: $this")
        println("NFCNAV DEBUG: Service class: ${this::class}")
    }

    /**
     * Gets the pending navigation data and clears it.
     */
    override fun consumePendingNavigation(): NfcEngagementNavigationService.PendingNavigation? {
        val pending = localPendingNavigation
        localPendingNavigation = null
        _pendingNavigation.value = null
        return pending
    }

    /**
     * Checks if there is a pending navigation.
     */
    override fun hasPendingNavigation(): Boolean = localPendingNavigation != null

    override fun navigateToNfcEngagement(engagement: EngagementInstance, transferManager: TransferManager): Boolean {
        log.info("NFCNAV: Direct navigation triggered for engagement: ${engagement.id}")
        return nfcNavigationTrigger.navigateToNfcEngagement(engagement, transferManager)
    }

    /**
     * Starts monitoring NFC engagement events.
     *
     * This method initiates the monitoring process for NFC engagements by setting up
     * event listeners and logging the current session context. The monitoring runs
     * in a separate coroutine to avoid blocking the main thread.
     *
     * If monitoring is already active, the previous monitoring job is cancelled
     * before starting a new one to prevent duplicate monitoring.
     */
    override fun startMonitoring() {
        log.info("NFCNAV: ################################")
        log.info("NFCNAV: Starting engagement monitoring")
        log.info(context.context.principal.toString())
        log.info("NFCNAV: ################################")
        monitoringJob?.cancel()

        monitoringJob = serviceScope.launch {
            engagementManager.engagementEvents.collect { event ->
                if (event is EngagementEvent.Connecting) {
                    log.info("NFCNAV: Connecting event received")
                }
                if (event is EngagementEvent.Connected) {
                    log.info("NFCNAV: Connected event received - attempting navigation")
                    handleConnectedEvent()
                }
            }
        }

    }

    /**
     * Stops monitoring NFC engagement events.
     *
     * This method cancels the active monitoring coroutine and cleans up resources.
     * It's safe to call this method multiple times or when monitoring is not active.
     */
    override fun stopMonitoring() {
        log.info("NFCNAV: Stopping engagement monitoring")
        monitoringJob?.cancel()
        monitoringJob = null
    }

    private suspend fun handleConnectedEvent() {
         val engagement = engagementManager.engagement.value
         if (engagement == null) {
             log.warn("NFCNAV: Connected event received but no current engagement available")
             return
         }

         try {
             println("NFCNAV: TODO: Check engagement start and thus transfer start!. Likely it already happened")
             val transfer = engagement.transfer()
             val transferManager = transfer.manager

             // Check if this is an anonymous session - if so, route through bridge
             val isAnonymous = context.isAnonymous()
             println("NFCNAV DEBUG: handleConnectedEvent - Session is anonymous: $isAnonymous")

             if (isAnonymous) {
                 log.info("NFCNAV: Anonymous session - routing engagement through authentication bridge")

                 // Try to get the bridge and route the engagement
                 try {
                     val bridge = MainActivity.nfcAuthenticationBridge
                     val success = bridge.routeNfcEngagementToAuthenticatedContext(engagement, transferManager)

                     if (success) {
                         log.info("NFCNAV: Successfully routed engagement to authenticated context via bridge")
                     } else {
                         log.warn("NFCNAV: Failed to route engagement via bridge - user not authenticated")
                     }
                 } catch (e: Exception) {
                     log.error("NFCNAV: Error routing engagement through bridge: ${e.message}")
                 }
             } else {
                 // For authenticated sessions, use direct navigation
                 val success = navigateToNfcEngagement(engagement, transferManager)
                 if (success) {
                     log.info("NFCNAV: Successfully triggered direct navigation")
                 } else {
                     log.warn("NFCNAV: Failed to trigger direct navigation")
                 }
             }
         } catch (e: Exception) {
             log.error("NFCNAV: Failed to start transfer manager: ${e.message}")
         }
     }

    /**
     * Handles the completion of an NFC engagement establishment.
     *
     * This method is called when an NFC engagement has been successfully established
     * and a transfer manager is available. It serves as the coordination point for
     * navigating to the appropriate engagement screen or workflow.
     *
     * @param engagement The established engagement instance
     * @param transferManager The transfer manager for handling document transfers
     *
     * TODO: Implement proper navigation mechanism integration
     */
    override fun handleNfcEngagementComplete(
        engagement: EngagementInstance,
        transferManager: TransferManager
    ) {
        log.info("NFCNAV: Engagement complete - will navigate to engagement screen")

        println("NFCNAV DEBUG: handleNfcEngagementComplete called with engagement: $engagement")

        // Check if this is an anonymous session - if so, this should be handled by the bridge
        val isAnonymous = context.isAnonymous()
        println("NFCNAV DEBUG: Session is anonymous: $isAnonymous")

        if (isAnonymous) {
            log.info("NFCNAV: Anonymous session - navigation should be handled by authentication bridge")
            println("NFCNAV DEBUG: Skipping direct navigation in anonymous session")
            return
        }

        // Only handle navigation directly for authenticated sessions
        // Store the pending navigation data in both local and global state
        val pending = NfcEngagementNavigationService.PendingNavigation(engagement, transferManager)
        localPendingNavigation = pending
        _pendingNavigation.value = pending

        println("NFCNAV DEBUG: Stored pending navigation, hasPendingNavigation: ${hasPendingNavigation()}")

        log.info("NFCNAV: Engagement complete - navigated to engagement screen")
        nfcNavigationTrigger.navigateToNfcEngagement(pending.engagement, pending.transferManager)
    }

    /**
     * Accepts an externally provided transfer manager for engagement handling.
     *
     * This method allows external components to provide a transfer manager directly,
     * typically used when the transfer manager is created outside the normal
     * engagement flow. If a current engagement exists, it proceeds with the
     * engagement completion handling.
     *
     * @param transferManager The externally provided transfer manager
     */
    override fun provideTransferManager(transferManager: TransferManager) {
        log.info("NFCNAV: External transfer manager provided")
        val engagement = engagementManager.engagement.value
        if (engagement != null) {
            handleNfcEngagementComplete(engagement, transferManager)
        } else {
            log.warn("NFCNAV: Transfer manager provided but no current engagement available")
        }
    }

    /**
     * Checks the current engagement state and triggers navigation if appropriate.
     *
     * This method performs a manual check of the current engagement state and
     * attempts to start the transfer manager if an engagement is available.
     * It's typically called when the application needs to explicitly check
     * and handle pending engagements.
     *
     * The operation runs asynchronously to avoid blocking the calling thread.
     */
    override fun checkAndTriggerNavigation() {
        val engagement = engagementManager.engagement.value
        if (engagement == null) {
            log.warn("NFCNAV: checkAndTriggerNavigation called but no current engagement available")
            return
        }

        serviceScope.launch {
            try {
                val transferManager = engagement.start()
                handleNfcEngagementComplete(engagement, transferManager)
            } catch (e: IllegalStateException) {
                log.error("NFCNAV: Failed to start transfer manager in checkAndTriggerNavigation: ${e.message}")
            }
        }
    }

    /**
     * Clears the pending navigation data.
     * This should be called when the NFC engagement is complete or cancelled.
     */
    override fun clearPendingNavigation() {
        log.info("NFCNAV: Clearing pending navigation")
        localPendingNavigation = null
        _pendingNavigation.value = null
    }

    /**
     * Creates a presenter wrapper for NFC engagement with existing engagement and transfer manager.
     * This method now delegates to the navigation trigger to avoid circular dependencies.
     */
    override fun createNfcEngagementPresenter(
        engagement: EngagementInstance,
        transferManager: TransferManager
    ): MoleculePresenter<Any, MdocEngagementPresenter.Model> {
        // This is mainly for compatibility with the interface
        // The actual navigation happens through navigateToNfcEngagement
        throw IllegalStateException("createNfcEngagementPresenter should not be called - use navigateToNfcEngagement instead")
    }
}
