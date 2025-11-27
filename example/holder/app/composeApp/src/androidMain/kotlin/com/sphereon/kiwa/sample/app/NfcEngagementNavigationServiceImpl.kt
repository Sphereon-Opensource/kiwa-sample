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
import com.sphereon.di.context.UserContextManager
import com.sphereon.di.session.SessionContext
import com.sphereon.di.session.SessionScope
import com.sphereon.mdoc.engagement.MdocEngagementEvent
import com.sphereon.mdoc.engagement.MdocEngagementManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
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
 * @param nfcNavigationTrigger Navigation trigger for NFC engagement screens
 * @param appServices Application services providing access to authentication bridge
 */
@Inject
@ContributesBinding(SessionScope::class)
class NfcEngagementNavigationServiceImpl(
    log: SessionLogService,
    val context: SessionContext,
    private val engagementManager: MdocEngagementManager,
    private val userContextManager: UserContextManager,
    private val nfcNavigationTrigger: INfcNavigationTrigger,
    private val appServices: AppServices,
) : NfcEngagementNavigationService {

    /** Logger instance tagged with the service name for tracking operations. */
    private val log = log.logManager.withTag("NfcEngagementNavigationService")

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

    /** Tracks the engagement ID for which we've already triggered navigation to avoid duplicates */
    @Volatile
    private var navigatedEngagementId: String? = null

    init {
        this.log.info("NFCNAV: NfcEngagementNavigationService initialized")
        this.log.info("NFCNAV DEBUG: NfcEngagementNavigationService created in context: $context")
        this.log.info("NFCNAV DEBUG: Context is anonymous: ${context.isAnonymous()}")
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

    override fun navigateToNfcEngagement(): Boolean {
        log.info("NFCNAV: Direct navigation triggered for engagement")
        return nfcNavigationTrigger.navigateToNfcEngagement()
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
        log.info("User Context: ${context.context}")
        log.info("Session Context: ${context}")
        log.info("NFCNAV: Engagement Manager Instance: ${engagementManager.hashCode()}")
        log.info("NFCNAV: ################################")
        monitoringJob?.cancel()
        monitoringJob = null

        if (userContextManager.isAnonymous()) {
            log.warn("NFCNAV: Not monitoring engagement events for anonymous session")
            return
        }

        monitoringJob = serviceScope.launch {
            log.info("NFCNAV: Started monitoring for NFC engagement")
            log.info("NFCNAV: Collecting from engagement manager: ${engagementManager.hashCode()}")

            // Monitor engagement events directly - SessionUiProjector ignores Connecting events!
            // So we can't rely on sessionState.phase transitions for NFC engagements
            engagementManager.eventHub.engagementEvents.collect { event ->
                log.info("NFCNAV: *** Engagement event received: $event")

                // When we get a Connecting event for an NFC engagement, navigate to the screen
                if (event is MdocEngagementEvent.Connecting) {
                    val nfcEng = engagementManager.nfcEngagement.value
                    if (nfcEng != null && nfcEng.id.toString() != navigatedEngagementId) {
                        log.info("NFCNAV: NFC Connecting event for engagement ${nfcEng.id}, navigating to engagement screen")
                        navigatedEngagementId = nfcEng.id.toString()
                        handleNfcConnectingEvent()
                    } else {
                        log.debug("NFCNAV: Connecting event but no NFC engagement or already navigated")
                    }
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
        navigatedEngagementId = null // Reset to allow navigation for new engagements
    }

    private suspend fun handleConnectedEvent() {
        val engagement = engagementManager.nfcEngagement.value
        if (engagement == null) {
            log.warn("NFCNAV: Connected event received but no current engagement available")
            return
        }
        try {
            val success = navigateToNfcEngagement()
            if (success) {
                log.info("NFCNAV: Successfully triggered direct navigation")
            } else {
                log.warn("NFCNAV: Failed to trigger direct navigation")
            }

        } catch (e: Exception) {
            log.error("NFCNAV: Failed to start transfer manager: ${e.message}")
        }
    }

    private suspend fun handleNfcTransferPhase() {
        val engagement = engagementManager.nfcEngagement.value
        if (engagement == null) {
            log.warn("NFCNAV: NFC Transfer phase detected but no current engagement available")
            return
        }
        try {
            val success = navigateToNfcEngagement()
            if (success) {
                log.info("NFCNAV: Successfully navigated to engagement screen for NFC transfer")
            } else {
                log.warn("NFCNAV: Failed to navigate to engagement screen")
            }

        } catch (e: Exception) {
            log.error("NFCNAV: Failed to handle NFC transfer phase: ${e.message}")
        }
    }

    private suspend fun handleNfcConnectingEvent() {
        val engagement = engagementManager.nfcEngagement.value
        if (engagement == null) {
            log.warn("NFCNAV: NFC Connecting event but no current engagement available")
            return
        }
        try {
            val success = navigateToNfcEngagement()
            if (success) {
                log.info("NFCNAV: Successfully navigated to engagement screen for NFC connecting")
            } else {
                log.warn("NFCNAV: Failed to navigate to engagement screen")
            }

        } catch (e: Exception) {
            log.error("NFCNAV: Failed to handle NFC connecting event: ${e.message}")
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

}
