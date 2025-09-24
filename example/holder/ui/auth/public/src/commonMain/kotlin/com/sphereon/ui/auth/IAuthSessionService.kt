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

import kotlinx.coroutines.flow.StateFlow
import com.sphereon.core.api.SureResult
import com.sphereon.core.api.error.SureError
import com.sphereon.di.context.ISureContextComponent
import com.sphereon.di.session.ISureSessionComponent

interface IAuthSessionService {

    val sessionComponentFlow: StateFlow<ISureSessionComponent?>
    val contextComponentFlow: StateFlow<ISureContextComponent?>

    val authenticatedFlow: StateFlow<Boolean>
    fun isAuthenticated(): Boolean

    fun logout()
    fun deleteAccount()
    fun authenticateWithUsernameAndPassword(
        username: String?,
        password: String?,
        remember: Boolean? = false
    ): SureResult<Pair<ISureContextComponent, ISureSessionComponent>, SureError>

    fun authenticateAnonymous()
}
