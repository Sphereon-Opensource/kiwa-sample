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

package com.sphereon.kiwa.sample.ui.card

import androidx.compose.runtime.Composable
import com.sphereon.di.session.SessionScope
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

/**
 * Implementation for the credential list item presenter. Straight mapping from Input.Row to Model.Row.
 */
@Inject
@ContributesBinding(SessionScope::class, boundType = CredentialListItemPresenter::class)
class CredentialListItemPresenterImpl : CredentialListItemPresenter {
    @Composable
    override fun present(input: CredentialListItemPresenter.Input): CredentialListItemPresenter.Model = when (input) {
        is CredentialListItemPresenter.Input.Row -> CredentialListItemPresenter.Model.Row(
            card = input.card.toModel(),
            document = input.document,
        )
    }
}
