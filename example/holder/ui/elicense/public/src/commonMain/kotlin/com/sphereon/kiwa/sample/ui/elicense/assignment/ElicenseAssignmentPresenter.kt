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

package com.sphereon.kiwa.sample.ui.elicense.assignment

import androidx.compose.runtime.Immutable
import com.sphereon.kiwa.elicense.sdk.holder.license.model.IssueLicenseResult
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter

/**
 * E-license Assignment presenter interface.
 * This presenter handles the flow of assigning an e-license to the device using a PIN code.
 * The PIN code is entered by the user and then used to call the Kiwa wallet service.
 */
interface ElicenseAssignmentPresenter : MoleculePresenter<ElicenseAssignmentPresenter.Input, ElicenseAssignmentPresenter.Model> {

    @Immutable
    data class Input(
        val onAssignmentComplete: (IssueLicenseResult) -> Unit = {},
        val onCancel: () -> Unit = {}
    )

    sealed interface Event {
        data object OnCancel : Event
        data object OnRetry : Event
        data class OnPinComplete(val pinCode: String) : Event
    }

    sealed interface Model : BaseModel {
        val onEvent: (Event) -> Unit

        @Immutable
        data class EnteringPin(
            val onPinComplete: (String) -> Unit,
            override val onEvent: (Event) -> Unit,
        ) : Model

        @Immutable
        data class AssigningLicense(
            val pinCode: String,
            override val onEvent: (Event) -> Unit,
        ) : Model

        @Immutable
        data class AssignmentSuccess(
            val result: IssueLicenseResult,
            override val onEvent: (Event) -> Unit,
        ) : Model

        @Immutable
        data class AssignmentError(
            val error: String,
            override val onEvent: (Event) -> Unit,
        ) : Model
    }
}
