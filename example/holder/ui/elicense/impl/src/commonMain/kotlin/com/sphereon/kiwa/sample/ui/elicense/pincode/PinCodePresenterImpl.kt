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

package com.sphereon.kiwa.sample.ui.elicense.pincode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sphereon.di.session.SessionScope
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@Inject
@ContributesBinding(SessionScope::class, boundType = PinCodePresenter::class)
class PinCodePresenterImpl : PinCodePresenter {

    @Composable
    override fun present(input: PinCodePresenter.Input): PinCodePresenter.Model {
        var pinCode by remember { mutableStateOf("") }

        val onEvent: (PinCodePresenter.Event) -> Unit = { event ->
            when (event) {
                is PinCodePresenter.Event.OnDigitEntered -> {
                    if (pinCode.length < PIN_LENGTH && event.digit.length == 1 && event.digit.all { it.isDigit() }) {
                        pinCode += event.digit

                        // Check if PIN is complete and trigger callback
                        if (pinCode.length == PIN_LENGTH) {
                            input.onPinComplete(pinCode)
                        }
                    }
                }

                is PinCodePresenter.Event.OnBackspace -> {
                    if (pinCode.isNotEmpty()) {
                        pinCode = pinCode.dropLast(1)
                    }
                }

                is PinCodePresenter.Event.OnClear -> {
                    pinCode = ""
                }

                is PinCodePresenter.Event.OnCancel -> {
                    input.onCancel()
                }
            }
        }

        return PinCodePresenter.Model.EnteringPin(
            title = input.title,
            description = input.description,
            pinCode = pinCode,
            isComplete = pinCode.length == PIN_LENGTH,
            onEvent = onEvent
        )
    }

    private companion object {
        const val PIN_LENGTH = 8
    }

    @ContributesTo(SessionScope::class)
    interface Component {
        val pinCodePresenter: PinCodePresenter
    }
}
