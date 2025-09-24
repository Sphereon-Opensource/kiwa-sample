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

package com.sphereon.ui.elicense.pincode

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
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.renderer.ComposeRenderer
import com.sphereon.ui.core.theme.AppColors

@ContributesRenderer(modelType = IPinCodePresenter.Model::class)
class PinCodeRenderer : ComposeRenderer<IPinCodePresenter.Model>() {

    @Composable
    override fun Compose(model: IPinCodePresenter.Model) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Screen.background)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 32.dp)
                ) {
                    Text(
                        text = model.title,
                        color = AppColors.Screen.foreground,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = model.description,
                        color = AppColors.Misc.mutedText,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // PIN Display Section
                PinCodeDisplay(
                    pinCode = model.pinCode,
                    modifier = Modifier.padding(vertical = 32.dp)
                )

                // Keypad Section
                Column {
                    NumericKeypad(
                        onDigitClick = { digit ->
                            model.onEvent(IPinCodePresenter.Event.OnDigitEntered(digit))
                        },
                        onBackspaceClick = {
                            model.onEvent(IPinCodePresenter.Event.OnBackspace)
                        },
                        onClearClick = {
                            model.onEvent(IPinCodePresenter.Event.OnClear)
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Cancel Button
                    OutlinedButton(
                        onClick = { model.onEvent(IPinCodePresenter.Event.OnCancel) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AppColors.Screen.foreground
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }
                }
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(8) { index ->
            PinDigitBox(
                digit = if (index < pinCode.length) pinCode[index].toString() else "",
                isFilled = index < pinCode.length
            )
        }
    }
}

@Composable
private fun PinDigitBox(
    digit: String,
    isFilled: Boolean
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                color = if (isFilled) AppColors.Accent.primary.copy(alpha = 0.1f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 2.dp,
                color = if (isFilled) AppColors.Accent.primary else AppColors.Screen.foreground.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isFilled) "●" else "",
            color = AppColors.Accent.primary,
            fontSize = 20.sp,
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
        "clear", "0", "backspace"
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.width(240.dp)
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
            .size(64.dp)
            .clip(CircleShape)
            .background(
                color = when (item) {
                    "clear", "backspace" -> AppColors.Screen.foreground.copy(alpha = 0.1f)
                    else -> AppColors.List.rowLight
                }
            )
            .clickable {
                when (item) {
                    "clear" -> onClearClick()
                    "backspace" -> onBackspaceClick()
                    else -> if (isDigit) onDigitClick(item)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when (item) {
            "clear" -> Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear",
                tint = AppColors.Screen.foreground,
                modifier = Modifier.size(24.dp)
            )

            "backspace" -> Icon(
                imageVector = Icons.Default.Backspace,
                contentDescription = "Backspace",
                tint = AppColors.Screen.foreground,
                modifier = Modifier.size(24.dp)
            )

            else -> Text(
                text = item,
                color = AppColors.Screen.foreground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
