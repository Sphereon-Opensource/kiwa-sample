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

package com.sphereon.kiwa.sample.ui.auth

import com.sphereon.core.api.IdkResult
import com.sphereon.core.api.asErrorResult
import com.sphereon.core.api.asOkResult
import com.sphereon.core.api.conf.DefaultTenantMapPropertySource
import com.sphereon.core.api.error.IdkError
import com.sphereon.core.defaults.context.DefaultPrincipalInputString
import com.sphereon.core.defaults.context.DefaultTenantInputString
import com.sphereon.di.context.UserContextComponent
import com.sphereon.di.context.UserContextInstance
import com.sphereon.di.context.UserContextManager
import com.sphereon.di.session.SessionComponent
import com.sphereon.di.session.SessionInstance
import com.sphereon.kiwa.sample.ui.auth.settings.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Authentication service that uses UserContextManager and SessionContextManager.
 *
 * Since UserContextManager doesn't expose flows, this service maintains flows.
 * Flows are always non-null because anonymous instances are returned when not authenticated.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AuthSessionServiceImpl(
    val userContextManager: UserContextManager,
    val userPreferences: UserPreferences
) : AuthSessionService {
    private val _contextInstanceFlow: MutableStateFlow<UserContextInstance> = MutableStateFlow(
        userContextManager.getAnonymous(makeActive = true)
    )
    private val _sessionInstanceFlow: MutableStateFlow<SessionInstance> = MutableStateFlow(
        _contextInstanceFlow.value.component.sessionContextManager.getAnonymous(makeActive = true)
    )

    override val contextInstanceFlow: StateFlow<UserContextInstance> = _contextInstanceFlow.asStateFlow()
    override val sessionInstanceFlow: StateFlow<SessionInstance> = _sessionInstanceFlow.asStateFlow()

    override val authenticatedFlow: StateFlow<Boolean> = _sessionInstanceFlow.map { sessionInstance ->
        sessionInstance.component.sessionContext.isAnonymous() == false
    }.stateIn(
        scope = CoroutineScope(Dispatchers.Default),
        started = SharingStarted.Eagerly,
        initialValue = _sessionInstanceFlow.value.component.sessionContext.isAnonymous() == false
    )

    override fun authenticateAnonymous() {
        _sessionInstanceFlow.value.sessionContextManager.destroyAll()
        _contextInstanceFlow.value.userContextManager.destroyAll()

        println("authenticateAnonymous CALLED")

        val subscriptionKey = userPreferences.kiwaSubscriptionKey
        DefaultTenantMapPropertySource.addProperty("kiwa.subscription.key", subscriptionKey)

        // Use manager with makeActive=true to set as active context
        val context = userContextManager.getAnonymous(makeActive = true)
        _contextInstanceFlow.value = context

        // Use session manager with makeActive=true to set as active session
        val session = context.component.sessionContextManager.getAnonymous(makeActive = true)
        _sessionInstanceFlow.value = session

        println("authenticateAnonymous DONE")
    }

    override fun authenticateWithUsernameAndPassword(
        username: String?,
        password: String?,
        remember: Boolean?
    ): IdkResult<Pair<UserContextComponent, SessionComponent>, IdkError> {
        println("authenticateWithUsernameAndPassword CALLED: $username")
        logout()

        val actualUsername = username ?: userPreferences.username

        val validationError = validateCredentials(actualUsername, password, remember)
        if (validationError != null) {
            return validationError
        }

        val subscriptionKey = userPreferences.kiwaSubscriptionKey
        DefaultTenantMapPropertySource.addProperty("kiwa.subscription.key.acc", subscriptionKey)

        // Use manager with makeActive=true to set as active context
        val contextInstance = userContextManager.createOrGetFromInputs(
            DefaultTenantInputString(actualUsername),
            DefaultPrincipalInputString(actualUsername),
            makeActive = true
        )
        _contextInstanceFlow.value = contextInstance

        // Use session manager with makeActive=true to set as active session
        val sessionInstance = contextInstance.component.sessionContextManager.createOrGetFromId(
            actualUsername,
            makeActive = true
        )
        _sessionInstanceFlow.value = sessionInstance

        println("authenticateWithUsernameAndPassword DONE")
        return (contextInstance.component to sessionInstance.component).asOkResult()
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
        return if (remember != true || userPreferences.username != actualUsername.lowercase().trim()) {
            IdkError.NOT_FOUND_ERROR(message = "password incorrect").asErrorResult()
        } else {
            null
        }
    }

    private fun validateNonEmptyPassword(
        actualUsername: String,
        password: String
    ): IdkResult<Pair<UserContextComponent, SessionComponent>, IdkError>? {
        return when {
            password.length != PASSWORD_LENGTH || !password.all { it.isDigit() } -> {
                IdkError.NOT_FOUND_ERROR(
                    message = "Password must be exactly $PASSWORD_LENGTH digits"
                ).asErrorResult()
            }
            !userPreferences.checkCredentials(actualUsername, password) -> {
                IdkError.NOT_FOUND_ERROR(message = "password incorrect").asErrorResult()
            }
            else -> null
        }
    }

    override fun logout() {
        println("logout CALLED")
        userPreferences.setRememberMe(false)

        // Switch to anonymous - this will make anonymous active
        authenticateAnonymous()

        println("logout DONE")
    }

    override fun deleteAccount() {
        userPreferences.clearAll()
        logout()
    }

    override fun isAuthenticated(): Boolean {
        // Check if the active session is not anonymous
        return _sessionInstanceFlow.value.component.sessionContext.isAnonymous() == false
    }

    @ContributesTo(AppScope::class)
    interface Component {
        val authSessionService: AuthSessionService
    }

    private companion object {
        const val PASSWORD_LENGTH = 6
    }
}
