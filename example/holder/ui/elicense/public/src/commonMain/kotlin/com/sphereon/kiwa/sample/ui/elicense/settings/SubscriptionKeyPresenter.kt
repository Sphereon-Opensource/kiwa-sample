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

package com.sphereon.kiwa.sample.ui.elicense.settings

import androidx.compose.runtime.Immutable
import com.sphereon.kiwa.sample.ui.core.appbar.IAppBarConfigModel
import com.sphereon.kiwa.sample.ui.core.appbar.TopAppBarConfig
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter

/**
 * Subscription key presenter: provides inputs and models for managing the Kiwa subscription key.
 */
interface SubscriptionKeyPresenter : MoleculePresenter<Unit, SubscriptionKeyPresenter.Model> {

    sealed interface Event {
        data class SubscriptionKeyChanged(val key: String) : Event
        object Submit : Event
        object Cancel : Event
    }

    sealed interface Model : BaseModel, IAppBarConfigModel {
        val onEvent: (event: Event) -> Unit

        override fun topAppBarConfig(): TopAppBarConfig {
            return TopAppBarConfig(
                title = "Subscription Key",
                backArrowAction = { onEvent(Event.Cancel) }
            )
        }

        @Immutable
        data class SubscriptionKeyForm(
            val subscriptionKey: String,
            val isLoading: Boolean = false,
            override val onEvent: (event: Event) -> Unit
        ) : Model
    }
}
