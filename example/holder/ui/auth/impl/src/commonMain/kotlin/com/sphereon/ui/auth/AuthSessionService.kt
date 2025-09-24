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

package com.sphereon.ui.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import com.sphereon.core.api.SureResult
import com.sphereon.core.api.asErrorResult
import com.sphereon.core.api.asOkResult
import com.sphereon.core.api.conf.DefaultTenantMapPropertySource
import com.sphereon.core.api.error.SureError
import com.sphereon.core.defaults.context.PrincipalInputString
import com.sphereon.core.defaults.context.TenantInputString
import com.sphereon.di.context.ISureContextComponent
import com.sphereon.di.context.ISureContextManager
import com.sphereon.di.session.ISureSessionComponent
import com.sphereon.ui.auth.settings.IUserPreferences

/**
 * Very simple auth service which uses the user preferences to authenticate the user. Not ready for production use!
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AuthSessionService(
    val contextManager: ISureContextManager,
    val userPreferences: IUserPreferences
) : IAuthSessionService {
    private val _contextComponentFlow: MutableStateFlow<ISureContextComponent?> = MutableStateFlow(null)
    private val _sessionComponentFlow: MutableStateFlow<ISureSessionComponent?> = MutableStateFlow(null)
    private val _authenticatedFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val authenticatedFlow: StateFlow<Boolean> = _authenticatedFlow.asStateFlow()
    override val contextComponentFlow: StateFlow<ISureContextComponent?> = _contextComponentFlow.asStateFlow()
    override val sessionComponentFlow: StateFlow<ISureSessionComponent?> = _sessionComponentFlow.asStateFlow()

    override fun authenticateAnonymous() {
        println("authenticateAnonymous CALLED")

        // Set the subscription key property during anonymous authentication too
        val subscriptionKey = userPreferences.kiwaSubscriptionKey
        DefaultTenantMapPropertySource.addProperty("kiwa.subscription.key.acc", subscriptionKey)

        val context = contextManager.initAnonymous()
        _contextComponentFlow.value = context
        val session = context.sessionContextManager.initAnonymousSession()
        _sessionComponentFlow.value = session
        _authenticatedFlow.value = false
        println("authenticateAnonymous DONE: $context, $session")
    }

    override fun authenticateWithUsernameAndPassword(
        username: String?,
        password: String?,
        remember: Boolean?
    ): SureResult<Pair<ISureContextComponent, ISureSessionComponent>, SureError> {
        println("authenticateWithUsernameAndPassword CALLED: $username, $password, $remember")
        logout() // Just make sure to clear any sessions
        val authError = SureError.NOT_FOUND_ERROR(message = "password incorrect").asErrorResult()

        // Use stored username if none provided (for 6-digit password login)
        val actualUsername = username ?: userPreferences.username

        if (actualUsername.isEmpty()) {
            return SureError.NOT_FOUND_ERROR(message = "No user account found").asErrorResult()
        }

        if (password.isNullOrEmpty()) {
            if (remember != true) return authError
            if (userPreferences.username != actualUsername.lowercase().trim()) return authError
        } else {
            // Validate 6-digit password format
            if (password.length != 6 || !password.all { it.isDigit() }) {
                return SureError.NOT_FOUND_ERROR(message = "Password must be exactly 6 digits").asErrorResult()
            }
            if (!userPreferences.checkCredentials(actualUsername, password)) return authError
        }

        // Set the subscription key property during login
        val subscriptionKey = userPreferences.kiwaSubscriptionKey
        DefaultTenantMapPropertySource.addProperty("kiwa.subscription.key.acc", subscriptionKey)

        val context = contextManager.initFromTenantAndPrincipal(
            TenantInputString(actualUsername),
            PrincipalInputString(actualUsername)
        )
        _contextComponentFlow.value = context
        val session = context.sessionContextManager.initSession(actualUsername)
        _sessionComponentFlow.value = session
        println("authenticateWithUsernameAndPassword LOGGED IN: $context, $session")
        _authenticatedFlow.value = true
        return (context to session).asOkResult()
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
        println("isAuthenticated: ${sessionComponentFlow.value?.sessionContext?.isAnonymous()}")
        return sessionComponentFlow.value != null && contextComponentFlow.value != null && sessionComponentFlow.value?.sessionContext?.isAnonymous() == false
    }

    @ContributesTo(AppScope::class)
    interface Component {
        val authSessionService: IAuthSessionService
    }
}
