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

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.renderer.ComposeRenderer
import kotlin.math.min

// Globe icon constants
private val GLOBE_STROKE_WIDTH = 2.dp
private const val GRID_LINE_POSITION_33 = 0.33f
private const val GRID_LINE_POSITION_66 = 0.66f

@ContributesRenderer(modelType = CredentialCardPresenter.Model::class)
class CredentialCardRenderer : ComposeRenderer<CredentialCardPresenter.Model>() {

    @Composable
    override fun Compose(model: CredentialCardPresenter.Model) {
        when (model.variant) {
            CredentialCardPresenter.Variant.Mini -> MiniCard(model)
            CredentialCardPresenter.Variant.Large -> LargeCard(model)
        }
    }

    @Composable
    private fun MiniCard(model: CredentialCardPresenter.Model) {
        val shape = RoundedCornerShape(MINI_CARD_CORNER_RADIUS)
        Box(
            modifier = Modifier
                .size(width = MINI_CARD_WIDTH, height = MINI_CARD_HEIGHT)
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
                        .background(Color(DEFAULT_BACKGROUND_COLOR))
                )
            }
            // Centered logo or globe fallback
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = MINI_CARD_PADDING),
                contentAlignment = Alignment.Center
            ) {
                val logo = model.issuerLogo
                if (logo != null) {
                    Image(
                        bitmap = logo,
                        contentDescription = "Issuer logo",
                        modifier = Modifier.size(MINI_ICON_SIZE),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    GlobeIcon(iconSize = MINI_ICON_SIZE, color = Color(WHITE_COLOR))
                }
            }
        }
    }

    @Composable
    private fun LargeCard(model: CredentialCardPresenter.Model) {
        val cardShape = RoundedCornerShape(LARGE_CARD_CORNER_RADIUS)
        Box(
            modifier = Modifier
                .width(LARGE_CARD_WIDTH)
                .height(LARGE_CARD_HEIGHT)
                .clip(cardShape)
        ) {
            // Background
            CardBackground(model)

            // Foreground content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = LARGE_CARD_TOP_PADDING)
            ) {
                // Header
                CardHeader(model)
                // Spacer below header before issuer/footer
                Spacer(modifier = Modifier.height(HEADER_BOTTOM_SPACER))
                // Issuer name row
                CardIssuerRow(model)
                // Footer translucent bar
                CardFooter(model)
            }
        }
    }

    @Composable
    private fun CardBackground(model: CredentialCardPresenter.Model) {
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
                    .background(Color(model.backgroundColor ?: DEFAULT_BACKGROUND_COLOR))
            )
        }
    }

    @Composable
    private fun CardHeader(model: CredentialCardPresenter.Model) {
        Row(
            modifier = Modifier.padding(horizontal = LARGE_CARD_HORIZONTAL_PADDING),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.size(LARGE_LOGO_SIZE), contentAlignment = Alignment.Center) {
                val logo = model.issuerLogo
                if (logo != null) {
                    Image(
                        bitmap = logo,
                        contentDescription = "Issuer logo",
                        modifier = Modifier.size(LARGE_LOGO_SIZE),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    GlobeIcon(iconSize = LARGE_GLOBE_SIZE, color = Color(WHITE_COLOR))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = model.title ?: "",
                    color = Color(WHITE_COLOR),
                    fontWeight = FontWeight.Medium,
                    fontSize = TITLE_FONT_SIZE,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = FontFamily.Default
                )
                model.subtitle?.let { sub ->
                    if (sub.isNotBlank()) {
                        Text(
                            text = sub,
                            color = Color(WHITE_COLOR),
                            fontWeight = FontWeight.Normal,
                            fontSize = SUBTITLE_FONT_SIZE,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = FontFamily.Default
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun CardIssuerRow(model: CredentialCardPresenter.Model) {
        Row(
            modifier = Modifier.padding(
                start = LARGE_CARD_HORIZONTAL_PADDING,
                end = LARGE_CARD_HORIZONTAL_PADDING,
                top = FOOTER_ROW_PADDING_TOP,
                bottom = FOOTER_ROW_PADDING_BOTTOM
            )
        ) {
            Text(
                text = model.issuerName ?: "",
                color = Color(WHITE_COLOR),
                fontSize = ISSUER_FONT_SIZE,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.Default
            )
        }
    }

    @Composable
    private fun CardFooter(model: CredentialCardPresenter.Model) {
        Row(
            modifier = Modifier
                .height(FOOTER_HEIGHT)
                .width(LARGE_CARD_WIDTH)
                .background(
                    color = Color.White.copy(alpha = FOOTER_ALPHA),
                    shape = RoundedCornerShape(
                        bottomStart = LARGE_CARD_CORNER_RADIUS,
                        bottomEnd = LARGE_CARD_CORNER_RADIUS
                    )
                )
                .padding(
                    vertical = FOOTER_PADDING_VERTICAL,
                    horizontal = LARGE_CARD_HORIZONTAL_PADDING
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Expires: ${model.expires ?: ""}",
                color = Color(WHITE_COLOR),
                fontSize = FOOTER_TEXT_FONT_SIZE,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.Default
            )
            // Status pill
            Row(
                modifier = Modifier
                    .height(STATUS_PILL_HEIGHT)
                    .border(
                        width = 1.dp,
                        color = Color(WHITE_COLOR),
                        shape = RoundedCornerShape(STATUS_PILL_CORNER_RADIUS)
                    )
                    .padding(start = STATUS_PILL_PADDING_START, end = STATUS_PILL_PADDING_END),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = model.statusText,
                    color = Color(WHITE_COLOR),
                    fontWeight = FontWeight.Normal,
                    fontSize = FOOTER_TEXT_FONT_SIZE,
                    fontFamily = FontFamily.Default
                )
            }
        }
    }

    companion object {
        // Mini card dimensions
        private val MINI_CARD_WIDTH = 75.dp
        private val MINI_CARD_HEIGHT = 50.dp
        private val MINI_CARD_CORNER_RADIUS = 4.6.dp
        private val MINI_CARD_PADDING = 16.dp
        private val MINI_ICON_SIZE = 24.dp

        // Large card dimensions
        private val LARGE_CARD_WIDTH = 327.dp
        private val LARGE_CARD_HEIGHT = 186.dp
        private val LARGE_CARD_CORNER_RADIUS = 16.dp
        private val LARGE_CARD_TOP_PADDING = 16.dp
        private val LARGE_CARD_HORIZONTAL_PADDING = 12.dp
        private val LARGE_LOGO_SIZE = 32.dp
        private val LARGE_GLOBE_SIZE = 20.dp

        // Spacings
        private val HEADER_BOTTOM_SPACER = 77.dp
        private val FOOTER_ROW_PADDING_TOP = 2.dp
        private val FOOTER_ROW_PADDING_BOTTOM = 4.dp
        private val FOOTER_HEIGHT = 39.dp
        private val FOOTER_PADDING_VERTICAL = 12.dp
        private val STATUS_PILL_HEIGHT = 17.dp
        private val STATUS_PILL_CORNER_RADIUS = 9.dp
        private val STATUS_PILL_PADDING_START = 7.dp
        private val STATUS_PILL_PADDING_END = 8.dp

        // Font sizes
        private val TITLE_FONT_SIZE = 14.sp
        private val SUBTITLE_FONT_SIZE = 10.sp
        private val ISSUER_FONT_SIZE = 10.sp
        private val FOOTER_TEXT_FONT_SIZE = 10.sp

        // Colors
        private const val DEFAULT_BACKGROUND_COLOR = 0xFF2CD5C5
        private const val WHITE_COLOR = 0xFFFBFBFB
        private const val FOOTER_ALPHA = 0.25f
    }
}

@Composable
private fun GlobeIcon(iconSize: Dp, color: Color) {
    Canvas(modifier = Modifier.size(iconSize)) {
        val w = this.size.width
        val h = this.size.height
        val radius = min(w, h) / 2f
        val stroke = GLOBE_STROKE_WIDTH.toPx()
        // Outer circle
        drawCircle(color = color, radius = radius - stroke / 2, style = Stroke(width = stroke))
        // Vertical meridians
        drawLine(
            color,
            start = Offset(w * GRID_LINE_POSITION_33, 0f),
            end = Offset(w * GRID_LINE_POSITION_33, h),
            strokeWidth = stroke
        )
        drawLine(
            color,
            start = Offset(w * GRID_LINE_POSITION_66, 0f),
            end = Offset(w * GRID_LINE_POSITION_66, h),
            strokeWidth = stroke
        )
        // Horizontal parallels
        drawLine(
            color,
            start = Offset(0f, h * GRID_LINE_POSITION_33),
            end = Offset(w, h * GRID_LINE_POSITION_33),
            strokeWidth = stroke
        )
        drawLine(
            color,
            start = Offset(0f, h * GRID_LINE_POSITION_66),
            end = Offset(w, h * GRID_LINE_POSITION_66),
            strokeWidth = stroke
        )
    }
}
