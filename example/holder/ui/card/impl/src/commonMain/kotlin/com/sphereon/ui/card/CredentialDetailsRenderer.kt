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

package com.sphereon.ui.card

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.renderer.ComposeRenderer
import com.sphereon.ui.core.theme.AppColors

@Inject
@ContributesRenderer(modelType = ICredentialDetailsPresenter.Model::class)
class CredentialDetailsRenderer(
    private val cardRenderer: CredentialCardRenderer
) : ComposeRenderer<ICredentialDetailsPresenter.Model>() {

    @Composable
    override fun Compose(model: ICredentialDetailsPresenter.Model) {
        when (model) {
            is ICredentialDetailsPresenter.Model.Content -> Content(model)
        }
    }

    @Composable
    private fun Content(model: ICredentialDetailsPresenter.Model.Content) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Screen.background)
                .padding(16.dp)
        ) {
            // Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                cardRenderer.renderCompose(model.card)
            }
            Spacer(Modifier.height(16.dp))
            // Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.Misc.tabBackground)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val selected = model.selectedTab
                TabButton(
                    text = "Verified info",
                    selected = selected == ICredentialDetailsPresenter.Tab.VerifiedInfo,
                    onClick = {
                        model.onEvent(
                            ICredentialDetailsPresenter.Event.SelectTab(ICredentialDetailsPresenter.Tab.VerifiedInfo)
                        )
                    }
                )
                TabButton(
                    text = "Activity",
                    selected = selected == ICredentialDetailsPresenter.Tab.Activity,
                    onClick = {
                        model.onEvent(
                            ICredentialDetailsPresenter.Event.SelectTab(ICredentialDetailsPresenter.Tab.Activity)
                        )
                    }
                )
            }
            Spacer(Modifier.height(12.dp))
            when (model.selectedTab) {
                ICredentialDetailsPresenter.Tab.VerifiedInfo -> VerifiedInfoList(model)
                ICredentialDetailsPresenter.Tab.Activity -> ActivityPlaceholder()
            }
        }

        if (model.showDeleteModal) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            AppColors.Surface.sheet,
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Delete credential?",
                        color = AppColors.Misc.titleText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "This action cannot be undone.",
                        color = AppColors.Misc.mutedText,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { model.onEvent(ICredentialDetailsPresenter.Event.CancelDelete) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = AppColors.Misc.titleText
                            ),
                            modifier = Modifier.weight(1f)
                        ) { Text("Cancel") }
                        Spacer(Modifier.height(0.dp))
                        Button(
                            onClick = { model.onEvent(ICredentialDetailsPresenter.Event.ConfirmDelete) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.Accent.primary,
                                contentColor = AppColors.Screen.foreground
                            ),
                            modifier = Modifier.weight(1f)
                        ) { Text("Delete") }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    @Composable
    private fun VerifiedInfoList(model: ICredentialDetailsPresenter.Model.Content) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            if (model.verifiedItems.isNotEmpty()) {
                itemsIndexed(model.verifiedItems) { idx, item ->
                    if (idx > 0) {
                        Divider(color = AppColors.Screen.foreground.copy(alpha = 0.15f))
                    }
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = item.label,
                            color = AppColors.Screen.foreground.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = item.value,
                            color = AppColors.Screen.foreground,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No verified data elements", color = AppColors.Screen.foreground.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }

    @Composable
    private fun ActivityPlaceholder() {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("No activity yet", color = AppColors.Screen.foreground.copy(alpha = 0.8f))
        }
    }

    @Composable
    private fun TabButton(text: String, selected: Boolean, onClick: () -> Unit) {
        Button(
            onClick = onClick,
            colors = if (selected) {
                ButtonDefaults.buttonColors(
                    containerColor = AppColors.Accent.primary,
                    contentColor = AppColors.Screen.foreground
                )
            } else {
                ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = AppColors.Screen.foreground,
                    disabledContainerColor = Color.Transparent
                )
            },
        ) {
            Text(text)
        }
    }
}
