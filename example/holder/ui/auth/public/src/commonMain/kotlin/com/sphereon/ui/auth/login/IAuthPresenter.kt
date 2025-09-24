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

import androidx.compose.runtime.Immutable
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter

interface IAuthPresenter : MoleculePresenter<Any, IAuthPresenter.Model> {

    sealed interface Event {

        data object Logout : Event
        data class OnLoginClicked(val password: String, val remember: Boolean) : Event
        data class OnCreateAccountClicked(
            val username: String,
            val password: String,
            val confirmPassword: String,
            val remember: Boolean
        ) : Event

        data object OnBackFromCreate : Event
        data object DeleteAccount : Event
    }

    sealed interface Model : BaseModel {
        val onEvent: (Event) -> Unit

        @Immutable
        data class CheckSavedPassword(
            override val onEvent: (Event) -> Unit,
        ) : Model

        @Immutable
        data class Login(
            val password: String,
            val remember: Boolean,
            override val onEvent: (Event) -> Unit,
        ) : Model

        /* @Immutable
         data class Logout(
             override val onEvent: (Event) -> Unit
         ): Model*/

        @Immutable
        data class CreateAccount(
            val username: String,
            val password: String,
            val confirmPassword: String,
            val remember: Boolean = true,
            override val onEvent: (Event) -> Unit,
        ) : Model

        @Immutable
        data class LoggedIn(
            val email: String,
            override val onEvent: (Event) -> Unit,
        ) : Model
    }
}
