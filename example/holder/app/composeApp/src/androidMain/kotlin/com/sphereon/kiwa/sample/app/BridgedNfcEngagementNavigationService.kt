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

import com.sphereon.kiwa.sample.ui.elicense.engagement.qr.MdocEngagementPresenter
import com.sphereon.mdoc.engagement.EngagementInstance
import com.sphereon.mdoc.transfer.TransferManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import software.amazon.app.platform.presenter.molecule.MoleculePresenter

/**
 * Wrapper navigation service that routes NFC engagement events through the authentication bridge.
 *
 * This service wraps the anonymous session's navigation service and intercepts engagement
 * completion events to route them through the authentication bridge to the authenticated
 * user's session context.
 *
 * This solves the multi-tenant/scope issue where:
 * - NFC service runs in anonymous context
 * - UI runs in authenticated context
 * - Need to bridge between the two contexts safely
 */
class BridgedNfcEngagementNavigationService(
    private val delegate: NfcEngagementNavigationService,
    private val bridge: NfcAuthenticationBridge
) : NfcEngagementNavigationService {

    /**
     * This bridged service maintains its own pending navigation state.
     * This ensures that the RootPresenter can observe this service's pendingNavigationFlow
     * and see pending navigation even when the actual engagement is handled in the authenticated context.
     */
    private val _pendingNavigation = MutableStateFlow<NfcEngagementNavigationService.PendingNavigation?>(null)
    override val pendingNavigationFlow: StateFlow<NfcEngagementNavigationService.PendingNavigation?> = _pendingNavigation

    /** Local storage for pending navigation data */
    @Volatile
    private var localPendingNavigation: NfcEngagementNavigationService.PendingNavigation? = null

    override fun startMonitoring() {
        println("BridgedNfcNavigation: Starting monitoring via delegate")
        println("BridgedNfcNavigation: Delegate service: $delegate")
        println("BridgedNfcNavigation: Delegate service class: ${delegate::class}")
        delegate.startMonitoring()
    }

    override fun stopMonitoring() {
        println("BridgedNfcNavigation: Stopping monitoring via delegate")
        delegate.stopMonitoring()
    }

    /**
     * Intercepts engagement completion and routes through the authentication bridge.
     * This ensures the event reaches the authenticated user's session context.
     */
    override fun handleNfcEngagementComplete(engagement: EngagementInstance, transferManager: TransferManager) {
        println("BridgedNfcNavigation: Intercepting NFC engagement completion - using direct navigation")

        // Use the new direct navigation method which handles everything internally
        val success = navigateToNfcEngagement(engagement, transferManager)

        if (success) {
            println("BridgedNfcNavigation: Successfully navigated via direct method")
        } else {
            println("BridgedNfcNavigation: Failed to navigate - user not authenticated or navigation failed")
        }
    }

    override fun provideTransferManager(transferManager: TransferManager) {
        println("BridgedNfcNavigation: Providing transfer manager via delegate")
        delegate.provideTransferManager(transferManager)
    }

    override fun checkAndTriggerNavigation() {
        println("BridgedNfcNavigation: Checking and triggering navigation via delegate")
        delegate.checkAndTriggerNavigation()
    }

    /**
     * Use the delegate's pending navigation flow for basic monitoring.
     * The actual navigation routing happens through the bridge.
     */

    override fun createNfcEngagementPresenter(
        engagement: EngagementInstance,
        transferManager: TransferManager
    ): MoleculePresenter<Any, MdocEngagementPresenter.Model> {
        // This should not be called on the anonymous context version
        // The authenticated context will handle presenter creation
        println("BridgedNfcNavigation: createNfcEngagementPresenter called - routing to authenticated context")

        val authenticatedService = bridge.getAuthenticatedNavigationService()
        return authenticatedService?.createNfcEngagementPresenter(engagement, transferManager)
            ?: throw IllegalStateException("No authenticated navigation service available for presenter creation")
    }

    override fun clearPendingNavigation() {
        println("BridgedNfcNavigation: Clearing pending navigation via delegate")
        delegate.clearPendingNavigation()

        // Also clear on bridged service
        localPendingNavigation = null
        _pendingNavigation.value = null

        // Also clear on authenticated context if available
        bridge.getAuthenticatedNavigationService()?.clearPendingNavigation()
    }

    override fun consumePendingNavigation(): NfcEngagementNavigationService.PendingNavigation? {
        println("BridgedNfcNavigation: Consuming pending navigation via delegate")
        val pending = localPendingNavigation
        localPendingNavigation = null
        _pendingNavigation.value = null
        return pending
    }

    override fun hasPendingNavigation(): Boolean {
        return localPendingNavigation != null
    }

    override fun navigateToNfcEngagement(engagement: EngagementInstance, transferManager: TransferManager): Boolean {
        println("BridgedNfcNavigation: Direct navigation called - routing to authenticated context")
        return bridge.routeNfcEngagementToAuthenticatedContext(engagement, transferManager)
    }
}