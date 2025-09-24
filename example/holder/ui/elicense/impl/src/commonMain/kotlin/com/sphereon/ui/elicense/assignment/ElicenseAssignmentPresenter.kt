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

package com.sphereon.ui.elicense.assignment

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import com.sphereon.core.api.error.NotFoundException
import com.sphereon.di.session.SureSessionScope
import com.sphereon.kiwa.elicense.sdk.holder.license.model.IssueLicenseResult
import com.sphereon.ui.elicense.issuance.IKiwaWalletService

@Inject
@ContributesBinding(SureSessionScope::class, boundType = IElicenseAssignmentPresenter::class)
class ElicenseAssignmentPresenter(
    private val kiwaWalletService: IKiwaWalletService
) : IElicenseAssignmentPresenter {

    @Composable
    override fun present(input: IElicenseAssignmentPresenter.Input): IElicenseAssignmentPresenter.Model {
        var state by remember { mutableStateOf(AssignmentState.ENTERING_PIN) }
        var pinCode by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf("") }
        var assignmentResult by remember { mutableStateOf<IssueLicenseResult?>(null) }

        val onEvent: (IElicenseAssignmentPresenter.Event) -> Unit = { event ->
            when (event) {
                is IElicenseAssignmentPresenter.Event.OnCancel -> {
                    input.onCancel()
                }

                is IElicenseAssignmentPresenter.Event.OnRetry -> {
                    state = AssignmentState.ENTERING_PIN
                    pinCode = ""
                    errorMessage = ""
                    assignmentResult = null
                }

                is IElicenseAssignmentPresenter.Event.OnPinComplete -> {
                    pinCode = event.pinCode
                    state = AssignmentState.ASSIGNING
                }
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
                try {
                    val result = kiwaWalletService.assignElicense(pinCode)
                    if (result.isOk) {
                        assignmentResult = result
                        state = AssignmentState.SUCCESS
                        input.onAssignmentComplete(result)
                    } else {
                        println("Error result not ok: ${result.error.message.defaultMessage}")
                        errorMessage = result.error.message.defaultMessage ?: "Failed to assign e-license"
                        state = AssignmentState.ERROR
                    }
                } catch (e: NotFoundException) {
                    println("Error: ${e.resource}")
                    errorMessage = e.resource
                    state = AssignmentState.ERROR
                } catch (e: Exception) {
                    println("Error: ${e.message}")
                    errorMessage = e.message ?: "An unexpected error occurred"
                    state = AssignmentState.ERROR
                }
            }
        }

        return when (state) {
            AssignmentState.ENTERING_PIN -> {
                IElicenseAssignmentPresenter.Model.EnteringPin(
                    onEvent = onEvent,
                    onPinComplete = onPinComplete
                )
            }

            AssignmentState.ASSIGNING -> {
                IElicenseAssignmentPresenter.Model.AssigningLicense(
                    pinCode = pinCode,
                    onEvent = onEvent
                )
            }

            AssignmentState.SUCCESS -> {
                IElicenseAssignmentPresenter.Model.AssignmentSuccess(
                    result = assignmentResult!!,
                    onEvent = onEvent
                )
            }

            AssignmentState.ERROR -> {
                IElicenseAssignmentPresenter.Model.AssignmentError(
                    error = errorMessage,
                    onEvent = onEvent
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

    @ContributesTo(SureSessionScope::class)
    interface Component {
        val elicenseAssignmentPresenter: IElicenseAssignmentPresenter
    }
}
