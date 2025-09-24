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

package com.sphereon.mdoc.testapp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import com.sphereon.core.api.ISessionLogService
import com.sphereon.di.session.ISessionContext
import com.sphereon.di.session.SureSessionScope
import com.sphereon.mdoc.engagement.IEngagementInstance
import com.sphereon.mdoc.engagement.IMdocEngagementManager
import com.sphereon.mdoc.transfer.ITransferManager

@Inject
@ContributesBinding(SureSessionScope::class)
class NfcEngagementNavigationService(
    log: ISessionLogService,
    val context: ISessionContext,
//    private val mdocEngagementQrPresenter: IMdocEngagementQrPresenter,
    private val engagementManager: IMdocEngagementManager,
) : INfcEngagementNavigationService {

    private val log = log.logManager.withTagSync("NfcEngagementNavigationService")
    private var monitoringJob: Job? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        this.log.info("NfcEngagementNavigationService initialized")
        startMonitoring()
    }

    override fun startMonitoring() {
        log.info("################################")
        log.info("Starting engagement monitoring")
        log.info(context.context.principal.toString())
        log.info("################################")
        monitoringJob?.cancel()

        /* monitoringJob = serviceScope.launch {
             engagementManager.engagementEvents.collect { event ->
                 if (event is EngagementEvent.Connected) {
                     log.info("Connected event received - attempting navigation")
                     handleConnectedEvent()
                 }
             }
         }*/
    }

    override fun stopMonitoring() {
        log.info("Stopping engagement monitoring")
        monitoringJob?.cancel()
        monitoringJob = null
    }

    private suspend fun handleConnectedEvent() {
        val engagement = engagementManager.engagement.value
        if (engagement == null) {
            log.warn("Connected event received but no current engagement available")
            return
        }

        try {
            val transferManager = engagement.start()
            handleNfcEngagementComplete(engagement, transferManager)
        } catch (e: Exception) {
            log.error("Failed to start transfer manager: ${e.message}")
        }
    }

    override fun handleNfcEngagementComplete(
        engagement: IEngagementInstance,
        transferManager: ITransferManager
    ) {
        log.info("Engagement complete - would navigate to engagement screen")
        // TODO: Implement proper navigation mechanism
    }

    override fun provideTransferManager(transferManager: ITransferManager) {
        log.info("External transfer manager provided")
        val engagement = engagementManager.engagement.value
        if (engagement != null) {
            handleNfcEngagementComplete(engagement, transferManager)
        } else {
            log.warn("Transfer manager provided but no current engagement available")
        }
    }

    override fun checkAndTriggerNavigation() {
        val engagement = engagementManager.engagement.value
        if (engagement == null) {
            log.warn("checkAndTriggerNavigation called but no current engagement available")
            return
        }

        serviceScope.launch {
            try {
                val transferManager = engagement.start()
                handleNfcEngagementComplete(engagement, transferManager)
            } catch (e: Exception) {
                log.error("Failed to start transfer manager in checkAndTriggerNavigation: ${e.message}")
            }
        }
    }
}
