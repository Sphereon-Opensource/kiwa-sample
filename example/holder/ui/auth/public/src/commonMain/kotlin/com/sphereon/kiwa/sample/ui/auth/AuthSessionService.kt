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
import com.sphereon.core.api.error.IdkError
import com.sphereon.di.context.UserContextComponent
import com.sphereon.di.context.UserContextInstance
import com.sphereon.di.session.SessionComponent
import com.sphereon.di.session.SessionInstance
import kotlinx.coroutines.flow.StateFlow

interface AuthSessionService {

    val sessionInstanceFlow: StateFlow<SessionInstance>
    val contextInstanceFlow: StateFlow<UserContextInstance>

    val authenticatedFlow: StateFlow<Boolean>
    fun isAuthenticated(): Boolean

    fun logout()
    fun deleteAccount()
    fun authenticateWithUsernameAndPassword(
        username: String?,
        password: String?,
        remember: Boolean? = false
    ): IdkResult<Pair<UserContextComponent, SessionComponent>, IdkError>

    fun authenticateAnonymous()
}
