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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import com.sphereon.ui.auth.IAuthSessionService
import com.sphereon.ui.auth.settings.IUserPreferences

@Inject
@ContributesBinding(AppScope::class)
class AuthPresenter(
    val userPrefs: IUserPreferences,
    val authService: IAuthSessionService
) : IAuthPresenter {

    @Composable
    override fun present(input: Any): IAuthPresenter.Model {
        val storedUsername = userPrefs.username
        val storedRemember = userPrefs.rememberMe
        val onboarded = userPrefs.isOnboarded

        var remember by remember { mutableStateOf(storedRemember) }
        var username by remember { mutableStateOf(storedUsername) }
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf<String?>("") }

        // Helper function to validate 6-digit password
        fun isValid6DigitPassword(pwd: String): Boolean {
            return pwd.length == 6 && pwd.all { it.isDigit() }
        }

        // Obviously this is non production and just for testing purposes. Any password stored suffices
        password.isNotEmpty() && remember

        var screen by remember { mutableStateOf(Screen.CheckSavedPassword) }

        fun checkCredentials(): Boolean = userPrefs.checkCredentials(username, password)

        fun computeInitial() {
            screen = if (remember && onboarded && authService.authenticateWithUsernameAndPassword(username, password, remember).isOk) {
                // Authentication successful - go to LoggedIn state (this properly updates auth state)
                Screen.LoggedIn
            } else if (!onboarded) {
                Screen.CreateAccount
            } else {
                Screen.Login
            }
        }

        if (screen == Screen.CheckSavedPassword) {
            computeInitial()
        }

        val onEvent: (IAuthPresenter.Event) -> Unit = { event ->
            when (event) {
                is IAuthPresenter.Event.OnLoginClicked -> {
                    password = event.password
                    remember = event.remember

                    // Validate 6-digit password and authenticate if valid
                    if (isValid6DigitPassword(password)) {
                        // Use stored username from preferences for authentication
                        val usernameForAuth = if (username.isNotEmpty()) username else userPrefs.username
                        if (authService.authenticateWithUsernameAndPassword(usernameForAuth, password, remember).isOk) {
                            userPrefs.setRememberMe(remember)
                            screen = Screen.LoggedIn
                        }
                    }
                    // If validation fails, we just stay on the current screen
                    // The UI will show validation errors
                }

                is IAuthPresenter.Event.OnCreateAccountClicked -> {
                    username = event.username
                    password = event.password
                    confirmPassword = event.confirmPassword
                    remember = event.remember

                    // Validate 6-digit password for registration
                    if (username.isNotEmpty() && isValid6DigitPassword(password) && password == confirmPassword) {
                        // Persist new user data
                        userPrefs.setUsername(username)
                        userPrefs.setPassword(password)
                        userPrefs.setRememberMe(remember)

                        // Immediately authenticate so the rest of the app (Landing/Root) observes the session and navigates
                        if (authService.authenticateWithUsernameAndPassword(username, password, remember).isOk) {
                            screen = Screen.LoggedIn
                        }
                    }
                }

                is IAuthPresenter.Event.Logout -> screen = Screen.Login

                IAuthPresenter.Event.OnBackFromCreate -> screen = Screen.Login

                is IAuthPresenter.Event.DeleteAccount -> {
                    authService.deleteAccount()
                    // After deleting account, go to create account screen
                    screen = Screen.CreateAccount
                }
            }
        }

        return when (screen) {
            Screen.CheckSavedPassword -> IAuthPresenter.Model.CheckSavedPassword(onEvent)
            Screen.Login -> IAuthPresenter.Model.Login(password, remember, onEvent)
            Screen.CreateAccount -> IAuthPresenter.Model.CreateAccount(
                username,
                password,
                confirmPassword ?: "",
                true,
                onEvent
            )

            Screen.LoggedIn -> {
                IAuthPresenter.Model.LoggedIn(username, onEvent)
            }
        }
    }

    private enum class Screen { CheckSavedPassword, Login, CreateAccount, LoggedIn }
}
