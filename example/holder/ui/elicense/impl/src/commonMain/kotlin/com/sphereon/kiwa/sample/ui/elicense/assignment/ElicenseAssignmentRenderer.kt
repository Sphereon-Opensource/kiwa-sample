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

package com.sphereon.kiwa.sample.ui.elicense.assignment

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import com.sphereon.di.session.SessionScope
import com.sphereon.kiwa.sample.ui.core.theme.AppColors
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.renderer.ComposeRenderer
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@ContributesRenderer(modelType = ElicenseAssignmentPresenter.Model::class)
class ElicenseAssignmentRenderer : ComposeRenderer<ElicenseAssignmentPresenter.Model>() {

    @Composable
    override fun Compose(model: ElicenseAssignmentPresenter.Model) {
        // Determine the state key for crossfade transitions.
        // Using Crossfade instead of key() allows for smoother transitions that don't
        // immediately tear down the composable tree. This prevents iOS Compose crashes
        // where LayoutNode disposal during render frames causes DepthSortedSet issues.
        val stateKey = when (model) {
            is ElicenseAssignmentPresenter.Model.EnteringPin -> "entering_pin"
            is ElicenseAssignmentPresenter.Model.AssigningLicense -> "assigning"
            is ElicenseAssignmentPresenter.Model.AssignmentSuccess -> "success"
            is ElicenseAssignmentPresenter.Model.AssignmentError -> "error"
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Screen.background)
        ) {
            // Use Crossfade for smooth transitions between states.
            // This keeps both old and new composables briefly in memory during transition,
            // allowing for gradual disposal that doesn't conflict with the render cycle.
            // The 200ms animation duration gives enough time for proper cleanup.
            Crossfade(
                targetState = stateKey,
                animationSpec = tween(durationMillis = CROSSFADE_DURATION_MS),
                label = "assignment_state_transition"
            ) { currentState ->
                when (currentState) {
                    "entering_pin" -> {
                        val pinModel = model as? ElicenseAssignmentPresenter.Model.EnteringPin
                        if (pinModel != null) {
                            PinEntryScreen(
                                onPinComplete = pinModel.onPinComplete,
                                onCancel = { pinModel.onEvent(ElicenseAssignmentPresenter.Event.OnCancel) }
                            )
                        }
                    }

                    "assigning" -> {
                        val assigningModel = model as? ElicenseAssignmentPresenter.Model.AssigningLicense
                        if (assigningModel != null) {
                            AssigningScreen(
                                pinCode = assigningModel.pinCode,
                                onCancel = { assigningModel.onEvent(ElicenseAssignmentPresenter.Event.OnCancel) }
                            )
                        }
                    }

                    "success" -> {
                        val successModel = model as? ElicenseAssignmentPresenter.Model.AssignmentSuccess
                        if (successModel != null) {
                            SuccessScreen(
                                onEvent = successModel.onEvent
                            )
                        }
                    }

                    "error" -> {
                        val errorModel = model as? ElicenseAssignmentPresenter.Model.AssignmentError
                        if (errorModel != null) {
                            ErrorScreen(
                                error = errorModel.error,
                                onEvent = errorModel.onEvent
                            )
                        }
                    }
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(SCREEN_PADDING.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            PinEntryHeader()
            PinEntryContent(
                pin = pin,
                onPinChanged = { newPin ->
                    pin = newPin
                    if (newPin.length == PIN_LENGTH) {
                        onPinComplete(newPin)
                    }
                }
            )
            PinEntryCancelButton(onCancel)
        }
    }

    @Composable
    private fun PinEntryHeader() {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = HEADER_TOP_PADDING.dp)
        ) {
            Text(
                text = "Assign E-License",
                color = AppColors.Screen.foreground,
                fontSize = TITLE_FONT_SIZE.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(HEADER_SPACING.dp))

            Text(
                text = "Enter the $PIN_LENGTH-digit assignment code sent to your email to activate your e-license",
                color = AppColors.Misc.mutedText,
                fontSize = DESCRIPTION_FONT_SIZE.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = DESCRIPTION_PADDING.dp)
            )
        }
    }

    @Composable
    private fun PinEntryContent(
        pin: String,
        onPinChanged: (String) -> Unit
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PinIndicatorRow(pin)
            Spacer(modifier = Modifier.height(KEYPAD_TOP_SPACING.dp))
            NumericKeypad(pin, onPinChanged)
        }
    }

    @Composable
    private fun PinIndicatorRow(pin: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PIN_ROW_PADDING.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (index in 1..PIN_LENGTH) {
                PinIndicatorBox(index, pin.length)
            }
        }
    }

    @Composable
    private fun PinIndicatorBox(index: Int, pinLength: Int) {
        Box(
            modifier = Modifier
                .size(PIN_INDICATOR_SIZE.dp)
                .clip(RoundedCornerShape(CORNER_RADIUS.dp))
                .background(
                    if (index <= pinLength) {
                        AppColors.Accent.primary.copy(alpha = INDICATOR_FILLED_ALPHA)
                    } else {
                        Color.Transparent
                    }
                )
                .border(
                    width = BORDER_WIDTH.dp,
                    color = if (index <= pinLength) {
                        AppColors.Accent.primary
                    } else {
                        AppColors.Screen.foreground.copy(alpha = INDICATOR_BORDER_ALPHA)
                    },
                    shape = RoundedCornerShape(CORNER_RADIUS.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (index <= pinLength) {
                Text(
                    text = "●",
                    color = AppColors.Accent.primary,
                    fontSize = INDICATOR_DOT_SIZE.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    @Composable
    private fun NumericKeypad(pin: String, onPinChanged: (String) -> Unit) {
        // Use regular Column/Row layout instead of LazyVerticalGrid to avoid
        // iOS Compose crashes during state transitions. LazyVerticalGrid's internal
        // DepthSortedSet can fail when layout nodes are removed during render frames.
        // Since we only have 12 buttons, lazy loading provides no benefit anyway.
        Column(
            verticalArrangement = Arrangement.spacedBy(KEYPAD_SPACING.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(KEYPAD_WIDTH.dp)
        ) {
            // Row 1: 1, 2, 3
            Row(horizontalArrangement = Arrangement.spacedBy(KEYPAD_SPACING.dp)) {
                KeypadNumberButton(1, pin, onPinChanged)
                KeypadNumberButton(2, pin, onPinChanged)
                KeypadNumberButton(3, pin, onPinChanged)
            }
            // Row 2: 4, 5, 6
            Row(horizontalArrangement = Arrangement.spacedBy(KEYPAD_SPACING.dp)) {
                KeypadNumberButton(4, pin, onPinChanged)
                KeypadNumberButton(5, pin, onPinChanged)
                KeypadNumberButton(6, pin, onPinChanged)
            }
            // Row 3: 7, 8, 9
            Row(horizontalArrangement = Arrangement.spacedBy(KEYPAD_SPACING.dp)) {
                KeypadNumberButton(7, pin, onPinChanged)
                KeypadNumberButton(8, pin, onPinChanged)
                KeypadNumberButton(9, pin, onPinChanged)
            }
            // Row 4: Clear, 0, Backspace
            Row(horizontalArrangement = Arrangement.spacedBy(KEYPAD_SPACING.dp)) {
                KeypadClearButton { onPinChanged("") }
                KeypadNumberButton(0, pin, onPinChanged)
                KeypadBackspaceButton(pin, onPinChanged)
            }
        }
    }

    @Composable
    private fun KeypadNumberButton(number: Int, pin: String, onPinChanged: (String) -> Unit) {
        Box(
            modifier = Modifier
                .size(KEYPAD_BUTTON_SIZE.dp)
                .clip(CircleShape)
                .background(AppColors.List.rowLight)
                .clickable {
                    if (pin.length < PIN_LENGTH) {
                        onPinChanged(pin + number.toString())
                    }
                }
        ) {
            Text(
                text = number.toString(),
                color = AppColors.Screen.foreground,
                fontSize = KEYPAD_FONT_SIZE.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }

    @Composable
    private fun KeypadClearButton(onClear: () -> Unit) {
        Box(
            modifier = Modifier
                .size(KEYPAD_BUTTON_SIZE.dp)
                .clip(CircleShape)
                .background(AppColors.Screen.foreground.copy(alpha = BUTTON_ALPHA))
                .clickable(onClick = onClear)
        ) {
            Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = "Clear",
                modifier = Modifier.align(Alignment.Center),
                tint = AppColors.Screen.foreground
            )
        }
    }

    @Composable
    private fun KeypadBackspaceButton(pin: String, onPinChanged: (String) -> Unit) {
        Box(
            modifier = Modifier
                .size(KEYPAD_BUTTON_SIZE.dp)
                .clip(CircleShape)
                .background(AppColors.Screen.foreground.copy(alpha = BUTTON_ALPHA))
                .clickable {
                    if (pin.isNotEmpty()) {
                        onPinChanged(pin.dropLast(1))
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

    @Composable
    private fun PinEntryCancelButton(onCancel: () -> Unit) {
        Column {
            OutlinedButton(
                onClick = onCancel,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppColors.Screen.foreground
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(CANCEL_TEXT)
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
                modifier = Modifier.size(CIRCULAR_PROGRESS_SIZE.dp),
                color = AppColors.Accent.primary,
                strokeWidth = 4.dp
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
                Text(CANCEL_TEXT)
            }
        }
    }

    @Composable
    private fun SuccessScreen(
        onEvent: (ElicenseAssignmentPresenter.Event) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(SUCCESS_SCREEN_PADDING.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "✓",
                color = AppColors.Accent.primary,
                fontSize = SUCCESS_CHECKMARK_FONT_SIZE.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(SUCCESS_VERTICAL_SPACING.dp))

            Text(
                text = "E-License Assigned Successfully",
                color = AppColors.Screen.foreground,
                fontSize = SUCCESS_TITLE_FONT_SIZE.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(SPACING_STANDARD.dp))

            Text(
                text = "Your e-license has been successfully assigned to this device. You can now use your digital license.",
                color = AppColors.Misc.mutedText,
                fontSize = DESCRIPTION_FONT_SIZE.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = DESCRIPTION_PADDING.dp)
            )

            Spacer(modifier = Modifier.height(SUCCESS_VERTICAL_SPACING.dp))

            Button(
                onClick = { onEvent(ElicenseAssignmentPresenter.Event.OnCancel) },
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
        onEvent: (ElicenseAssignmentPresenter.Event) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(SCREEN_PADDING.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "⚠",
                color = Color.Red,
                fontSize = ERROR_WARNING_FONT_SIZE.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(SPACING_STANDARD.dp))

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
                onClick = { onEvent(ElicenseAssignmentPresenter.Event.OnRetry) },
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
                onClick = { onEvent(ElicenseAssignmentPresenter.Event.OnCancel) },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppColors.Screen.foreground
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(CANCEL_TEXT)
            }
        }
    }

    private companion object {
        const val CROSSFADE_DURATION_MS = 200
        const val PIN_LENGTH = 8
        const val KEYPAD_DIGIT_MAX = 9
        const val PIN_INDICATOR_SIZE = 32
        const val KEYPAD_BUTTON_SIZE = 64
        const val CIRCULAR_PROGRESS_SIZE = 60
        const val SUCCESS_CHECKMARK_FONT_SIZE = 72
        const val ERROR_WARNING_FONT_SIZE = 72
        const val SUCCESS_TITLE_FONT_SIZE = 24
        const val SUCCESS_VERTICAL_SPACING = 32
        const val CANCEL_TEXT = "Cancel"
        const val BORDER_WIDTH = 2
        const val CORNER_RADIUS = 8
        const val INDICATOR_PADDING = 4
        const val SCREEN_PADDING = 24
        const val SUCCESS_SCREEN_PADDING = 24
        const val HEADER_TOP_PADDING = 32
        const val HEADER_SPACING = 16
        const val SPACING_STANDARD = 16
        const val KEYPAD_TOP_SPACING = 32
        const val KEYPAD_SPACING = 12
        const val KEYPAD_WIDTH = 240
        const val KEYPAD_COLUMNS = 3
        const val PIN_ROW_PADDING = 8
        const val TITLE_FONT_SIZE = 24
        const val DESCRIPTION_FONT_SIZE = 16
        const val DESCRIPTION_PADDING = 16
        const val KEYPAD_FONT_SIZE = 24
        const val INDICATOR_DOT_SIZE = 16
        const val INDICATOR_FILLED_ALPHA = 0.1f
        const val INDICATOR_BORDER_ALPHA = 0.3f
        const val BUTTON_ALPHA = 0.1f
    }

    @ContributesTo(SessionScope::class)
    interface Component {
        val eLicenseAssignmentPresenter: ElicenseAssignmentPresenter
    }
}
