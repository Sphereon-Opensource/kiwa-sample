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

import com.sphereon.di.session.SessionScope
import com.sphereon.mdoc.engagement.EngagementInstance
import com.sphereon.mdoc.transfer.TransferManager
import kotlinx.coroutines.flow.StateFlow
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

interface NfcEngagementNavigationService {
    fun startMonitoring()
    fun stopMonitoring()
//    fun handleNfcEngagementComplete()

    /**
     * Reactive flow of pending navigation data for NFC engagements.
     * This allows UI components to observe and react to NFC engagement state changes.
     */
    val pendingNavigationFlow: StateFlow<PendingNavigation?>


    /**
     * Clears the pending navigation data.
     * This should be called when the NFC engagement is complete or cancelled.
     */
    fun clearPendingNavigation()

    /**
     * Data class to hold pending navigation information for NFC engagements.
     */
    data class PendingNavigation(
        val engagement: EngagementInstance,
        val transferManager: TransferManager
    )

    /**
     * Gets the pending navigation data and clears it.
     */
    fun consumePendingNavigation(): PendingNavigation?

    /**
     * Checks if there is a pending navigation.
     */
    fun hasPendingNavigation(): Boolean

    /**
     * Navigates to the NFC engagement presenter with the given engagement and transfer manager.
     * This can be called from background NFC services to trigger UI navigation.
     * Returns true if navigation was successful, false otherwise.
     */
    fun navigateToNfcEngagement(): Boolean

    @ContributesTo(SessionScope::class)
    interface Component {
        val nfcEngagementNavigationService: NfcEngagementNavigationService
    }
}
