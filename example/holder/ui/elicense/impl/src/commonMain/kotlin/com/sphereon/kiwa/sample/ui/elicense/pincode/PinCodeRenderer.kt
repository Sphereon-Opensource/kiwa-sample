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

package com.sphereon.kiwa.sample.ui.elicense.pincode

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sphereon.kiwa.sample.ui.core.theme.AppColors
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.renderer.ComposeRenderer

private const val PIN_LENGTH = 8
private const val PIN_BOX_SIZE = 48
private const val CORNER_RADIUS = 8
private const val KEYPAD_WIDTH = 240
private const val KEYPAD_BUTTON_SIZE = 64
private const val ICON_SIZE = 24
private const val KEYPAD_COLUMNS = 3
private const val KEYPAD_SPACING = 16
private const val CLEAR_TEXT = "clear"
private const val BACKSPACE_TEXT = "backspace"
private const val CANCEL_TEXT = "Cancel"
private const val PIN_DOT_FONT_SIZE = 20
private const val SPACING_STANDARD = 24
private const val PIN_FILLED_ALPHA = 0.1f
private const val PIN_EMPTY_ALPHA = 0.3f
private const val BORDER_WIDTH = 2
private const val BUTTON_FONT_SIZE = 24
private const val TITLE_FONT_SIZE = 24
private const val DESCRIPTION_FONT_SIZE = 16
private const val DESCRIPTION_PADDING = 16
private const val HEADER_TOP_PADDING = 32
private const val HEADER_SPACING = 16
private const val KEYPAD_TOP_SPACING = 32

@ContributesRenderer(modelType = PinCodePresenter.Model::class)
class PinCodeRenderer : ComposeRenderer<PinCodePresenter.Model>() {

    @Composable
    override fun Compose(model: PinCodePresenter.Model) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Screen.background)
                .padding(SPACING_STANDARD.dp)
        ) {
            PinCodeScreen(model)
        }
    }

    @Composable
    private fun PinCodeScreen(model: PinCodePresenter.Model) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            PinCodeHeader(model)
            PinCodeDisplay(pinCode = model.pinCode, modifier = Modifier.padding(vertical = KEYPAD_TOP_SPACING.dp))
            PinCodeKeypadSection(model)
        }
    }

    @Composable
    private fun PinCodeHeader(model: PinCodePresenter.Model) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = HEADER_TOP_PADDING.dp)
        ) {
            Text(
                text = model.title,
                color = AppColors.Screen.foreground,
                fontSize = TITLE_FONT_SIZE.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(HEADER_SPACING.dp))

            Text(
                text = model.description,
                color = AppColors.Misc.mutedText,
                fontSize = DESCRIPTION_FONT_SIZE.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = DESCRIPTION_PADDING.dp)
            )
        }
    }

    @Composable
    private fun PinCodeKeypadSection(model: PinCodePresenter.Model) {
        Column {
            NumericKeypad(
                onDigitClick = { digit ->
                    model.onEvent(PinCodePresenter.Event.OnDigitEntered(digit))
                },
                onBackspaceClick = {
                    model.onEvent(PinCodePresenter.Event.OnBackspace)
                },
                onClearClick = {
                    model.onEvent(PinCodePresenter.Event.OnClear)
                }
            )

            Spacer(modifier = Modifier.height(SPACING_STANDARD.dp))

            OutlinedButton(
                onClick = { model.onEvent(PinCodePresenter.Event.OnCancel) },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppColors.Screen.foreground
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(CANCEL_TEXT)
            }
        }
    }
}

@Composable
private fun PinCodeDisplay(
    pinCode: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(SPACING_STANDARD.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(PIN_LENGTH) { index ->
            PinDigitBox(
                isFilled = index < pinCode.length
            )
        }
    }
}

@Composable
private fun PinDigitBox(
    isFilled: Boolean
) {
    Box(
        modifier = Modifier
            .size(PIN_BOX_SIZE.dp)
            .background(
                color = if (isFilled) {
                    AppColors.Accent.primary.copy(alpha = PIN_FILLED_ALPHA)
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(CORNER_RADIUS.dp)
            )
            .border(
                width = BORDER_WIDTH.dp,
                color = if (isFilled) {
                    AppColors.Accent.primary
                } else {
                    AppColors.Screen.foreground.copy(alpha = PIN_EMPTY_ALPHA)
                },
                shape = RoundedCornerShape(CORNER_RADIUS.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isFilled) {
                "●"
            } else {
                ""
            },
            color = AppColors.Accent.primary,
            fontSize = PIN_DOT_FONT_SIZE.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun NumericKeypad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit
) {
    val keypadItems = listOf(
        "1", "2", "3",
        "4", "5", "6",
        "7", "8", "9",
        CLEAR_TEXT, "0", BACKSPACE_TEXT
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(KEYPAD_COLUMNS),
        horizontalArrangement = Arrangement.spacedBy(KEYPAD_SPACING.dp),
        verticalArrangement = Arrangement.spacedBy(KEYPAD_SPACING.dp),
        modifier = Modifier.width(KEYPAD_WIDTH.dp)
    ) {
        items(keypadItems) { item ->
            KeypadButton(
                item = item,
                onDigitClick = onDigitClick,
                onBackspaceClick = onBackspaceClick,
                onClearClick = onClearClick
            )
        }
    }
}

@Composable
private fun KeypadButton(
    item: String,
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit
) {
    val isDigit = item.all { it.isDigit() }

    Box(
        modifier = Modifier
            .size(KEYPAD_BUTTON_SIZE.dp)
            .clip(CircleShape)
            .background(
                color = when (item) {
                    CLEAR_TEXT, BACKSPACE_TEXT -> AppColors.Screen.foreground.copy(alpha = PIN_FILLED_ALPHA)
                    else -> AppColors.List.rowLight
                }
            )
            .clickable {
                when (item) {
                    CLEAR_TEXT -> onClearClick()
                    BACKSPACE_TEXT -> onBackspaceClick()
                    else -> {
                        if (isDigit) {
                            onDigitClick(item)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when (item) {
            CLEAR_TEXT -> Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear",
                tint = AppColors.Screen.foreground,
                modifier = Modifier.size(ICON_SIZE.dp)
            )

            BACKSPACE_TEXT -> Icon(
                imageVector = Icons.Default.Backspace,
                contentDescription = "Backspace",
                tint = AppColors.Screen.foreground,
                modifier = Modifier.size(ICON_SIZE.dp)
            )

            else -> Text(
                text = item,
                color = AppColors.Screen.foreground,
                fontSize = BUTTON_FONT_SIZE.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
