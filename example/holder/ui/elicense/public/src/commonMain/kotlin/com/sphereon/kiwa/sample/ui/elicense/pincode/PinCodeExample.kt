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

package com.sphereon.kiwa.sample.ui.elicense.pincode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Example demonstrating how to use the PIN Code presenter.
 *
 * Usage example:
 * ```kotlin
 * // In your screen/presenter
 * val pinCodePresenter = remember { pinCodePresenter() }
 * var enteredPin by remember { mutableStateOf<String?>(null) }
 *
 * val pinModel = pinCodePresenter.present(
 *     IPinCodePresenter.Input(
 *         title = "Verify Your Email",
 *         description = "Enter the 8-digit code sent to your email address",
 *         onPinComplete = { pin ->
 *             enteredPin = pin
 *             // Proceed with verification logic
 *             verifyPinCode(pin)
 *         },
 *         onCancel = {
 *             // Handle cancellation
 *             navigateBack()
 *         }
 *     )
 * )
 *
 * // The pin code is automatically stored in the presenter
 * // and can be accessed via pinModel.pinCode
 * ```
 */
object PinCodeExample {

    private const val PIN_CODE_LENGTH = 8

    /**
     * Example of how to integrate PIN code verification in your app flow.
     */
    @Composable
    fun ExampleUsage(
        pinCodePresenter: PinCodePresenter,
        onPinVerified: (String) -> Unit = {},
        onCancel: () -> Unit = {}
    ) {
        var verificationState by remember { mutableStateOf(VerificationState.ENTERING_PIN) }
        var enteredPin by remember { mutableStateOf("") }

        when (verificationState) {
            VerificationState.ENTERING_PIN -> {
                // Present the PIN input screen
                pinCodePresenter.present(
                    PinCodePresenter.Input(
                        title = "Email Verification",
                        description = "Please enter the $PIN_CODE_LENGTH-digit PIN code sent to your email",
                        onPinComplete = { pin ->
                            enteredPin = pin
                            verificationState = VerificationState.VERIFYING
                            // In a real app, you'd call your verification service here
                            verifyPinCode(pin) { success ->
                                if (success) {
                                    verificationState = VerificationState.VERIFIED
                                    onPinVerified(pin)
                                } else {
                                    verificationState = VerificationState.ERROR
                                }
                            }
                        },
                        onCancel = onCancel
                    )
                )

                // The PIN is stored in pinModel.pinCode and automatically
                // triggered via onPinComplete when 8 digits are entered
            }

            VerificationState.VERIFYING -> {
                // Show loading state
                // LoadingScreen()
            }

            VerificationState.VERIFIED -> {
                // PIN verification successful
                // SuccessScreen()
            }

            VerificationState.ERROR -> {
                // Show error and allow retry
                // ErrorScreen { verificationState = VerificationState.ENTERING_PIN }
            }
        }
    }

    private enum class VerificationState {
        ENTERING_PIN,
        VERIFYING,
        VERIFIED,
        ERROR
    }

    private fun verifyPinCode(pin: String, onResult: (Boolean) -> Unit) {
        // Simulate API call
        // In a real implementation, this would call your backend service
        onResult(pin.length == PIN_CODE_LENGTH && pin.all { it.isDigit() })
    }
}
