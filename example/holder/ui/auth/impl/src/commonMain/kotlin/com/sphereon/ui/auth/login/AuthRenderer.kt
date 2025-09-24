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

package com.sphereon.ui.auth.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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

@ContributesRenderer(modelType = IAuthPresenter.Model::class)
class AuthRenderer : ComposeRenderer<IAuthPresenter.Model>() {

    private val bg = Color(0xFF202537)
    private val fg = Color(0xFFFBFBFB)

    private val initialLabel = Color(0xCCFFFFFF) // More opaque white for better visibility
    private val accent = Color(0xFF0B81FF)

    @Composable
    override fun Compose(model: IAuthPresenter.Model) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(16.dp)
        ) {
            when (model) {
                is IAuthPresenter.Model.CheckSavedPassword -> {
                    Column(Modifier.align(Alignment.Center)) {
                        Text("Checking saved credentials…", color = fg)
                    }
                }

                is IAuthPresenter.Model.Login -> {
                    LoginContent(model, fg, initialLabel, accent)
                }

                is IAuthPresenter.Model.CreateAccount -> {
                    CreateAccountContent(model, fg = fg, bg = bg, initialLabel = initialLabel, accent = accent)
                }

                is IAuthPresenter.Model.LoggedIn -> {
                    Column(Modifier.align(Alignment.Center)) {
                        Text("Authentication successful! Loading...", color = fg)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginContent(
    model: IAuthPresenter.Model.Login,
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
        val isValid = password.value.length == 6 && password.value.all { it.isDigit() }
        passwordError.value = when {
            password.value.isEmpty() -> "Password is required"
            password.value.length != 6 -> "Password must be exactly 6 digits"
            !password.value.all { it.isDigit() } -> "Password must contain only digits"
            else -> null
        }
        return isValid
    }

    fun onPasswordChange(newPassword: String) {
        // Only allow digits and limit to 6 characters
        if (newPassword.all { it.isDigit() } && newPassword.length <= 6) {
            password.value = newPassword
            if (passwordError.value != null) validatePassword()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Sign in", color = fg, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = password.value,
            onValueChange = ::onPasswordChange,
            label = { Text("6-Digit Password") },
            placeholder = { Text("Enter 6-digit password", color = initialLabel) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedTextColor = accent,
                unfocusedTextColor = fg,
                focusedLabelColor = accent,
                unfocusedLabelColor = initialLabel,
                focusedPlaceholderColor = initialLabel.copy(alpha = 0.7f),
                unfocusedPlaceholderColor = initialLabel,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = accent,
                unfocusedIndicatorColor = initialLabel
            ),
            isError = passwordError.value != null,
            supportingText = {
                if (passwordError.value != null) {
                    Text(passwordError.value!!, color = Color.Red)
                } else {
                    Text("Enter your 6-digit password", color = initialLabel.copy(alpha = 0.7f))
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done)
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(
                checked = rememberMe.value,
                onCheckedChange = { rememberMe.value = it },
                colors = CheckboxDefaults.colors(checkedColor = accent),
            )
            Text("Remember password", color = fg)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (validatePassword()) {
                    model.onEvent(IAuthPresenter.Event.OnLoginClicked(password.value, rememberMe.value))
                }
            },
            enabled = password.value.length == 6 && password.value.all { it.isDigit() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF7276F7),
                contentColor = fg,
                disabledContainerColor = Color(0xFF7276F7).copy(alpha = 0.4f),
                disabledContentColor = fg.copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Continue") }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { showDeleteConfirmation.value = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = fg),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Delete Account") }
    }

    // Delete Account Confirmation Dialog
    if (showDeleteConfirmation.value) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation.value = false },
            title = { Text("Delete Account?") },
            text = { Text("This action cannot be undone. All your account data will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation.value = false
                        model.onEvent(IAuthPresenter.Event.DeleteAccount)
                        model.onEvent(IAuthPresenter.Event.OnBackFromCreate)
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation.value = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CreateAccountContent(
    model: IAuthPresenter.Model.CreateAccount,
    fg: Color,
    bg: Color,
    initialLabel: Color,
    accent: Color,
) {
    val username = remember { mutableStateOf(model.username) }
    val password = remember { mutableStateOf(model.password) }
    val confirmPassword = remember { mutableStateOf(model.confirmPassword) }
    val rememberMe = remember { mutableStateOf(model.remember) }
    val emailError = remember { mutableStateOf<String?>(null) }
    val passwordError = remember { mutableStateOf<String?>(null) }
    val confirmError = remember { mutableStateOf<String?>(null) }

    fun isValidEmail(value: String): Boolean {
        val at = value.indexOf('@')
        if (at <= 0 || at == value.lastIndex || at == value.length - 1) return false
        val domain = value.substring(at + 1)
        return '.' in domain
    }

    fun validateEmail(): Boolean {
        val ok = username.value.isNotBlank() && isValidEmail(username.value)
        emailError.value = when {
            username.value.isBlank() -> "Email is required"
            !isValidEmail(username.value) -> "Enter a valid email address"
            else -> null
        }
        return ok
    }

    fun validatePassword(): Boolean {
        val ok = password.value.length == 6 && password.value.all { it.isDigit() }
        passwordError.value = when {
            password.value.isEmpty() -> "Password is required"
            password.value.length != 6 -> "Password must be exactly 6 digits"
            !password.value.all { it.isDigit() } -> "Password must contain only digits"
            else -> null
        }
        return ok
    }

    fun validateConfirm(): Boolean {
        val ok = confirmPassword.value.isNotBlank() && confirmPassword.value == password.value
        confirmError.value = when {
            confirmPassword.value.isBlank() -> "Please confirm your password"
            confirmPassword.value != password.value -> "Passwords do not match"
            else -> null
        }
        return ok
    }

    fun validateAll(): Boolean {
        val e = validateEmail()
        val p = validatePassword()
        val c = validateConfirm()
        return e && p && c
    }

    fun onEmailChange(v: String) {
        username.value = v
        if (emailError.value != null) validateEmail()
    }

    fun onPasswordChange(v: String) {
        // Only allow digits and limit to 6 characters
        if (v.all { it.isDigit() } && v.length <= 6) {
            password.value = v
            if (passwordError.value != null) validatePassword()
            if (confirmError.value != null) validateConfirm()
        }
    }

    fun onConfirmChange(v: String) {
        // Only allow digits and limit to 6 characters
        if (v.all { it.isDigit() } && v.length <= 6) {
            confirmPassword.value = v
            if (confirmError.value != null) validateConfirm()
        }
    }

    val formValid = remember { mutableStateOf(false) }
    formValid.value = username.value.isNotBlank() &&
        isValidEmail(username.value) &&
        password.value.length == 6 &&
        password.value.all { it.isDigit() } &&
        confirmPassword.value == password.value

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create account", color = fg, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = username.value,
            onValueChange = ::onEmailChange,
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
                focusedPlaceholderColor = initialLabel.copy(alpha = 0.7f),
                unfocusedPlaceholderColor = initialLabel,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = accent,
                unfocusedIndicatorColor = initialLabel
            ),
            isError = emailError.value != null,
            supportingText = {
                if (emailError.value != null) Text(emailError.value!!, color = Color.Red)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                capitalization = KeyboardCapitalization.None,
                autoCorrect = false
            )
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = password.value,
            onValueChange = ::onPasswordChange,
            label = { Text("6-Digit Password") },
            placeholder = { Text("Enter 6-digit password", color = initialLabel) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedTextColor = accent,
                unfocusedTextColor = fg,
                focusedLabelColor = accent,
                unfocusedLabelColor = initialLabel,
                focusedPlaceholderColor = initialLabel.copy(alpha = 0.7f),
                unfocusedPlaceholderColor = initialLabel,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = accent,
                unfocusedIndicatorColor = initialLabel
            ),
            isError = passwordError.value != null,
            supportingText = {
                if (passwordError.value != null) {
                    Text(passwordError.value!!, color = Color.Red)
                } else {
                    Text("Enter your 6-digit password", color = initialLabel.copy(alpha = 0.7f))
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next)
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = confirmPassword.value,
            onValueChange = ::onConfirmChange,
            label = { Text("Confirm 6-Digit Password") },
            placeholder = { Text("Confirm 6-digit password", color = initialLabel) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedTextColor = accent,
                unfocusedTextColor = fg,
                focusedLabelColor = accent,
                unfocusedLabelColor = initialLabel,
                focusedPlaceholderColor = initialLabel.copy(alpha = 0.7f),
                unfocusedPlaceholderColor = initialLabel,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = accent,
                unfocusedIndicatorColor = initialLabel
            ),
            isError = confirmError.value != null,
            supportingText = {
                if (confirmError.value != null) {
                    Text(confirmError.value!!, color = Color.Red)
                } else {
                    Text("Confirm your 6-digit password", color = initialLabel.copy(alpha = 0.7f))
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done)
        )

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(
                checked = rememberMe.value,
                onCheckedChange = { rememberMe.value = it },
                colors = CheckboxDefaults.colors(checkedColor = accent),
            )
            Text("Remember password", color = fg)
        }

        Button(
            onClick = {
                if (validateAll()) {
                    model.onEvent(
                        IAuthPresenter.Event.OnCreateAccountClicked(
                            username = username.value,
                            password = password.value,
                            confirmPassword = confirmPassword.value,
                            remember = rememberMe.value
                        )
                    )
                }
            },
            enabled = formValid.value,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF7276F7),
                contentColor = fg,
                disabledContainerColor = Color(0xFF7276F7).copy(alpha = 0.4f),
                disabledContentColor = fg.copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Continue") }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { model.onEvent(IAuthPresenter.Event.OnBackFromCreate) },
            colors = ButtonDefaults.buttonColors(containerColor = fg.copy(alpha = 0.12f), contentColor = fg),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Back") }
    }
}
