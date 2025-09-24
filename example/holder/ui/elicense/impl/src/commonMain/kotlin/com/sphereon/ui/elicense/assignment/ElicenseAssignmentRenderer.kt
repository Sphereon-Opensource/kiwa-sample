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

package com.sphereon.ui.elicense.assignment

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import com.sphereon.di.session.SureSessionScope
import com.sphereon.ui.core.theme.AppColors

@ContributesRenderer(modelType = IElicenseAssignmentPresenter.Model::class)
class ElicenseAssignmentRenderer : ComposeRenderer<IElicenseAssignmentPresenter.Model>() {

    @Composable
    override fun Compose(model: IElicenseAssignmentPresenter.Model) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Screen.background)
        ) {
            when (model) {
                is IElicenseAssignmentPresenter.Model.EnteringPin -> {
                    PinEntryScreen(
                        onPinComplete = model.onPinComplete,
                        onCancel = { model.onEvent(IElicenseAssignmentPresenter.Event.OnCancel) }
                    )
                }

                is IElicenseAssignmentPresenter.Model.AssigningLicense -> {
                    AssigningScreen(
                        pinCode = model.pinCode,
                        onCancel = { model.onEvent(IElicenseAssignmentPresenter.Event.OnCancel) }
                    )
                }

                is IElicenseAssignmentPresenter.Model.AssignmentSuccess -> {
                    SuccessScreen(
                        onEvent = model.onEvent
                    )
                }

                is IElicenseAssignmentPresenter.Model.AssignmentError -> {
                    ErrorScreen(
                        error = model.error,
                        onEvent = model.onEvent
                    )
                }
            }
        }
    }

    @Composable
    private fun PinEntryScreen(
        onPinComplete: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        var pin by remember { mutableStateOf("") }
        val pinLength = 8

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Text(
                    text = "Assign E-License",
                    color = AppColors.Screen.foreground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Enter the 8-digit assignment code sent to your email to activate your e-license",
                    color = AppColors.Misc.mutedText,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // PIN input and keypad section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    (1..pinLength).forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (index <= pin.length) {
                                        AppColors.Accent.primary.copy(alpha = 0.1f)
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (index <= pin.length) {
                                        AppColors.Accent.primary
                                    } else {
                                        AppColors.Screen.foreground.copy(alpha = 0.3f)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (index <= pin.length) {
                                Text(
                                    text = "●",
                                    color = AppColors.Accent.primary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .width(240.dp)
                ) {
                    // Numbers 1-9
                    items((1..9).toList()) { number ->
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(AppColors.List.rowLight)
                                .clickable {
                                    if (pin.length < pinLength) {
                                        pin += number.toString()
                                        if (pin.length == pinLength) {
                                            onPinComplete(pin)
                                        }
                                    }
                                }
                        ) {
                            Text(
                                text = number.toString(),
                                color = AppColors.Screen.foreground,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }

                    // Clear button
                    item {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(AppColors.Screen.foreground.copy(alpha = 0.1f))
                                .clickable {
                                    pin = ""
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear",
                                modifier = Modifier.align(Alignment.Center),
                                tint = AppColors.Screen.foreground
                            )
                        }
                    }

                    // Number 0
                    item {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(AppColors.List.rowLight)
                                .clickable {
                                    if (pin.length < pinLength) {
                                        pin += "0"
                                        if (pin.length == pinLength) {
                                            onPinComplete(pin)
                                        }
                                    }
                                }
                        ) {
                            Text(
                                text = "0",
                                color = AppColors.Screen.foreground,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }

                    // Backspace button
                    item {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(AppColors.Screen.foreground.copy(alpha = 0.1f))
                                .clickable {
                                    if (pin.isNotEmpty()) {
                                        pin = pin.dropLast(1)
                                    }
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Backspace,
                                contentDescription = "Backspace",
                                modifier = Modifier.align(Alignment.Center),
                                tint = AppColors.Screen.foreground
                            )
                        }
                    }
                }
            }

            // Cancel button section
            Column {
                OutlinedButton(
                    onClick = onCancel,
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

    @Composable
    private fun AssigningScreen(
        pinCode: String,
        onCancel: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = AppColors.Accent.primary,
                modifier = Modifier.size(60.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Assigning E-License",
                color = AppColors.Screen.foreground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Please wait while we assign your e-license using code: $pinCode",
                color = AppColors.Misc.mutedText,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedButton(
                onClick = onCancel,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppColors.Screen.foreground
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }

    @Composable
    private fun SuccessScreen(
        onEvent: (IElicenseAssignmentPresenter.Event) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "✓",
                color = AppColors.Accent.primary,
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "E-License Assigned Successfully",
                color = AppColors.Screen.foreground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your e-license has been successfully assigned to this device. You can now use your digital license.",
                color = AppColors.Misc.mutedText,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onEvent(IElicenseAssignmentPresenter.Event.OnCancel) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Accent.primary,
                    contentColor = AppColors.Screen.background
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue")
            }
        }
    }

    @Composable
    private fun ErrorScreen(
        error: String,
        onEvent: (IElicenseAssignmentPresenter.Event) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "⚠",
                color = Color.Red,
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Assignment Failed",
                color = AppColors.Screen.foreground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = error,
                color = AppColors.Misc.mutedText,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onEvent(IElicenseAssignmentPresenter.Event.OnRetry) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Accent.primary,
                    contentColor = AppColors.Screen.background
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Try Again")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { onEvent(IElicenseAssignmentPresenter.Event.OnCancel) },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppColors.Screen.foreground
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }

    @ContributesTo(SureSessionScope::class)
    interface Component {
        val eLicenseAssignmentPresenter: IElicenseAssignmentPresenter
    }
}
