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

import android.content.Context
import com.sphereon.core.api.log.LogManager
import com.sphereon.mdoc.engagement.MdocEngagementEvent
import com.sphereon.mdoc.engagement.MdocEngagementManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Observes BLE engagement lifecycle and manages the BleEngagementService foreground service.
 *
 * This observer monitors the engagement manager for BLE-related events and automatically
 * starts/stops the foreground service to ensure BLE operations continue even when the
 * screen is locked or the app is in the background.
 *
 * The foreground service is required because:
 * - Android suspends BLE advertising when the screen locks (especially on Samsung devices)
 * - Android 12+ has strict background execution limits
 * - Without a foreground service, BLE connections will be interrupted when the device sleeps
 *
 * Key responsibilities:
 * - Monitor engagement events from the engagement manager
 * - Start BleEngagementService when BLE advertising begins
 * - Stop BleEngagementService when BLE engagement completes
 * - Ensure service lifecycle matches engagement lifecycle
 */
interface BleEngagementLifecycleObserver {
    /**
     * Initializes the observer to start monitoring engagement events.
     *
     * @param context Android context for starting/stopping services
     * @param engagementManager Manager to observe for BLE engagement events
     */
    fun initialize(context: Context, engagementManager: MdocEngagementManager)

    /**
     * Stops observing and cleans up resources.
     */
    fun shutdown()
}

/**
 * Implementation of BleEngagementLifecycleObserver.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class BleEngagementLifecycleObserverImpl @Inject constructor(
    logManager: LogManager
) : BleEngagementLifecycleObserver {

    private val log = logManager.withTag("BleEngagementLifecycle")
    private var observerScope: CoroutineScope? = null
    private var isServiceRunning = false

    override fun initialize(context: Context, engagementManager: MdocEngagementManager) {
        log.info("Initializing BLE engagement lifecycle observer")

        // Cancel any existing scope
        observerScope?.cancel()

        // Create new scope for observing
        observerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        // Observe active engagement - start service when engagement becomes active
        engagementManager.activeEngagement
            .onEach { engagement ->
                handleEngagementChange(context, engagement)
            }
            .launchIn(observerScope!!)

        log.info("BLE engagement lifecycle observer initialized")
    }

    override fun shutdown() {
        log.info("Shutting down BLE engagement lifecycle observer")
        observerScope?.cancel()
        observerScope = null
    }

    private fun handleEngagementChange(context: Context, engagement: Any?) {
        if (engagement == null) {
            // No active engagement - stop service if running
            if (isServiceRunning) {
                log.info("No active engagement, stopping foreground service")
                BleEngagementService.stop(context)
                isServiceRunning = false
            }
        } else {
            // Active engagement detected - start service to keep BLE alive during screen lock
            // The service is needed for any engagement that might use BLE (QR with BLE retrieval)
            if (!isServiceRunning) {
                log.info("Engagement active, starting foreground service to maintain BLE")
                BleEngagementService.start(context)
                isServiceRunning = true
            }
        }
    }
}
