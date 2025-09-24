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

package com.sphereon.ui.elicense.pincode

import androidx.compose.runtime.Immutable
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter

/**
 * PIN Code input presenter interface.
 * Presents a screen where the user enters an 8-digit numerical PIN code.
 * The PIN is typically sent via email to the user.
 */
interface IPinCodePresenter : MoleculePresenter<IPinCodePresenter.Input, IPinCodePresenter.Model> {

    @Immutable
    data class Input(
        val title: String = "Enter PIN Code",
        val description: String = "Please enter the 8-digit PIN code sent to your email",
        val onPinComplete: (pinCode: String) -> Unit = {},
        val onCancel: () -> Unit = {}
    )

    sealed interface Event {
        data class OnDigitEntered(val digit: String) : Event
        data object OnBackspace : Event
        data object OnClear : Event
        data object OnCancel : Event
    }

    sealed interface Model : BaseModel {
        val title: String
        val description: String
        val pinCode: String
        val isComplete: Boolean
        val onEvent: (Event) -> Unit

        @Immutable
        data class EnteringPin(
            override val title: String,
            override val description: String,
            override val pinCode: String,
            override val isComplete: Boolean,
            override val onEvent: (Event) -> Unit,
        ) : Model
    }
}
