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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import com.sphereon.di.session.SureSessionScope

@Inject
@ContributesBinding(SureSessionScope::class, boundType = IPinCodePresenter::class)
class PinCodePresenter : IPinCodePresenter {

    @Composable
    override fun present(input: IPinCodePresenter.Input): IPinCodePresenter.Model {
        var pinCode by remember { mutableStateOf("") }

        val onEvent: (IPinCodePresenter.Event) -> Unit = { event ->
            when (event) {
                is IPinCodePresenter.Event.OnDigitEntered -> {
                    if (pinCode.length < 8 && event.digit.length == 1 && event.digit.all { it.isDigit() }) {
                        pinCode += event.digit

                        // Check if PIN is complete and trigger callback
                        if (pinCode.length == 8) {
                            input.onPinComplete(pinCode)
                        }
                    }
                }

                is IPinCodePresenter.Event.OnBackspace -> {
                    if (pinCode.isNotEmpty()) {
                        pinCode = pinCode.dropLast(1)
                    }
                }

                is IPinCodePresenter.Event.OnClear -> {
                    pinCode = ""
                }

                is IPinCodePresenter.Event.OnCancel -> {
                    input.onCancel()
                }
            }
        }

        return IPinCodePresenter.Model.EnteringPin(
            title = input.title,
            description = input.description,
            pinCode = pinCode,
            isComplete = pinCode.length == 8,
            onEvent = onEvent
        )
    }

    @ContributesTo(SureSessionScope::class)
    interface Component {
        val pinCodePresenter: IPinCodePresenter
    }
}
