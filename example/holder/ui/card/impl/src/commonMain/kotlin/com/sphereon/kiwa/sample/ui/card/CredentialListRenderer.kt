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

package com.sphereon.kiwa.sample.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sphereon.kiwa.sample.ui.core.theme.AppColors
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.renderer.ComposeRenderer

@Inject
@ContributesRenderer(modelType = CredentialListPresenter.Model::class)
class CredentialListRenderer(
    private val itemRenderer: CredentialListItemRenderer
) : ComposeRenderer<CredentialListPresenter.Model>() {

    @Composable
    override fun Compose(model: CredentialListPresenter.Model) {
        when (model) {
            is CredentialListPresenter.Model.CredentialList -> CredentialList(model)
        }
    }

    @Composable
    private fun CredentialList(model: CredentialListPresenter.Model.CredentialList) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(AppColors.Screen.background)
        ) {
            if (model.items.isEmpty()) {
                EmptyStateContent(model)
            } else {
                CredentialListContent(model)
            }

            // Bottom confirmation modal
            val pending = model.pendingDelete
            if (pending != null) {
                DeleteConfirmationModal(model, pending)
            }
        }
    }

    @Composable
    private fun EmptyStateContent(model: CredentialListPresenter.Model.CredentialList) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No licenses yet",
                color = AppColors.Screen.foreground,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Please add licenses to start using this app",
                color = AppColors.Screen.foreground,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { model.onStateEvent(CredentialListPresenter.StateEvent.AssignLicense) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Accent.primary,
                    contentColor = AppColors.Screen.foreground
                ),
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Text("Assign License")
            }
        }
    }

    @Composable
    private fun CredentialListContent(model: CredentialListPresenter.Model.CredentialList) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            model.items.forEachIndexed { idx, item ->
                val rowBgColor = if (idx % 2 == 0) {
                    AppColors.List.rowLight
                } else {
                    AppColors.List.rowDark
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBgColor)
                        .clickable {
                            model.onStateEvent(
                                CredentialListPresenter.StateEvent.OpenCredentialDetails(item.document)
                            )
                        }
                ) {
                    itemRenderer.ListItemRow(item, rowBg = rowBgColor)
                }
            }
        }
    }

    @Composable
    private fun DeleteConfirmationModal(
        model: CredentialListPresenter.Model.CredentialList,
        pending: com.sphereon.mdoc.data.device.Document
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // White sheet at the bottom
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
                        onClick = { model.onStateEvent(CredentialListPresenter.StateEvent.CancelDelete) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = AppColors.Misc.titleText
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            model.onStateEvent(
                                CredentialListPresenter.StateEvent.ConfirmDelete(pending)
                            )
                        },
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
