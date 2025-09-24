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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.renderer.ComposeRenderer

@ContributesRenderer(modelType = ICredentialCardPresenter.Model::class)
class CredentialCardRenderer : ComposeRenderer<ICredentialCardPresenter.Model>() {

    @Composable
    override fun Compose(model: ICredentialCardPresenter.Model) {
        when (model.variant) {
            ICredentialCardPresenter.Variant.Mini -> MiniCard(model)
            ICredentialCardPresenter.Variant.Large -> LargeCard(model)
        }
    }

    @Composable
    private fun MiniCard(model: ICredentialCardPresenter.Model) {
        val shape = RoundedCornerShape(4.6.dp)
        Box(
            modifier = Modifier
                .size(width = 75.dp, height = 50.dp)
                .clip(shape)
        ) {
            // Background color or image
            model.backgroundImage?.let { bmp ->
                Image(
                    bitmap = bmp,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } ?: run {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF2CD5C5))
                )
            }
            // Centered logo or globe fallback
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                val logo = model.issuerLogo
                if (logo != null) {
                    Image(
                        bitmap = logo,
                        contentDescription = "Issuer logo",
                        modifier = Modifier.size(24.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    GlobeIcon(iconSize = 24.dp, color = Color(0xFFFBFBFB))
                }
            }
        }
    }

    @Composable
    private fun LargeCard(model: ICredentialCardPresenter.Model) {
        val cardShape = RoundedCornerShape(16.dp)
        Box(
            modifier = Modifier
                .width(327.dp)
                .height(186.dp)
                .clip(cardShape)
        ) {
            // Background
            model.backgroundImage?.let { bmp ->
                Image(
                    bitmap = bmp,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } ?: run {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(model.backgroundColor ?: 0xFF2CD5C5))
                )
            }

            // Foreground content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                        val logo = model.issuerLogo
                        if (logo != null) {
                            Image(
                                bitmap = logo,
                                contentDescription = "Issuer logo",
                                modifier = Modifier.size(32.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            GlobeIcon(iconSize = 20.dp, color = Color(0xFFFBFBFB))
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = model.title ?: "",
                            color = Color(0xFFFBFBFB),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = FontFamily.Default
                        )
                        model.subtitle?.let { sub ->
                            if (sub.isNotBlank()) {
                                Text(
                                    text = sub,
                                    color = Color(0xFFFBFBFB),
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontFamily = FontFamily.Default
                                )
                            }
                        }
                    }
                }
                // Figma: 77.dp gap below header before issuer/footer
                Spacer(modifier = Modifier.height(77.dp))
                // Issuer name row
                Row(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 4.dp)) {
                    Text(
                        text = model.issuerName ?: "",
                        color = Color(0xFFFBFBFB),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.Default
                    )
                }
                // Footer translucent bar
                Row(
                    modifier = Modifier
                        .height(39.dp)
                        .width(327.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        )
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Expires: ${model.expires ?: ""}",
                        color = Color(0xFFFBFBFB),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.Default
                    )
                    // Status pill
                    Row(
                        modifier = Modifier
                            .height(17.dp)
                            .border(
                                width = 1.dp,
                                color = Color(0xFFFBFBFB),
                                shape = RoundedCornerShape(9.dp)
                            )
                            .padding(start = 7.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = model.statusText,
                            color = Color(0xFFFBFBFB),
                            fontWeight = FontWeight.Normal,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Default
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobeIcon(iconSize: androidx.compose.ui.unit.Dp, color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(iconSize)) {
        val w = this.size.width
        val h = this.size.height
        val radius = kotlin.math.min(w, h) / 2f
        val stroke = 2.dp.toPx()
        // Outer circle
        drawCircle(color = color, radius = radius - stroke / 2, style = Stroke(width = stroke))
        // Vertical meridians
        drawLine(
            color,
            start = androidx.compose.ui.geometry.Offset(w * 0.33f, 0f),
            end = androidx.compose.ui.geometry.Offset(w * 0.33f, h),
            strokeWidth = stroke
        )
        drawLine(
            color,
            start = androidx.compose.ui.geometry.Offset(w * 0.66f, 0f),
            end = androidx.compose.ui.geometry.Offset(w * 0.66f, h),
            strokeWidth = stroke
        )
        // Horizontal parallels
        drawLine(
            color,
            start = androidx.compose.ui.geometry.Offset(0f, h * 0.33f),
            end = androidx.compose.ui.geometry.Offset(w, h * 0.33f),
            strokeWidth = stroke
        )
        drawLine(
            color,
            start = androidx.compose.ui.geometry.Offset(0f, h * 0.66f),
            end = androidx.compose.ui.geometry.Offset(w, h * 0.66f),
            strokeWidth = stroke
        )
    }
}
