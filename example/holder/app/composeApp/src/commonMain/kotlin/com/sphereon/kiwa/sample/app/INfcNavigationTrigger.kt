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
 */

package com.sphereon.kiwa.sample.app

import com.sphereon.kiwa.sample.ui.core.backstack.PresenterBackstackScope
import com.sphereon.mdoc.engagement.EngagementInstance
import com.sphereon.mdoc.transfer.TransferManager
import kotlinx.coroutines.flow.StateFlow

/**
 * A simple navigation trigger that can be called from background NFC services
 * to push NFC engagement presenters to the UI backstack, similar to how the
 * share button works.
 */
interface INfcNavigationTrigger {
    /**
     * Sets the current backstack scope that can be used for navigation.
     * This is called from the UI layer when the backstack is available.
     */
    fun setBackstackScope(backstackScope: PresenterBackstackScope?)

    /**
     * Triggers navigation to NFC engagement with the given parameters.
     * Returns true if navigation was triggered, false if no backstack is available.
     */
    fun navigateToNfcEngagement(): Boolean

    /**
     * Observable flow that indicates when NFC navigation was triggered.
     * UI components can observe this to know when navigation occurred.
     */
    val navigationTriggeredFlow: StateFlow<Long>
}