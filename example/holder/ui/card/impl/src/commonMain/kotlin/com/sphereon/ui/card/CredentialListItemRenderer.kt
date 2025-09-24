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
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.renderer.ComposeRenderer

@Inject
@ContributesRenderer(modelType = ICredentialListItemPresenter.Model::class)
class CredentialListItemRenderer(
    val credentialCardRenderer: CredentialCardRenderer
) : ComposeRenderer<ICredentialListItemPresenter.Model>() {

    private val fg = Color(0xFFFBFBFB)
    private val deletePurple = Color(0xFF7276F7)

    @Composable
    override fun Compose(model: ICredentialListItemPresenter.Model) {
        when (model) {
            is ICredentialListItemPresenter.Model.Row -> ListItemRow(model, rowBg = Color.Unspecified)
        }
    }

    @Composable
    fun ListItemRow(model: ICredentialListItemPresenter.Model.Row, rowBg: Color) {
        val revealWidth = 72.dp
        val scope = rememberCoroutineScope()
        val revealWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { revealWidth.toPx() }
        val offsetX = remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Background delete action area sized to row height
            Box(modifier = Modifier.matchParentSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val onDelete = model.onDelete
                    if (onDelete != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(revealWidth)
                                .background(deletePurple, shape = RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = { onDelete() }) {
                                Text("Delete", color = fg, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Foreground content (swipeable)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { androidx.compose.ui.unit.IntOffset(offsetX.floatValue.toInt(), 0) }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, dragAmount ->
                                val new = (offsetX.floatValue + dragAmount).coerceIn(-revealWidthPx, 0f)
                                offsetX.floatValue = new
                            },
                            onDragEnd = {
                                val target = if (offsetX.floatValue <= -revealWidthPx / 2f) -revealWidthPx else 0f
                                scope.launch { offsetX.floatValue = target }
                            }
                        )
                    }
                    .background(
                        if (rowBg != Color.Unspecified) rowBg else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Use actual MiniCard from CredentialCardRenderer
                    credentialCardRenderer.renderCompose(model.card)
                    Spacer(modifier = Modifier.width(12.dp))
                    // Info column
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        // Title row: title + status pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = model.card.title ?: "",
                                color = fg,
                                fontSize = 14.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .height(17.dp)
                                    .border(
                                        width = 1.dp,
                                        color = Color(model.card.statusColor),
                                        shape = RoundedCornerShape(9.dp)
                                    )
                                    .background(
                                        color = Color.Transparent,
                                        shape = RoundedCornerShape(9.dp)
                                    )
                            ) {
                                Text(
                                    text = model.card.statusText,
                                    color = Color(model.card.statusColor),
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        // Issuer name
                        Text(
                            text = model.card.issuerName ?: "",
                            color = fg,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        // Date row
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = model.card.validFrom,
                                color = fg,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "Expires on ${model.card.expires ?: ""}",
                                color = fg,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
