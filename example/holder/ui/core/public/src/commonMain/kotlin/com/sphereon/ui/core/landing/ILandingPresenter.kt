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

package com.sphereon.ui.core.landing

import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter

interface ILandingPresenter : MoleculePresenter<Any, ILandingPresenter.Model> {

    /** The state of the landing screen. */
    data class Model(
        /** Callback to send events back to the presenter. */
        val onEvent: (Event) -> Unit,
        val authenticated: Boolean
    ) : BaseModel

    /** All events that [ILandingPresenter] can process. */
    sealed interface Event {
        /** Add a new presenter to the backstack. */
        data object AddPresenterToBackstack : Event

        /** Show authentication presenter. */
        data object AuthPresenter : Event

        /** Navigate to main content. */
        data object NavigateToMain : Event
    }
}
