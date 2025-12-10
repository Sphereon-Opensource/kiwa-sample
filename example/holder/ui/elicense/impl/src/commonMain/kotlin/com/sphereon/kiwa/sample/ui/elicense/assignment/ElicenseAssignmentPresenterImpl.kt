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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sphereon.core.api.error.NotFoundException
import com.sphereon.di.session.SessionScope
import com.sphereon.kiwa.elicense.sdk.holder.license.model.IssueLicenseResult
import com.sphereon.kiwa.sample.ui.elicense.issuance.KiwaWalletService
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@Inject
@ContributesBinding(SessionScope::class, boundType = ElicenseAssignmentPresenter::class)
class ElicenseAssignmentPresenterImpl(
    private val kiwaWalletService: KiwaWalletService
) : ElicenseAssignmentPresenter {

    @Composable
    override fun present(input: ElicenseAssignmentPresenter.Input): ElicenseAssignmentPresenter.Model {
        var state by remember { mutableStateOf(AssignmentState.ENTERING_PIN) }
        var pinCode by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf("") }
        var assignmentResult by remember { mutableStateOf<IssueLicenseResult?>(null) }

        val onEvent: (ElicenseAssignmentPresenter.Event) -> Unit = { event ->
            handleEvent(event, input) { newState, newPin, newError, newResult ->
                state = newState
                pinCode = newPin
                errorMessage = newError
                assignmentResult = newResult
            }
        }

        // Expose PIN completion callback
        val onPinComplete: (String) -> Unit = { pin ->
            pinCode = pin
            state = AssignmentState.ASSIGNING
        }

        // Launch assignment when state changes to ASSIGNING
        LaunchedEffect(state) {
            if (state == AssignmentState.ASSIGNING && pinCode.isNotEmpty()) {
                performAssignment(pinCode, kiwaWalletService) { newState, newError, result ->
                    state = newState
                    errorMessage = newError
                    assignmentResult = result
                    if (newState == AssignmentState.SUCCESS) {
                        input.onAssignmentComplete(result!!)
                    }
                }
            }
        }

        return buildModel(
            ModelBuilderParams(
                state = state,
                pinCode = pinCode,
                assignmentResult = assignmentResult,
                errorMessage = errorMessage,
                onEvent = onEvent,
                onPinComplete = onPinComplete
            )
        )
    }

    private fun handleEvent(
        event: ElicenseAssignmentPresenter.Event,
        input: ElicenseAssignmentPresenter.Input,
        updateState: (AssignmentState, String, String, IssueLicenseResult?) -> Unit
    ) {
        when (event) {
            is ElicenseAssignmentPresenter.Event.OnCancel -> {
                input.onCancel()
            }

            is ElicenseAssignmentPresenter.Event.OnRetry -> {
                updateState(AssignmentState.ENTERING_PIN, "", "", null)
            }

            is ElicenseAssignmentPresenter.Event.OnPinComplete -> {
                updateState(AssignmentState.ASSIGNING, event.pinCode, "", null)
            }
        }
    }

    private suspend fun performAssignment(
        pinCode: String,
        service: KiwaWalletService,
        updateState: (AssignmentState, String, IssueLicenseResult?) -> Unit
    ) {
        try {
            val result = service.assignElicense(pinCode)
            if (result.isOk) {
                service
                updateState(AssignmentState.SUCCESS, "", result)
            } else {
                val error = result.error.message.defaultMessage ?: DEFAULT_ERROR_MESSAGE
                println("Error result not ok: $error")
                updateState(AssignmentState.ERROR, error, null)
            }
        } catch (e: NotFoundException) {
            println("Error: ${e.resource}")
            updateState(AssignmentState.ERROR, e.resource, null)
        }
    }

    private data class ModelBuilderParams(
        val state: AssignmentState,
        val pinCode: String,
        val assignmentResult: IssueLicenseResult?,
        val errorMessage: String,
        val onEvent: (ElicenseAssignmentPresenter.Event) -> Unit,
        val onPinComplete: (String) -> Unit
    )

    private fun buildModel(params: ModelBuilderParams): ElicenseAssignmentPresenter.Model {
        return when (params.state) {
            AssignmentState.ENTERING_PIN -> {
                ElicenseAssignmentPresenter.Model.EnteringPin(
                    onEvent = params.onEvent,
                    onPinComplete = params.onPinComplete
                )
            }

            AssignmentState.ASSIGNING -> {
                ElicenseAssignmentPresenter.Model.AssigningLicense(
                    pinCode = params.pinCode,
                    onEvent = params.onEvent
                )
            }

            AssignmentState.SUCCESS -> {
                ElicenseAssignmentPresenter.Model.AssignmentSuccess(
                    result = params.assignmentResult!!,
                    onEvent = params.onEvent
                )
            }

            AssignmentState.ERROR -> {
                ElicenseAssignmentPresenter.Model.AssignmentError(
                    error = params.errorMessage,
                    onEvent = params.onEvent
                )
            }
        }
    }

    private enum class AssignmentState {
        ENTERING_PIN,
        ASSIGNING,
        SUCCESS,
        ERROR
    }

    @ContributesTo(SessionScope::class)
    interface Component {
        val elicenseAssignmentPresenter: ElicenseAssignmentPresenter
    }

    private companion object {
        const val DEFAULT_ERROR_MESSAGE = "Failed to assign e-license"
    }
}
