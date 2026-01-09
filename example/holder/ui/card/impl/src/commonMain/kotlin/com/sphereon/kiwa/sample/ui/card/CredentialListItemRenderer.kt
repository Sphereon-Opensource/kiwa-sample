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

package com.sphereon.kiwa.sample.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.renderer.ComposeRenderer

@Inject
@ContributesRenderer(modelType = CredentialListItemPresenter.Model::class)
class CredentialListItemRenderer(
    val credentialCardRenderer: CredentialCardRenderer
) : ComposeRenderer<CredentialListItemPresenter.Model>() {

    private val fg = Color(WHITE_COLOR)
    private val deletePurple = Color(DELETE_PURPLE_COLOR)

    @Composable
    override fun Compose(model: CredentialListItemPresenter.Model) {
        when (model) {
            is CredentialListItemPresenter.Model.Row -> ListItemRow(model, rowBg = Color.Unspecified)
        }
    }

    @Composable
    fun ListItemRow(model: CredentialListItemPresenter.Model.Row, rowBg: Color) {
        val scope = rememberCoroutineScope()
        val revealWidthPx = with(LocalDensity.current) { REVEAL_WIDTH.toPx() }
        val offsetX = remember { mutableFloatStateOf(0f) }

        Box(modifier = Modifier.fillMaxWidth()) {
            // Background delete action area
            DeleteActionBackground(model)
            // Foreground content (swipeable)
            SwipeableContent(model, rowBg, scope, revealWidthPx, offsetX)
        }
    }

    @Composable
    private fun DeleteActionBackground(model: CredentialListItemPresenter.Model.Row) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val onDelete = model.onDelete
            if (onDelete != null) {
                Box(
                    modifier = Modifier
                        .width(REVEAL_WIDTH)
                        .height(65.dp)
                        .background(deletePurple)
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Delete",
                        color = fg,
                        fontSize = DELETE_BUTTON_FONT_SIZE
                    )
                }
            }
        }
    }

    @Composable
    private fun SwipeableContent(
        model: CredentialListItemPresenter.Model.Row,
        rowBg: Color,
        scope: kotlinx.coroutines.CoroutineScope,
        revealWidthPx: Float,
        offsetX: androidx.compose.runtime.MutableFloatState
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.floatValue.toInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            val new = (offsetX.floatValue + dragAmount).coerceIn(-revealWidthPx, 0f)
                            offsetX.floatValue = new
                        },
                        onDragEnd = {
                            val target = if (offsetX.floatValue <= -revealWidthPx * REVEAL_THRESHOLD) {
                                -revealWidthPx
                            } else {
                                0f
                            }
                            scope.launch { offsetX.floatValue = target }
                        }
                    )
                }
                .background(
                    if (rowBg != Color.Unspecified) {
                        rowBg
                    } else {
                        Color.Transparent
                    },
                    shape = RoundedCornerShape(CORNER_RADIUS)
                )
                .padding(horizontal = CONTENT_HORIZONTAL_PADDING, vertical = CONTENT_VERTICAL_PADDING)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Use actual MiniCard from CredentialCardRenderer
                credentialCardRenderer.renderCompose(model.card)
                Spacer(modifier = Modifier.width(CARD_SPACING))
                // Info column
                InfoColumn(model)
            }
        }
    }

    @Composable
    private fun InfoColumn(model: CredentialListItemPresenter.Model.Row) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = INFO_VERTICAL_PADDING)
        ) {
            // Title row: title + status pill
            TitleRow(model)
            Spacer(modifier = Modifier.height(TITLE_SPACING))
            // Issuer name
            IssuerName(model)
            Spacer(modifier = Modifier.height(ISSUER_SPACING))
            // Date row
            DateRow(model)
        }
    }

    @Composable
    private fun TitleRow(model: CredentialListItemPresenter.Model.Row) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = model.card.title ?: "",
                color = fg,
                fontSize = TITLE_FONT_SIZE,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.weight(1f))
            StatusPill(model)
        }
    }

    @Composable
    private fun StatusPill(model: CredentialListItemPresenter.Model.Row) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(STATUS_PILL_HEIGHT)
                .border(
                    width = 1.dp,
                    color = Color(model.card.statusColor),
                    shape = RoundedCornerShape(STATUS_PILL_CORNER_RADIUS)
                )
                .background(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(STATUS_PILL_CORNER_RADIUS)
                )
        ) {
            Text(
                text = model.card.statusText,
                color = Color(model.card.statusColor),
                fontSize = STATUS_FONT_SIZE,
                modifier = Modifier.padding(
                    horizontal = STATUS_PILL_HORIZONTAL_PADDING,
                    vertical = STATUS_PILL_VERTICAL_PADDING
                )
            )
        }
    }

    @Composable
    private fun IssuerName(model: CredentialListItemPresenter.Model.Row) {
        Text(
            text = model.card.issuerName ?: "",
            color = fg,
            fontSize = ISSUER_FONT_SIZE
        )
    }

    @Composable
    private fun DateRow(model: CredentialListItemPresenter.Model.Row) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = model.card.validFrom,
                color = fg,
                fontSize = DATE_FONT_SIZE
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Expires on ${model.card.expires ?: ""}",
                color = fg,
                fontSize = DATE_FONT_SIZE
            )
        }
    }

    companion object {
        private val REVEAL_WIDTH = 72.dp
        private val CONTAINER_PADDING = 8.dp
        private val CONTENT_HORIZONTAL_PADDING = 16.dp
        private val CONTENT_VERTICAL_PADDING = 4.dp
        private val CARD_SPACING = 12.dp
        private val CORNER_RADIUS = 8.dp
        private val INFO_VERTICAL_PADDING = 10.dp
        private val STATUS_PILL_HEIGHT = 17.dp
        private val STATUS_PILL_CORNER_RADIUS = 9.dp
        private val STATUS_PILL_HORIZONTAL_PADDING = 8.dp
        private val STATUS_PILL_VERTICAL_PADDING = 1.dp
        private val TITLE_SPACING = 2.dp
        private val ISSUER_SPACING = 10.dp

        private val TITLE_FONT_SIZE = 14.sp
        private val DELETE_BUTTON_FONT_SIZE = 12.sp
        private val ISSUER_FONT_SIZE = 12.sp
        private val DATE_FONT_SIZE = 10.sp
        private val STATUS_FONT_SIZE = 10.sp

        private const val WHITE_COLOR = 0xFFFBFBFB
        private const val DELETE_PURPLE_COLOR = 0xFF7276F7
        private const val REVEAL_THRESHOLD = 0.5f
    }
}
