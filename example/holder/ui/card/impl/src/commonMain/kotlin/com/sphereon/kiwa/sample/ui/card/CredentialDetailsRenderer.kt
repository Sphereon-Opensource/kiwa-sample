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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sphereon.kiwa.sample.ui.core.theme.AppColors
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.renderer.ComposeRenderer

@Inject
@ContributesRenderer(modelType = CredentialDetailsPresenter.Model::class)
class CredentialDetailsRenderer(
    private val cardRenderer: CredentialCardRenderer
) : ComposeRenderer<CredentialDetailsPresenter.Model>() {
    @Composable
    override fun Compose(model: CredentialDetailsPresenter.Model) {
        when (model) {
            is CredentialDetailsPresenter.Model.Content -> Content(model)
        }
    }

    @Composable
    private fun Content(model: CredentialDetailsPresenter.Model.Content) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Screen.background)
                .padding(16.dp)
        ) {
            // Card
            CardSection(model)
            // Tabs
            TabSection(model)
            // Content based on selected tab
            TabContent(model)
        }

        // Full-screen image modal
        model.fullScreenImage?.let { (imageData, label) ->
            val imageBitmap = remember(imageData) { decodeImageBitmap(imageData) }
            if (imageBitmap != null) {
                FullScreenImageModal(
                    imageBitmap = imageBitmap,
                    contentDescription = label,
                    onClose = { model.onEvent(CredentialDetailsPresenter.Event.CloseImage) }
                )
            }
        }

        // Delete confirmation modal
        if (model.showDeleteModal) {
            DeleteConfirmationModal(model)
        }
    }

    @Composable
    private fun CardSection(model: CredentialDetailsPresenter.Model.Content) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            cardRenderer.renderCompose(model.card)
        }
        Spacer(Modifier.height(16.dp))
    }

    @Composable
    private fun TabSection(model: CredentialDetailsPresenter.Model.Content) {
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
                selected = selected == CredentialDetailsPresenter.Tab.VerifiedInfo,
                onClick = {
                    model.onEvent(
                        CredentialDetailsPresenter.Event.SelectTab(CredentialDetailsPresenter.Tab.VerifiedInfo)
                    )
                }
            )
            TabButton(
                text = "Activity",
                selected = selected == CredentialDetailsPresenter.Tab.Activity,
                onClick = {
                    model.onEvent(
                        CredentialDetailsPresenter.Event.SelectTab(CredentialDetailsPresenter.Tab.Activity)
                    )
                }
            )
        }
        Spacer(Modifier.height(12.dp))
    }

    @Composable
    private fun TabContent(
        model: CredentialDetailsPresenter.Model.Content,
    ) {
        when (model.selectedTab) {
            CredentialDetailsPresenter.Tab.VerifiedInfo -> VerifiedInfoList(model)
            CredentialDetailsPresenter.Tab.Activity -> ActivityPlaceholder()
        }
    }

    @Composable
    private fun DeleteConfirmationModal(model: CredentialDetailsPresenter.Model.Content) {
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
                        onClick = { model.onEvent(CredentialDetailsPresenter.Event.CancelDelete) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = AppColors.Misc.titleText
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                    Spacer(Modifier.height(0.dp))
                    Button(
                        onClick = { model.onEvent(CredentialDetailsPresenter.Event.ConfirmDelete) },
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

    @Composable
    private fun VerifiedInfoList(
        model: CredentialDetailsPresenter.Model.Content
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            if (model.verifiedItems.isNotEmpty()) {
                model.verifiedItems.forEach { item ->
                    renderVerifiedInfoItem(item, level = 0, model = model)
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

    /**
     * Utility to render an image from ByteArray if present. Now supports click to expand.
     */
    @Composable
    private fun VerifiedInfoImage(
        imageData: ByteArray?,
        contentDescription: String?,
        modifier: Modifier = Modifier,
        onClick: ((ByteArray, String) -> Unit)? = null
    ) {
        if (imageData == null) return
        val imageBitmap: ImageBitmap? = remember(imageData) {
            decodeImageBitmap(imageData)
        }
        if (imageBitmap != null) {
            val clickableModifier =
                if (onClick != null)
                    modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).clickable {
                        onClick(imageData, contentDescription ?: "")
                    }
                else
                    modifier.size(56.dp)
            Image(
                bitmap = imageBitmap,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = clickableModifier
            )
        }
    }

    /**
     * Recursively renders a verified info item with proper indentation based on nesting level.
     */
    private fun androidx.compose.foundation.lazy.LazyListScope.renderVerifiedInfoItem(
        item: CredentialDetailsPresenter.VerifiedInfoItem,
        level: Int,
        model: CredentialDetailsPresenter.Model.Content
    ) {
        item {
            val indentDp = (level * 16).dp
            val itemValue = item.value
            Column(
                modifier = Modifier
                    .padding(start = indentDp, top = 8.dp, bottom = 8.dp)
            ) {
                // Only show label if it's not empty
                if (item.label.isNotEmpty()) {
                    Text(
                        text = item.label,
                        color = AppColors.Screen.foreground.copy(alpha = 0.75f),
                        fontSize = 12.sp
                    )
                }
                if (item.imageData != null) {
                    VerifiedInfoImage(
                        imageData = item.imageData,
                        contentDescription = item.label.ifEmpty { null },
                        modifier = Modifier,
                        onClick = { imgData, desc ->
                            model.onEvent(CredentialDetailsPresenter.Event.ViewImage(imgData, desc))
                        }
                    )
                }
                if (itemValue != null) {
                    if (item.label.isNotEmpty() || item.imageData != null) {
                        Spacer(Modifier.height(2.dp))
                    }
                    Text(
                        text = itemValue,
                        color = AppColors.Screen.foreground,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Render children recursively
        item.children?.forEach { child ->
            renderVerifiedInfoItem(child, level + 1, model)
        }

        // Add divider after the entire item tree (only at root level)
        if (level == 0) {
            item {
                HorizontalDivider(color = AppColors.Screen.foreground.copy(alpha = 0.15f))
            }
        }
    }

    /**
     * Full screen image modal with close button.
     */
    @Composable
    private fun FullScreenImageModal(
        imageBitmap: ImageBitmap?,
        contentDescription: String?,
        onClose: () -> Unit
    ) {
        if (imageBitmap == null) return
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Screen.background.copy(alpha = 0.98f))
                .clickable { onClose() }
        ) {
            Image(
                bitmap = imageBitmap,
                contentDescription = contentDescription,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize()
                    .padding(56.dp),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AppColors.Surface.sheet.copy(alpha = 0.9f))
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = AppColors.Screen.foreground,
                    modifier = Modifier.size(24.dp)
                )
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
