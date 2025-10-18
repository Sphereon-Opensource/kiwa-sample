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

import com.sphereon.data.link.nfc.NfcApduDispatcher
import com.sphereon.data.link.nfc.SessionNfcApduDispatcher
import com.sphereon.di.session.SessionComponent
import com.sphereon.mdoc.engagement.nfc.AbstractMdocNfcService


/**
 * Android NFC service implementation for handling mobile document (mDoc) communication.
 *
 * This service extends [AbstractMdocNfcService] to provide Android-specific NFC functionality
 * for mobile document presentation and verification. It handles NFC card emulation mode,
 * allowing the device to act as a contactless card for credential presentation.
 *
 * IMPORTANT: This service now uses an authentication bridge to ensure NFC operations
 * only occur when a user is properly authenticated. The service operates in anonymous
 * context but routes events through the bridge to the authenticated user's session context.
 *
 * Key responsibilities:
 * - Verify user authentication before processing NFC requests
 * - Handle incoming NFC APDU commands from external readers
 * - Route NFC events to authenticated session via the bridge
 * - Process mobile document presentation requests over NFC
 * - Manage the NFC communication session lifecycle
 *
 * Note: This class is instantiated by the Android NFC framework via class reference,
 * so it cannot use dependency injection and must use the bridge pattern.
 */
class MdocNfcService : AbstractMdocNfcService() {

    /**
     * NFC authentication bridge for routing events to authenticated context.
     * This bridge checks authentication and routes events to the proper session.
     */
    private val nfcBridge by lazy { MainActivity.nfcAuthenticationBridge }

    /**
     * The active session component providing session-scoped dependencies.
     *
     * CRITICAL: This now uses the anonymous session as a fallback for basic NFC operations,
     * but all engagement events are routed through the bridge to the authenticated context.
     */
    override val sessionComponent: SessionComponent by lazy {
        try {
            // Always use anonymous session for basic NFC operations
            // Engagement events will be routed to authenticated context via bridge
            println("MdocNfcService: Using anonymous session component for basic NFC operations")
            MainActivity.sessionComponentFlow.value
        } catch (e: Exception) {
            throw IllegalStateException("Failed to access session component for NFC service: ${e.message}", e)
        }
    }

    /**
     * The NFC APDU dispatcher responsible for handling NFC communication protocols.
     *
     * This dispatcher uses the anonymous session context for basic APDU processing,
     * but engagement events are routed to authenticated context via the bridge.
     */
    override val apduService: NfcApduDispatcher by lazy {
        (sessionComponent as SessionNfcApduDispatcher.Component).sessionNfcApduDispatcher
    }

    /**
     * Navigation service from the anonymous session context.
     * This is wrapped with the bridge to route engagement events to authenticated context.
     */
    private val baseNavigationService: NfcEngagementNavigationService by lazy {
        val service = (sessionComponent as NfcEngagementNavigationService.Component).nfcEngagementNavigationService
        println("MdocNfcService: Created base navigation service: $service")
        println("MdocNfcService: Base navigation service class: ${service::class}")
        service
    }

    /**
     * Bridged navigation service that routes engagement events to authenticated context.
     */
    val navigationService: NfcEngagementNavigationService by lazy {
        val bridgedService = BridgedNfcEngagementNavigationService(baseNavigationService, nfcBridge)
        println("MdocNfcService: Created bridged navigation service: $bridgedService")
        println("MdocNfcService: Bridged navigation service class: ${bridgedService::class}")
        bridgedService
    }

    /**
     * Checks authentication status and initializes NFC monitoring.
     */
    init {
        println("MdocNfcService initialized with authentication bridge")

        // Start monitoring with bridged navigation service
        // The bridge will handle routing to authenticated context when needed
        println("MdocNfcService: Starting NFC monitoring with bridged navigation service")
        navigationService.startMonitoring()
    }

    /**
     * Override APDU processing to include authentication check via bridge.
     * This ensures every NFC interaction is validated against authentication status.
     */
    override fun processCommandApdu(commandApdu: ByteArray?, extras: android.os.Bundle?): ByteArray? {
        println("MdocNfcService: Processing APDU command via bridge")

        // Check authentication via bridge before processing any APDU
        if (!nfcBridge.isNfcAllowed()) {
            println("MdocNfcService: Rejecting APDU - user not authenticated")
            // Return error response for unauthenticated access
            return byteArrayOf(0x6F.toByte(), 0x00.toByte()) // General error
        }

        println("MdocNfcService: User authenticated, proceeding with APDU processing")

        // Proceed with normal APDU processing using anonymous session
        // Engagement events will be intercepted and routed via bridge
        return try {
            super.processCommandApdu(commandApdu, extras)
        } catch (e: Exception) {
            println("MdocNfcService: Error processing APDU: ${e.message}")
            // Return error response
            byteArrayOf(0x6F.toByte(), 0x00.toByte())
        }
    }

    /**
     * Called when the NFC service is being destroyed.
     *
     * This method ensures proper cleanup of resources when the service lifecycle ends.
     */
    override fun onDestroy() {
        println("MdocNfcService onDestroy - stopping navigation monitoring")
        try {
            navigationService.stopMonitoring()
        } catch (e: Exception) {
            println("MdocNfcService: Error during destruction: ${e.message}")
        }
        super.onDestroy()
    }
}
