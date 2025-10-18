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

package com.sphereon.kiwa.sample.ui.auth

import com.sphereon.core.api.IdkResult
import com.sphereon.core.api.asErrorResult
import com.sphereon.core.api.asOkResult
import com.sphereon.core.api.conf.DefaultTenantMapPropertySource
import com.sphereon.core.api.error.IdkError
import com.sphereon.core.defaults.context.DefaultPrincipalInputString
import com.sphereon.core.defaults.context.DefaultTenantInputString
import com.sphereon.di.context.UserContextComponent
import com.sphereon.di.context.UserContextManager
import com.sphereon.di.session.SessionComponent
import com.sphereon.kiwa.sample.ui.auth.settings.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Very simple auth service which uses the user preferences to authenticate the user. Not ready for production use!
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AuthSessionServiceImpl(
    val userContextManager: UserContextManager,
    val userPreferences: UserPreferences
) : AuthSessionService {
    private val _contextComponentFlow: MutableStateFlow<UserContextComponent?> = MutableStateFlow(null)
    private val _sessionComponentFlow: MutableStateFlow<SessionComponent?> = MutableStateFlow(null)
    private val _authenticatedFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val authenticatedFlow: StateFlow<Boolean> = _authenticatedFlow.asStateFlow()
    override val contextComponentFlow: StateFlow<UserContextComponent?> = _contextComponentFlow.asStateFlow()
    override val sessionComponentFlow: StateFlow<SessionComponent?> = _sessionComponentFlow.asStateFlow()

    override fun authenticateAnonymous() {
        println("authenticateAnonymous CALLED")

        // Set the subscription key property during anonymous authentication too
        val subscriptionKey = userPreferences.kiwaSubscriptionKey
        DefaultTenantMapPropertySource.addProperty("kiwa.subscription.key", subscriptionKey)

        val context = userContextManager.getOrCreateAnonymous(makeActive = true)
        _contextComponentFlow.value = context.component
        val session = context.component.sessionContextManager.getOrCreateAnonymous(makeActive = true)
        _sessionComponentFlow.value = session.component
        _authenticatedFlow.value = false
        println("authenticateAnonymous DONE: $context, $session")
    }

    override fun authenticateWithUsernameAndPassword(
        username: String?,
        password: String?,
        remember: Boolean?
    ): IdkResult<Pair<UserContextComponent, SessionComponent>, IdkError> {
        println("authenticateWithUsernameAndPassword CALLED: $username, $password, $remember")
        logout() // Just make sure to clear any sessions

        // Use stored username if none provided (for 6-digit password login)
        val actualUsername = username ?: userPreferences.username

        // Validate username and password
        val validationError = validateCredentials(actualUsername, password, remember)
        if (validationError != null) {
            return validationError
        }

        // Set the subscription key property during login
        val subscriptionKey = userPreferences.kiwaSubscriptionKey
        DefaultTenantMapPropertySource.addProperty("kiwa.subscription.key.acc", subscriptionKey)

        val context = userContextManager.createOrGetFromInputs(
            DefaultTenantInputString(actualUsername),
            DefaultPrincipalInputString(actualUsername)
        )
        _contextComponentFlow.value = context
        val session = context.sessionContextManager.createOrGetFromId(actualUsername)
        _sessionComponentFlow.value = session
        println("authenticateWithUsernameAndPassword LOGGED IN: $context, $session")
        _authenticatedFlow.value = true
        return (context to session).asOkResult()
    }

    private fun validateCredentials(
        actualUsername: String,
        password: String?,
        remember: Boolean?
    ): IdkResult<Pair<UserContextComponent, SessionComponent>, IdkError>? {
        return when {
            actualUsername.isEmpty() -> {
                IdkError.NOT_FOUND_ERROR(message = "No user account found").asErrorResult()
            }
            password.isNullOrEmpty() -> {
                validateEmptyPassword(actualUsername, remember)
            }
            else -> {
                validateNonEmptyPassword(actualUsername, password)
            }
        }
    }

    private fun validateEmptyPassword(
        actualUsername: String,
        remember: Boolean?
    ): IdkResult<Pair<UserContextComponent, SessionComponent>, IdkError>? {
        val authError = IdkError.NOT_FOUND_ERROR(message = "password incorrect").asErrorResult()

        return if (remember != true || userPreferences.username != actualUsername.lowercase().trim()) {
            authError
        } else {
            null
        }
    }

    private fun validateNonEmptyPassword(
        actualUsername: String,
        password: String
    ): IdkResult<Pair<UserContextComponent, SessionComponent>, IdkError>? {
        val authError = IdkError.NOT_FOUND_ERROR(message = "password incorrect").asErrorResult()

        return when {
            password.length != PASSWORD_LENGTH || !password.all { it.isDigit() } -> {
                IdkError.NOT_FOUND_ERROR(
                    message = "Password must be exactly $PASSWORD_LENGTH digits"
                ).asErrorResult()
            }
            !userPreferences.checkCredentials(actualUsername, password) -> {
                authError
            }
            else -> null
        }
    }

    override fun logout() {
        userPreferences.setRememberMe(false)
        _contextComponentFlow.value = null
        _sessionComponentFlow.value = null
        _authenticatedFlow.value = false
    }

    override fun deleteAccount() {
        userPreferences.clearAll()
        logout()
    }

    override fun isAuthenticated(): Boolean {
        val sessionComponent = sessionComponentFlow.value
        val contextComponent = contextComponentFlow.value
        val isAnonymous = sessionComponent?.sessionContext?.isAnonymous()

        println("isAuthenticated DEBUG:")
        println("  sessionComponent != null: ${sessionComponent != null}")
        println("  contextComponent != null: ${contextComponent != null}")
        println("  isAnonymous: $isAnonymous")
        println("  isAnonymous == false: ${isAnonymous == false}")

        val result = sessionComponent != null &&
                contextComponent != null &&
                isAnonymous == false

        println("  final result: $result")

        return result
    }

    @ContributesTo(AppScope::class)
    interface Component {
        val authSessionService: AuthSessionService
    }

    private companion object {
        const val PASSWORD_LENGTH = 6
    }
}
