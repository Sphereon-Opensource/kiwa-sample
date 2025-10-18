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

/**
 * App-level bridge service for routing NFC engagement events between contexts.
 *
 * This service operates at the app scope (shared between all contexts) and serves as a bridge
 * between the anonymous NFC service context and the authenticated user's UI context.
 *
 * The problem it solves:
 * - NFC service runs in anonymous context (instantiated by Android)
 * - UI runs in authenticated context (different DI scope)
 * - Need to route NFC events to authenticated user's navigation service
 * - Ensure NFC only works when user is authenticated
 *
 * This bridge uses the shared app scope to coordinate between the different session scopes.
 */
interface NfcAuthenticationBridge {

    /**
     * Handles NFC engagement completion from anonymous context and routes to authenticated context.
     *
     * @param engagement The engagement instance from the NFC service
     * @param transferManager The transfer manager from the NFC service
     * @return true if successfully routed to authenticated context, false if user not authenticated
     */
    fun routeNfcEngagementToAuthenticatedContext(
        engagement: EngagementInstance,
        transferManager: TransferManager
    ): Boolean

    /**
     * Checks if NFC operations should be allowed based on authentication status.
     *
     * @return true if user is authenticated and NFC operations are allowed
     */
    fun isNfcAllowed(): Boolean

    /**
     * Gets the authenticated navigation service if available.
     *
     * @return the navigation service from authenticated context, or null if not authenticated
     */
    fun getAuthenticatedNavigationService(): NfcEngagementNavigationService?
}