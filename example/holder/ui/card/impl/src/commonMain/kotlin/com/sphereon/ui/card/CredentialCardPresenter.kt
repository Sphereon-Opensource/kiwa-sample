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

package com.sphereon.ui.card

import androidx.compose.runtime.Composable
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import com.sphereon.di.session.SureSessionScope

/** Simple presenter that maps the given unified [ICredentialCardPresenter.Input] to a [ICredentialCardPresenter.Model].
 *  Any future data normalization can be added here. */
@Inject
@ContributesBinding(SureSessionScope::class, boundType = ICredentialCardPresenter::class)
class CredentialCardPresenter : ICredentialCardPresenter {

    @Composable
    override fun present(input: ICredentialCardPresenter.Input): ICredentialCardPresenter.Model {
        // Use mapping helper for clarity and consistency // TODO, lookup images etc.
        return input.toModel()
    }
}
