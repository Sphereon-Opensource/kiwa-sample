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

package com.sphereon.kiwa.sample.ui.auth.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.renderer.ComposeRenderer

private object UiConstants {
    const val PADDING_SMALL = 8
    const val PADDING_MEDIUM = 16
    const val PADDING_LARGE = 24
    const val PASSWORD_LENGTH = 6
    const val LABEL_ALPHA = 0.7f
    const val DISABLED_ALPHA = 0.4f
    const val DISABLED_CONTENT_ALPHA = 0.6f
    const val TRANSPARENT_BG_ALPHA = 0.12f
}

private object ColorHex {
    const val BG_COLOR = 0xFF202537
    const val FG_COLOR = 0xFFFBFBFB
    const val INITIAL_LABEL_COLOR = 0xCCFFFFFF
    const val ACCENT_COLOR = 0xFF0B81FF
    const val BUTTON_PRIMARY_COLOR = 0xFF7276F7
    const val BUTTON_DELETE_COLOR = 0xFFFF0000
}

private object ColorConstants {
    val BG_COLOR = Color(ColorHex.BG_COLOR)
    val FG_COLOR = Color(ColorHex.FG_COLOR)
    val INITIAL_LABEL_COLOR = Color(ColorHex.INITIAL_LABEL_COLOR)
    val ACCENT_COLOR = Color(ColorHex.ACCENT_COLOR)
    val BUTTON_PRIMARY_COLOR = Color(ColorHex.BUTTON_PRIMARY_COLOR)
    val BUTTON_DELETE_COLOR = Color(ColorHex.BUTTON_DELETE_COLOR)
}

@ContributesRenderer(modelType = AuthPresenter.Model::class)
class AuthRenderer : ComposeRenderer<AuthPresenter.Model>() {

    private val bg = ColorConstants.BG_COLOR
    private val fg = ColorConstants.FG_COLOR
    private val initialLabel = ColorConstants.INITIAL_LABEL_COLOR
    private val accent = ColorConstants.ACCENT_COLOR

    @Composable
    override fun Compose(model: AuthPresenter.Model) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(UiConstants.PADDING_MEDIUM.dp)
        ) {
            when (model) {
                is AuthPresenter.Model.CheckSavedPassword -> {
                    Column(Modifier.align(Alignment.Center)) {
                        Text("Checking saved credentials…", color = fg)
                    }
                }

                is AuthPresenter.Model.Login -> {
                    LoginContent(model, fg, initialLabel, accent)
                }

                is AuthPresenter.Model.CreateAccount -> {
                    CreateAccountContent(model, fg = fg, initialLabel = initialLabel, accent = accent)
                }
            }
        }
    }
}

@Composable
private fun LoginContent(
    model: AuthPresenter.Model.Login,
    fg: Color,
    initialLabel: Color,
    accent: Color,
) {
    val password = remember { mutableStateOf(model.password) }
    val rememberMe = remember { mutableStateOf(model.remember) }
    val showDeleteConfirmation = remember { mutableStateOf(false) }
    val passwordError = remember { mutableStateOf<String?>(null) }

    // Validation function for 6-digit password
    fun validatePassword(): Boolean {
        val isValid = password.value.length == UiConstants.PASSWORD_LENGTH &&
            password.value.all { it.isDigit() }
        passwordError.value = when {
            password.value.isEmpty() -> "Password is required"
            password.value.length != UiConstants.PASSWORD_LENGTH -> "Password must be exactly 6 digits"
            !password.value.all { it.isDigit() } -> "Password must contain only digits"
            else -> null
        }
        return isValid
    }

    fun onPasswordChange(newPassword: String) {
        // Only allow digits and limit to 6 characters
        if (newPassword.all { it.isDigit() } &&
            newPassword.length <= UiConstants.PASSWORD_LENGTH
        ) {
            password.value = newPassword
            if (passwordError.value != null) {
                validatePassword()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Sign in", color = fg, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(UiConstants.PADDING_LARGE.dp))

        PasswordTextField(
            value = password.value,
            onValueChange = ::onPasswordChange,
            error = passwordError.value,
            fg = fg,
            initialLabel = initialLabel,
            accent = accent
        )

        Spacer(Modifier.height(UiConstants.PADDING_MEDIUM.dp))

        RememberMeCheckbox(
            checked = rememberMe.value,
            onCheckedChange = { rememberMe.value = it },
            fg = fg,
            accent = accent
        )

        Spacer(Modifier.height(UiConstants.PADDING_MEDIUM.dp))

        LoginButtons(
            password = password.value,
            onLoginClick = {
                if (validatePassword()) {
                    model.onEvent(AuthPresenter.Event.OnLoginClicked(password.value, rememberMe.value))
                }
            },
            onDeleteClick = { showDeleteConfirmation.value = true },
            fg = fg
        )
    }

    // Delete Account Confirmation Dialog
    if (showDeleteConfirmation.value) {
        DeleteAccountDialog(
            onConfirm = {
                showDeleteConfirmation.value = false
                model.onEvent(AuthPresenter.Event.DeleteAccount)
            },
            onDismiss = { showDeleteConfirmation.value = false }
        )
    }
}

@Composable
private fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    fg: Color,
    initialLabel: Color,
    accent: Color
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("6-Digit Password") },
        placeholder = { Text("Enter 6-digit password", color = initialLabel) },
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedTextColor = accent,
            unfocusedTextColor = fg,
            focusedLabelColor = accent,
            unfocusedLabelColor = initialLabel,
            focusedPlaceholderColor = initialLabel.copy(alpha = UiConstants.LABEL_ALPHA),
            unfocusedPlaceholderColor = initialLabel,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = accent,
            unfocusedIndicatorColor = initialLabel
        ),
        isError = error != null,
        supportingText = {
            if (error != null) {
                Text(error, color = Color.Red)
            } else {
                Text(
                    "Enter your 6-digit password",
                    color = initialLabel.copy(alpha = UiConstants.LABEL_ALPHA)
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done
        )
    )
}

@Composable
private fun RememberMeCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    fg: Color,
    accent: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = accent),
        )
        Text("Remember password", color = fg)
    }
}

@Composable
private fun LoginButtons(
    password: String,
    onLoginClick: () -> Unit,
    onDeleteClick: () -> Unit,
    fg: Color
) {
    Button(
        onClick = onLoginClick,
        enabled = password.length == UiConstants.PASSWORD_LENGTH &&
            password.all { it.isDigit() },
        colors = ButtonDefaults.buttonColors(
            containerColor = ColorConstants.BUTTON_PRIMARY_COLOR,
            contentColor = fg,
            disabledContainerColor = ColorConstants.BUTTON_PRIMARY_COLOR.copy(
                alpha = UiConstants.DISABLED_ALPHA
            ),
            disabledContentColor = fg.copy(alpha = UiConstants.DISABLED_CONTENT_ALPHA)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Continue")
    }

    Spacer(Modifier.height(UiConstants.PADDING_SMALL.dp))

    Button(
        onClick = onDeleteClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = ColorConstants.BUTTON_DELETE_COLOR,
            contentColor = fg
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Delete Account")
    }
}

@Composable
private fun DeleteAccountDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Account?") },
        text = {
            Text("This action cannot be undone. All your account data will be permanently deleted.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = Color.Red)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CreateAccountContent(
    model: AuthPresenter.Model.CreateAccount,
    fg: Color,
    initialLabel: Color,
    accent: Color,
) {
    val formState = rememberCreateAccountFormState(model)
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                keyboardController?.hide()
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create account", color = fg, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(UiConstants.PADDING_MEDIUM.dp))

        CreateAccountFormFields(formState, fg, initialLabel, accent)

        CreateAccountButtons(
            formValid = formState.isFormValid,
            onContinueClick = {
                keyboardController?.hide()
                if (formState.validateAll()) {
                    model.onEvent(
                        AuthPresenter.Event.OnCreateAccountClicked(
                            username = formState.username,
                            password = formState.password,
                            confirmPassword = formState.confirmPassword,
                            remember = formState.rememberMe
                        )
                    )
                }
            },
            onBackClick = {
                keyboardController?.hide()
                model.onEvent(AuthPresenter.Event.OnBackFromCreate)
            },
            fg = fg
        )

        // Add bottom padding to ensure buttons are visible when keyboard is shown
        Spacer(Modifier.height(UiConstants.PADDING_LARGE.dp))
    }
}

@Composable
private fun CreateAccountFormFields(
    formState: CreateAccountFormState,
    fg: Color,
    initialLabel: Color,
    accent: Color
) {
    CreateAccountEmailField(
        value = formState.username,
        onValueChange = formState::onEmailChange,
        error = formState.emailError,
        fg = fg,
        initialLabel = initialLabel,
        accent = accent
    )
    Spacer(Modifier.height(UiConstants.PADDING_SMALL.dp))
    CreateAccountPasswordField(
        value = formState.password,
        onValueChange = formState::onPasswordChange,
        error = formState.passwordError,
        fg = fg,
        initialLabel = initialLabel,
        accent = accent
    )
    Spacer(Modifier.height(UiConstants.PADDING_SMALL.dp))
    CreateAccountConfirmPasswordField(
        value = formState.confirmPassword,
        onValueChange = formState::onConfirmChange,
        error = formState.confirmError,
        fg = fg,
        initialLabel = initialLabel,
        accent = accent
    )
    Spacer(Modifier.height(UiConstants.PADDING_SMALL.dp))
    RememberMeCheckbox(
        checked = formState.rememberMe,
        onCheckedChange = formState::onRememberChange,
        fg = fg,
        accent = accent
    )
}

@Composable
private fun rememberCreateAccountFormState(model: AuthPresenter.Model.CreateAccount): CreateAccountFormState {
    val username = remember { mutableStateOf(model.username) }
    val password = remember { mutableStateOf(model.password) }
    val confirmPassword = remember { mutableStateOf(model.confirmPassword) }
    val rememberMe = remember { mutableStateOf(model.remember) }
    val emailError = remember { mutableStateOf<String?>(null) }
    val passwordError = remember { mutableStateOf<String?>(null) }
    val confirmError = remember { mutableStateOf<String?>(null) }

    return remember(username.value, password.value, confirmPassword.value, rememberMe.value) {
        CreateAccountFormState(
            username = username.value,
            password = password.value,
            confirmPassword = confirmPassword.value,
            rememberMe = rememberMe.value,
            emailError = emailError.value,
            passwordError = passwordError.value,
            confirmError = confirmError.value,
            onUsernameChange = { username.value = it },
            onPasswordChange = { password.value = it },
            onConfirmPasswordChange = { confirmPassword.value = it },
            onRememberMeChange = { rememberMe.value = it },
            setEmailError = { emailError.value = it },
            setPasswordError = { passwordError.value = it },
            setConfirmError = { confirmError.value = it }
        )
    }
}

private data class CreateAccountFormState(
    val username: String,
    val password: String,
    val confirmPassword: String,
    val rememberMe: Boolean,
    val emailError: String?,
    val passwordError: String?,
    val confirmError: String?,
    val onUsernameChange: (String) -> Unit,
    val onPasswordChange: (String) -> Unit,
    val onConfirmPasswordChange: (String) -> Unit,
    val onRememberMeChange: (Boolean) -> Unit,
    val setEmailError: (String?) -> Unit,
    val setPasswordError: (String?) -> Unit,
    val setConfirmError: (String?) -> Unit
) {
    val isFormValid: Boolean
        get() = username.isNotBlank() &&
            isValidEmail(username) &&
            password.length == UiConstants.PASSWORD_LENGTH &&
            password.all { it.isDigit() } &&
            confirmPassword == password

    fun isValidEmail(value: String): Boolean {
        val at = value.indexOf('@')
        if (at <= 0 || at == value.lastIndex || at == value.length - 1) {
            return false
        }
        val domain = value.substring(at + 1)
        return '.' in domain
    }

    fun validateEmail(): Boolean {
        val ok = username.isNotBlank() && isValidEmail(username)
        setEmailError(
            when {
                username.isBlank() -> "Email is required"
                !isValidEmail(username) -> "Enter a valid email address"
                else -> null
            }
        )
        return ok
    }

    fun validatePassword(): Boolean {
        val ok = password.length == UiConstants.PASSWORD_LENGTH && password.all { it.isDigit() }
        setPasswordError(
            when {
                password.isEmpty() -> "Password is required"
                password.length != UiConstants.PASSWORD_LENGTH -> "Password must be exactly 6 digits"
                !password.all { it.isDigit() } -> "Password must contain only digits"
                else -> null
            }
        )
        return ok
    }

    fun validateConfirm(): Boolean {
        val ok = confirmPassword.isNotBlank() && confirmPassword == password
        setConfirmError(
            when {
                confirmPassword.isBlank() -> "Please confirm your password"
                confirmPassword != password -> "Passwords do not match"
                else -> null
            }
        )
        return ok
    }

    fun validateAll(): Boolean {
        val e = validateEmail()
        val p = validatePassword()
        val c = validateConfirm()
        return e && p && c
    }

    fun onEmailChange(v: String) {
        onUsernameChange(v)
        if (emailError != null) {
            validateEmail()
        }
    }

    fun onPasswordChange(v: String) {
        if (v.all { it.isDigit() } && v.length <= UiConstants.PASSWORD_LENGTH) {
            onPasswordChange.invoke(v)
            if (passwordError != null) {
                validatePassword()
            }
            if (confirmError != null) {
                validateConfirm()
            }
        }
    }

    fun onConfirmChange(v: String) {
        if (v.all { it.isDigit() } && v.length <= UiConstants.PASSWORD_LENGTH) {
            onConfirmPasswordChange(v)
            if (confirmError != null) {
                validateConfirm()
            }
        }
    }

    fun onRememberChange(v: Boolean) {
        onRememberMeChange(v)
    }
}

@Composable
private fun CreateAccountEmailField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    fg: Color,
    initialLabel: Color,
    accent: Color
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Email") },
        placeholder = { Text("Enter your email", color = initialLabel) },
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentType = ContentType.EmailAddress },
        colors = TextFieldDefaults.colors(
            focusedTextColor = accent,
            unfocusedTextColor = fg,
            focusedLabelColor = accent,
            unfocusedLabelColor = initialLabel,
            focusedPlaceholderColor = initialLabel.copy(alpha = UiConstants.LABEL_ALPHA),
            unfocusedPlaceholderColor = initialLabel,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = accent,
            unfocusedIndicatorColor = initialLabel
        ),
        isError = error != null,
        supportingText = {
            if (error != null) {
                Text(error, color = Color.Red)
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            capitalization = KeyboardCapitalization.None,
            autoCorrect = false
        )
    )
}

@Composable
private fun CreateAccountPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    fg: Color,
    initialLabel: Color,
    accent: Color
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("6-Digit Password") },
        placeholder = { Text("Enter 6-digit password", color = initialLabel) },
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedTextColor = accent,
            unfocusedTextColor = fg,
            focusedLabelColor = accent,
            unfocusedLabelColor = initialLabel,
            focusedPlaceholderColor = initialLabel.copy(alpha = UiConstants.LABEL_ALPHA),
            unfocusedPlaceholderColor = initialLabel,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = accent,
            unfocusedIndicatorColor = initialLabel
        ),
        isError = error != null,
        supportingText = {
            if (error != null) {
                Text(error, color = Color.Red)
            } else {
                Text(
                    "Enter your 6-digit password",
                    color = initialLabel.copy(alpha = UiConstants.LABEL_ALPHA)
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Next
        )
    )
}

@Composable
private fun CreateAccountConfirmPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    fg: Color,
    initialLabel: Color,
    accent: Color
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Confirm 6-Digit Password") },
        placeholder = { Text("Confirm 6-digit password", color = initialLabel) },
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedTextColor = accent,
            unfocusedTextColor = fg,
            focusedLabelColor = accent,
            unfocusedLabelColor = initialLabel,
            focusedPlaceholderColor = initialLabel.copy(alpha = UiConstants.LABEL_ALPHA),
            unfocusedPlaceholderColor = initialLabel,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = accent,
            unfocusedIndicatorColor = initialLabel
        ),
        isError = error != null,
        supportingText = {
            if (error != null) {
                Text(error, color = Color.Red)
            } else {
                Text(
                    "Confirm your 6-digit password",
                    color = initialLabel.copy(alpha = UiConstants.LABEL_ALPHA)
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done
        )
    )
}

@Composable
private fun CreateAccountButtons(
    formValid: Boolean,
    onContinueClick: () -> Unit,
    onBackClick: () -> Unit,
    fg: Color
) {
    Button(
        onClick = onContinueClick,
        enabled = formValid,
        colors = ButtonDefaults.buttonColors(
            containerColor = ColorConstants.BUTTON_PRIMARY_COLOR,
            contentColor = fg,
            disabledContainerColor = ColorConstants.BUTTON_PRIMARY_COLOR.copy(
                alpha = UiConstants.DISABLED_ALPHA
            ),
            disabledContentColor = fg.copy(alpha = UiConstants.DISABLED_CONTENT_ALPHA)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Continue")
    }

    Spacer(Modifier.height(UiConstants.PADDING_SMALL.dp))

    Button(
        onClick = onBackClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = fg.copy(alpha = UiConstants.TRANSPARENT_BG_ALPHA),
            contentColor = fg
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Back")
    }
}
