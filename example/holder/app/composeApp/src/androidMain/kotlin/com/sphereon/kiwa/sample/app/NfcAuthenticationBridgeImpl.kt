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

import com.sphereon.mdoc.engagement.EngagementInstance
import com.sphereon.mdoc.transfer.TransferManager
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Implementation of the NFC authentication bridge service.
 *
 * This service operates at the app scope and serves as a bridge between the anonymous
 * NFC service context and the authenticated user's UI context. It ensures that NFC
 * operations only occur when a user is properly authenticated and routes engagement
 * events to the correct session scope.
 *
 * The service uses the shared app-level authentication service to check authentication
 * status and dynamically access the authenticated session's navigation service.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class NfcAuthenticationBridgeImpl : NfcAuthenticationBridge {

    /**
     * Routes NFC engagement completion from anonymous context to authenticated context.
     * This ensures that NFC engagements are only processed when the user is authenticated
     * and navigation happens in the correct session context.
     */
    override fun routeNfcEngagementToAuthenticatedContext(
        engagement: EngagementInstance,
        transferManager: TransferManager
    ): Boolean {
        println("NfcAuthenticationBridge: Routing NFC engagement to authenticated context")
        println("NfcAuthenticationBridge: Engagement details - ID: ${engagement.id}, Transfer Manager: $transferManager")

        try {
            val authService = MainActivity.authSessionService
            val isAuthenticated = authService.isAuthenticated()

            if (!isAuthenticated) {
                println("NfcAuthenticationBridge: User not authenticated, rejecting NFC engagement")
                return false
            }

            val sessionComponent = authService.sessionComponentFlow.value

            if (sessionComponent == null) {
                println("NfcAuthenticationBridge: No session component available")
                return false
            }

            println("NfcAuthenticationBridge: Retrieved session component: $sessionComponent")
            println("NfcAuthenticationBridge: Session component class: ${sessionComponent::class}")
            println("NfcAuthenticationBridge: Session component session context: ${sessionComponent.sessionContext}")
            println("NfcAuthenticationBridge: Session context anonymous: ${sessionComponent.sessionContext.isAnonymous()}")

            val authenticatedNavigationService = (sessionComponent as? NfcEngagementNavigationService.Component)?.nfcEngagementNavigationService

            if (authenticatedNavigationService == null) {
                println("NfcAuthenticationBridge: No authenticated navigation service available, rejecting NFC engagement")
                println("NfcAuthenticationBridge: Session component does not implement INfcEngagementNavigationService.Component")
                return false
            }

            println("NfcAuthenticationBridge: Successfully retrieved authenticated navigation service")
            println("NfcAuthenticationBridge: Authenticated navigation service instance: $authenticatedNavigationService")
            println("NfcAuthenticationBridge: Authenticated navigation service class: ${authenticatedNavigationService::class}")
            println("NfcAuthenticationBridge: Authenticated navigation service hashCode: ${authenticatedNavigationService.hashCode()}")
            println("NfcAuthenticationBridge: Found authenticated navigation service, routing engagement")

            // Use the new direct navigation method instead of the complex pending navigation
            val navigationSuccess = authenticatedNavigationService.navigateToNfcEngagement(engagement, transferManager)

            println("NfcAuthenticationBridge: Navigation result: $navigationSuccess")
            return navigationSuccess

        } catch (e: Exception) {
            println("NfcAuthenticationBridge: Error routing engagement: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * Checks if NFC operations should be allowed based on authentication status.
     *
     * @return true if user is authenticated and NFC operations are allowed
     */
    override fun isNfcAllowed(): Boolean {
        return try {
            val authService = MainActivity.authSessionService
            val isAuthenticated = authService.isAuthenticated()
            println("NfcAuthenticationBridge: Authentication check - isAuthenticated: $isAuthenticated")

            // Additional debugging to understand session state
            val sessionComponent = authService.sessionComponentFlow.value
            val contextComponent = authService.contextComponentFlow.value
            val isAnonymous = sessionComponent?.sessionContext?.isAnonymous()

            println("NfcAuthenticationBridge: Session state - sessionComponent: ${sessionComponent != null}, contextComponent: ${contextComponent != null}, isAnonymous: $isAnonymous")
            println("NfcAuthenticationBridge: NFC allowed = $isAuthenticated")

            isAuthenticated
        } catch (e: Exception) {
            println("NfcAuthenticationBridge: Error checking authentication status: ${e.message}")
            false
        }
    }

    /**
     * Gets the authenticated navigation service if available.
     *
     * This method accesses the authenticated session component and retrieves the
     * navigation service from the authenticated context.
     *
     * @return the navigation service from authenticated context, or null if not authenticated
     */
    override fun getAuthenticatedNavigationService(): NfcEngagementNavigationService? {
        return try {
            val authService = MainActivity.authSessionService

            // Check if user is authenticated
            if (!authService.isAuthenticated()) {
                println("NfcAuthenticationBridge: User not authenticated, no navigation service available")
                return null
            }

            // Get the authenticated session component
            val sessionComponent = authService.sessionComponentFlow.value
            if (sessionComponent == null) {
                println("NfcAuthenticationBridge: No authenticated session component available")
                return null
            }

            // Extract the navigation service from the authenticated session
            val navigationService = (sessionComponent as? NfcEngagementNavigationService.Component)?.nfcEngagementNavigationService
            if (navigationService != null) {
                println("NfcAuthenticationBridge: Successfully retrieved authenticated navigation service")
            } else {
                println("NfcAuthenticationBridge: Authenticated session component does not provide navigation service")
            }

            navigationService
        } catch (e: Exception) {
            println("NfcAuthenticationBridge: Error accessing authenticated navigation service: ${e.message}")
            null
        }
    }
}