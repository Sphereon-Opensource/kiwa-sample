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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sphereon.kiwa.sample.ui.auth.AuthSessionService
import com.sphereon.kiwa.sample.ui.auth.settings.UserPreferences
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@Inject
@ContributesBinding(AppScope::class)
class AuthPresenterImpl(
    val userPrefs: UserPreferences,
    val authService: AuthSessionService
) : AuthPresenter {

    @Composable
    override fun present(input: Any): AuthPresenter.Model {
        val state = initializeState()

        println("AuthPresenter: present() called, screen=${state.screen}, onboarded=${state.onboarded}")

        val onEvent = createEventHandler(state)
        val model = createModel(state, onEvent)
        println("AuthPresenter: Returning model of type ${model::class.simpleName}")

        return model
    }

    @Composable
    private fun initializeState(): PresentState {
        val storedUsername = userPrefs.username
        val storedRemember = userPrefs.rememberMe
        val onboarded = userPrefs.isOnboarded

        var remember by remember { mutableStateOf(storedRemember) }
        var username by remember { mutableStateOf(storedUsername) }
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf<String?>("") }

        // Compute the initial screen directly instead of using CheckSavedPassword + computeInitial()
        // This avoids the issue where the captured screen value doesn't update after setScreen() is called
        val initialScreen = remember {
            println("AuthPresenter: Computing initial screen, onboarded=$onboarded, remember=$storedRemember")
            when {
                storedRemember && onboarded && authService.authenticateWithUsernameAndPassword(
                    storedUsername, "", storedRemember
                ).isOk -> {
                    println("AuthPresenter: Initial screen = Login (auto-auth)")
                    Screen.Login
                }
                !onboarded -> {
                    println("AuthPresenter: Initial screen = CreateAccount (not onboarded)")
                    Screen.CreateAccount
                }
                else -> {
                    println("AuthPresenter: Initial screen = Login (default)")
                    Screen.Login
                }
            }
        }
        var screen by remember { mutableStateOf(initialScreen) }

        return PresentState(
            remember = remember,
            username = username,
            password = password,
            confirmPassword = confirmPassword,
            screen = screen,
            onboarded = onboarded,
            authService = authService,
            setRemember = { remember = it },
            setUsername = { username = it },
            setPassword = { password = it },
            setConfirmPassword = { confirmPassword = it },
            setScreen = { screen = it }
        )
    }

    private fun createEventHandler(state: PresentState): (AuthPresenter.Event) -> Unit {
        return { event ->
            when (event) {
                is AuthPresenter.Event.OnLoginClicked -> {
                    handleLoginClicked(
                        event,
                        state.username,
                        state.setPassword,
                        state.setRemember
                    )
                }

                is AuthPresenter.Event.OnCreateAccountClicked -> {
                    handleCreateAccountClicked(
                        event,
                        state.setUsername,
                        state.setPassword,
                        state.setConfirmPassword,
                        state.setRemember
                    )
                }

                is AuthPresenter.Event.Logout -> state.setScreen(Screen.Login)

                is AuthPresenter.Event.OnBackFromCreate -> state.setScreen(Screen.Login)

                is AuthPresenter.Event.DeleteAccount -> {
                    handleDeleteAccount(
                        state.setUsername,
                        state.setPassword,
                        state.setConfirmPassword,
                        state.setRemember,
                        state.setScreen
                    )
                }
            }
        }
    }

    private fun createModel(state: PresentState, onEvent: (AuthPresenter.Event) -> Unit): AuthPresenter.Model {
        return when (state.screen) {
            Screen.CheckSavedPassword -> AuthPresenter.Model.CheckSavedPassword(onEvent)
            Screen.Login -> AuthPresenter.Model.Login(state.password, state.remember, onEvent)
            Screen.CreateAccount -> {
                AuthPresenter.Model.CreateAccount(
                    state.username,
                    state.password,
                    state.confirmPassword ?: "",
                    true,
                    onEvent
                )
            }
        }
    }

    private fun handleLoginClicked(
        event: AuthPresenter.Event.OnLoginClicked,
        username: String,
        setPassword: (String) -> Unit,
        setRemember: (Boolean) -> Unit
    ) {
        setPassword(event.password)
        setRemember(event.remember)

        // Validate 6-digit password and authenticate if valid
        if (isValid6DigitPassword(event.password)) {
            // Use stored username from preferences for authentication
            val usernameForAuth = username.ifEmpty { userPrefs.username }
            val result = authService.authenticateWithUsernameAndPassword(
                usernameForAuth,
                event.password,
                event.remember
            )

            if (result.isOk) {
                userPrefs.setRememberMe(event.remember)
            }
        }
    }

    private fun handleCreateAccountClicked(
        event: AuthPresenter.Event.OnCreateAccountClicked,
        setUsername: (String) -> Unit,
        setPassword: (String) -> Unit,
        setConfirmPassword: (String?) -> Unit,
        setRemember: (Boolean) -> Unit
    ) {
        setUsername(event.username)
        setPassword(event.password)
        setConfirmPassword(event.confirmPassword)
        setRemember(event.remember)

        // Validate 6-digit password for registration
        if (event.username.isNotEmpty() &&
            isValid6DigitPassword(event.password) &&
            event.password == event.confirmPassword
        ) {
            // Persist new user data
            userPrefs.setUsername(event.username)
            userPrefs.setPassword(event.password)
            userPrefs.setRememberMe(event.remember)

            // Authenticate
            authService.authenticateWithUsernameAndPassword(
                event.username,
                event.password,
                event.remember
            )
        }
    }

    private fun handleDeleteAccount(
        setUsername: (String) -> Unit,
        setPassword: (String) -> Unit,
        setConfirmPassword: (String?) -> Unit,
        setRemember: (Boolean) -> Unit,
        setScreen: (Screen) -> Unit
    ) {
        authService.deleteAccount()
        // Reset all state variables after deleting account
        setUsername("")
        setPassword("")
        setConfirmPassword("")
        setRemember(true)
        // Go to create account screen
        setScreen(Screen.CreateAccount)
    }

    private enum class Screen { CheckSavedPassword, Login, CreateAccount }

    private data class PresentState(
        val remember: Boolean,
        val username: String,
        val password: String,
        val confirmPassword: String?,
        val screen: Screen,
        val onboarded: Boolean,
        val authService: AuthSessionService,
        val setRemember: (Boolean) -> Unit,
        val setUsername: (String) -> Unit,
        val setPassword: (String) -> Unit,
        val setConfirmPassword: (String?) -> Unit,
        val setScreen: (Screen) -> Unit
    )

    private companion object {
        const val PASSWORD_LENGTH = 6

        // Helper function to validate 6-digit password
        fun isValid6DigitPassword(pwd: String): Boolean {
            return pwd.length == PASSWORD_LENGTH && pwd.all { it.isDigit() }
        }
    }
}
