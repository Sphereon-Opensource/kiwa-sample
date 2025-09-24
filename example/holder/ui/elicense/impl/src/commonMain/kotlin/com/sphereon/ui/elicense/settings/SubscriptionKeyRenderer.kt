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

package com.sphereon.ui.elicense.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.renderer.ComposeRenderer
import com.sphereon.ui.core.theme.AppColors

// @Inject
@ContributesRenderer(modelType = ISubscriptionKeyPresenter.Model::class)
class SubscriptionKeyRenderer : ComposeRenderer<ISubscriptionKeyPresenter.Model>() {

    @Composable
    override fun Compose(model: ISubscriptionKeyPresenter.Model) {
        when (model) {
            is ISubscriptionKeyPresenter.Model.SubscriptionKeyForm -> SubscriptionKeyForm(model)
        }
    }

    @Composable
    private fun SubscriptionKeyForm(model: ISubscriptionKeyPresenter.Model.SubscriptionKeyForm) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Screen.background)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Kiwa Subscription Key",
                    color = AppColors.Screen.foreground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter your Kiwa subscription key to configure API access.",
                    color = AppColors.Screen.foreground.copy(alpha = 0.7f),
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = model.subscriptionKey,
                    onValueChange = { model.onEvent(ISubscriptionKeyPresenter.Event.SubscriptionKeyChanged(it)) },
                    label = { Text("Subscription Key") },
                    placeholder = { Text("Enter subscription key") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = AppColors.Accent.primary,
                        unfocusedTextColor = AppColors.Screen.foreground,
                        focusedLabelColor = AppColors.Accent.primary,
                        unfocusedLabelColor = AppColors.Screen.foreground.copy(alpha = 0.7f),
                        focusedPlaceholderColor = AppColors.Screen.foreground.copy(alpha = 0.5f),
                        unfocusedPlaceholderColor = AppColors.Screen.foreground.copy(alpha = 0.5f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = AppColors.Accent.primary,
                        unfocusedIndicatorColor = AppColors.Screen.foreground.copy(alpha = 0.3f)
                    ),
                    enabled = !model.isLoading,
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.None,
                        autoCorrect = false
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { model.onEvent(ISubscriptionKeyPresenter.Event.Cancel) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = AppColors.Screen.foreground
                        ),
                        modifier = Modifier.weight(1f),
                        enabled = !model.isLoading
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { model.onEvent(ISubscriptionKeyPresenter.Event.Submit) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Accent.primary,
                            contentColor = AppColors.Screen.foreground
                        ),
                        modifier = Modifier.weight(1f),
                        enabled = !model.isLoading && model.subscriptionKey.isNotBlank()
                    ) {
                        if (model.isLoading) {
                            CircularProgressIndicator(
                                color = AppColors.Screen.foreground,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}
