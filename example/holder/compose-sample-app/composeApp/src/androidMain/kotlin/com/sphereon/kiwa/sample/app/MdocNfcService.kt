/*
 * © 2026 Sphereon International B.V.
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
import com.sphereon.di.session.SessionInstance
import com.sphereon.mdoc.engagement.nfc.AbstractMdocNfcService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


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
 * so it cannot use dependency injection. It accesses services through the Application instance.
 */
class MdocNfcService : AbstractMdocNfcService() {


    companion object {
        lateinit var app: KiwaSampleApplication

        /**
         * Application services providing access to authentication bridge and session components.
         */
        private val appServices
            get() = app.appComponent.appServices


        /**
         * Coroutine scope for observing activeUserContextInstance changes.
         */
        private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)


        /**
         * The active session instance providing session-scoped dependencies.
         *
         */
        private val sessionInstance: SessionInstance
            get() = try {
                println("===============>> Auth session service requested")
                val activeUserContext = appServices.userContextManager.activeInstance.value
                println("Active user context: ${activeUserContext.context}")
                println("Is anonymous: ${activeUserContext.userContextManager.isAnonymous()}")
                val sessionInstance = activeUserContext.sessionContextManager.getActive()
                println("Auth session service: ${sessionInstance.sessionId}: ${sessionInstance.sessionContext}")
                sessionInstance
            } catch (e: Exception) {
                println("ERROR: Failed to access session instance for NFC service: ${e.message}")
                e.printStackTrace()
                throw IllegalStateException("Failed to access session instance for NFC service: ${e.message}", e)
            }

    }



    /**
     * The session component required by AbstractMdocNfcService.
     * Lazily initialized and cached to ensure the same instance is used throughout.
     */
    override val sessionComponent: SessionComponent by lazy {
        println("MdocNfcService: sessionComponent accessed - getting from sessionInstance")
        val component = sessionInstance.component
        println("MdocNfcService: sessionComponent obtained: $component")
        println("MdocNfcService: sessionComponent hashCode: ${component.hashCode()}")
        component
    }


    /**
     * The NFC APDU dispatcher responsible for handling NFC communication protocols.
     * Lazily initialized and cached to ensure the same dispatcher instance is used
     * for both tryDispatch() and receive() calls in the async processing loop.
     *
     * This dispatcher uses the authenticated user's session context for APDU processing.
     */
    override val apduService: NfcApduDispatcher by lazy {
        (sessionComponent as SessionNfcApduDispatcher.Component).sessionNfcApduDispatcher
    }


    /**
     * Navigation service from the anonymous session context.
     * This is wrapped with the bridge to route engagement events to authenticated context.
     */
    private val navigationService: NfcEngagementNavigationService
        get() {
            val service = (sessionComponent as NfcEngagementNavigationService.Component).nfcEngagementNavigationService
            println("MdocNfcService: Created navigation service: $service")
            println("MdocNfcService: navigation service class: ${service::class}")
            return service
        }

    /*   */
    /**
     * Bridged navigation service that routes engagement events to authenticated context.
     *//*
    val navigationService: NfcEngagementNavigationService by lazy {
        val bridgedService = BridgedNfcEngagementNavigationService(nfcEngagementNavigationService, nfcBridge)
        println("MdocNfcService: Created bridged navigation service: $bridgedService")
        println("MdocNfcService: Bridged navigation service class: ${bridgedService::class}")

        bridgedService
    }*/

    /**
     * Background NFC engagement is managed by the engagementManager singleton.
     * No need to track it locally since the manager enforces one engagement per type.
     * Note: engagementManager is provided by AbstractMdocNfcService
     */

    /**
     * Override APDU processing to include authentication check and delegate to base class.
     * This ensures every NFC interaction is validated against authentication status.
     */
    override fun processCommandApdu(commandApdu: ByteArray?, extras: android.os.Bundle?): ByteArray? {
        // Check authentication before processing any APDU
        if (appServices.userContextManager.isAnonymous()) {
            println("MdocNfcService: Rejecting APDU - user not authenticated")
            // Return error response for unauthenticated access
            return byteArrayOf(0x6F.toByte(), 0x00.toByte()) // General error
        }

        // Proceed with normal APDU processing
        return try {
            super.processCommandApdu(commandApdu, extras)
        } catch (e: Exception) {
            println("MdocNfcService: Error processing APDU: ${e.message}")
            // Return error response
            byteArrayOf(0x6F.toByte(), 0x00.toByte())
        }
    }

    /**
     * Called when the service is first created by the Android framework.
     * This is where we can safely access the application instance.
     */
    override fun onCreate() {
        app = application as KiwaSampleApplication
        super.onCreate()
        println("MdocNfcService: Service onCreate() - application now available")
        println("MdocNfcService: Engagement Manager Instance: ${engagementManager.hashCode()}")

        // Observe activeUserContextInstance changes and update monitoring accordingly
        appServices.userContextManager.activeInstance
            .onEach { userContext ->
                println("MdocNfcService: activeUserContextInstance changed to: $userContext")
                if (!userContext.isCurrentlyActive() || userContext.userContextManager.isAnonymous()) {
                    log.warn("MdocNfcService: NFC not allowed (user not logged in) - stopping monitoring and closing NFC engagement")
                    navigationService.stopMonitoring()
                    closeNfcEngagement()
                } else {
                    log.info("==================================================")
                    log.info("MdocNfcService: NFC allowed - starting monitoring")
                    log.info("MdocNfcService: Navigation Service Instance: ${navigationService.hashCode()}")
                    log.info("MdocNfcService: Engagement Manager from NFC Service: ${engagementManager.hashCode()}")
                    navigationService.startMonitoring()
                }
            }
            .launchIn(serviceScope)
    }

    /**
     * Closes any active NFC engagement.
     * Called when user logs out to ensure clean state.
     */
    private fun closeNfcEngagement() {
        serviceScope.launch {
            runCatching {
                log.info("MdocNfcService: Closing NFC engagement via engagement manager")
                val result = engagementManager.closeNfcEngagement()
                if (result.isOk) {
                    log.info("MdocNfcService: NFC engagement closed successfully")
                } else {
                    log.debug("MdocNfcService: No NFC engagement to close: ${result.error}")
                }
            }.onFailure { error ->
                log.error("MdocNfcService: Error closing NFC engagement: ${error.message}")
            }
        }
    }



    /**
     * Called when the NFC service is being destroyed.
     *
     * This method ensures proper cleanup of resources when the service lifecycle ends.
     */
    override fun onDestroy() {
        println("MdocNfcService onDestroy - stopping navigation monitoring and closing NFC engagement")
        try {
            navigationService.stopMonitoring()
            closeNfcEngagement()
            serviceScope.cancel()
        } catch (e: Exception) {
            println("MdocNfcService: Error during destruction: ${e.message}")
        }
        super.onDestroy()
    }
}
