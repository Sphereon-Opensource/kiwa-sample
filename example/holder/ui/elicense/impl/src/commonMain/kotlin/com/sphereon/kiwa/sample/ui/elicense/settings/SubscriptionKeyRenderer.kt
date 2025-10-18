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
import com.sphereon.kiwa.sample.ui.core.theme.AppColors
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.renderer.ComposeRenderer

// @Inject
@ContributesRenderer(modelType = SubscriptionKeyPresenter.Model::class)
class SubscriptionKeyRenderer : ComposeRenderer<SubscriptionKeyPresenter.Model>() {

    @Composable
    override fun Compose(model: SubscriptionKeyPresenter.Model) {
        when (model) {
            is SubscriptionKeyPresenter.Model.SubscriptionKeyForm -> SubscriptionKeyForm(model)
        }
    }

    @Composable
    private fun SubscriptionKeyForm(model: SubscriptionKeyPresenter.Model.SubscriptionKeyForm) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Screen.background)
                .padding(FORM_PADDING.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                FormHeader()
                FormTextField(model)
                FormButtons(model)
            }
        }
    }

    @Composable
    private fun FormHeader() {
        Spacer(modifier = Modifier.height(HEADER_TOP_SPACING.dp))

        Text(
            text = "Kiwa Subscription Key",
            color = AppColors.Screen.foreground,
            fontSize = TITLE_FONT_SIZE.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(HEADER_SPACING.dp))

        Text(
            text = "Enter your Kiwa subscription key to configure API access.",
            color = AppColors.Screen.foreground.copy(alpha = TEXT_ALPHA),
            fontSize = DESCRIPTION_FONT_SIZE.sp
        )

        Spacer(modifier = Modifier.height(CONTENT_SPACING.dp))
    }

    @Composable
    private fun FormTextField(model: SubscriptionKeyPresenter.Model.SubscriptionKeyForm) {
        OutlinedTextField(
            value = model.subscriptionKey,
            onValueChange = {
                model.onEvent(SubscriptionKeyPresenter.Event.SubscriptionKeyChanged(it))
            },
            label = { Text("Subscription Key") },
            placeholder = { Text("Enter subscription key") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedTextColor = AppColors.Accent.primary,
                unfocusedTextColor = AppColors.Screen.foreground,
                focusedLabelColor = AppColors.Accent.primary,
                unfocusedLabelColor = AppColors.Screen.foreground.copy(alpha = TEXT_ALPHA),
                focusedPlaceholderColor = AppColors.Screen.foreground.copy(alpha = PLACEHOLDER_ALPHA),
                unfocusedPlaceholderColor = AppColors.Screen.foreground.copy(alpha = PLACEHOLDER_ALPHA),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = AppColors.Accent.primary,
                unfocusedIndicatorColor = AppColors.Screen.foreground.copy(alpha = INDICATOR_ALPHA)
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

        Spacer(modifier = Modifier.height(BUTTON_SPACING.dp))
    }

    @Composable
    private fun FormButtons(model: SubscriptionKeyPresenter.Model.SubscriptionKeyForm) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BUTTON_GAP.dp)
        ) {
            Button(
                onClick = { model.onEvent(SubscriptionKeyPresenter.Event.Cancel) },
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
                onClick = { model.onEvent(SubscriptionKeyPresenter.Event.Submit) },
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
                        modifier = Modifier.size(PROGRESS_SIZE.dp),
                        strokeWidth = PROGRESS_STROKE.dp
                    )
                } else {
                    Text("Save")
                }
            }
        }
    }

    private companion object {
        const val FORM_PADDING = 16
        const val HEADER_TOP_SPACING = 24
        const val HEADER_SPACING = 8
        const val CONTENT_SPACING = 24
        const val BUTTON_SPACING = 32
        const val BUTTON_GAP = 16
        const val TITLE_FONT_SIZE = 24
        const val DESCRIPTION_FONT_SIZE = 16
        const val TEXT_ALPHA = 0.7f
        const val PLACEHOLDER_ALPHA = 0.5f
        const val INDICATOR_ALPHA = 0.3f
        const val PROGRESS_SIZE = 16
        const val PROGRESS_STROKE = 2
    }
}
