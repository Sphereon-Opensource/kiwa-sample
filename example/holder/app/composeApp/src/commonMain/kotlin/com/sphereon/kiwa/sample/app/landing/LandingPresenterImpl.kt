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

package com.sphereon.kiwa.sample.app.landing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.sphereon.di.session.SessionScope
import com.sphereon.kiwa.sample.ui.auth.AuthSessionService
import com.sphereon.kiwa.sample.ui.auth.login.AuthPresenter
import com.sphereon.kiwa.sample.ui.card.CredentialListPresenter
import com.sphereon.kiwa.sample.ui.core.backstack.LocalBackstackScope
import com.sphereon.kiwa.sample.ui.core.backstack.presenter.BackstackChildPresenter
import com.sphereon.kiwa.sample.ui.core.landing.LandingPresenter
import com.sphereon.kiwa.sample.ui.elicense.engagement.qr.MdocEngagementPresenter
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

/** The presenter that is responsible to show the content of the landing page in the Recipes app. */
@Inject
@ContributesBinding(SessionScope::class, boundType = LandingPresenter::class)
class LandingPresenterImpl(
    val authSessionService: AuthSessionService,
    val authPresenter: AuthPresenter,
    val credentialListPresenter: CredentialListPresenter,
    val mdocEngagementQrPresenter: MdocEngagementPresenter,
) : LandingPresenter {
    @Composable
    override fun present(input: Any): LandingPresenter.Model {
        val backstack = checkNotNull(LocalBackstackScope.current)

        // Observe the session flow to trigger recomposition when auth state changes
        val authenticated = authSessionService.authenticatedFlow.collectAsState().value
        val backstackState = backstack.lastBackstackChange.value
        val backstackSize = backstackState.backstack.size

        // Use LaunchedEffect to handle navigation based on auth state changes
        // We need to observe both authenticated AND backstackSize to handle all cases
        LaunchedEffect(authenticated, backstackSize) {
            val currentSize = backstack.lastBackstackChange.value.backstack.size

            // When auth state changes to true and auth presenter is on top, navigate to credential list
            if (authenticated) {
                val currentBackstack = backstack.lastBackstackChange.value.backstack
                if (currentBackstack.lastOrNull() == authPresenter) {
                    backstack.pop()
                    backstack.push(credentialListPresenter)
                } else if (currentSize == 1) {
                    backstack.push(credentialListPresenter)
                }
            } else {
                // If not authenticated and we're at the root, push auth presenter
                if (currentSize == 1) {
                    backstack.push(authPresenter)
                }
            }
        }

        return LandingPresenter.Model(authenticated = authenticated, onEvent = { event ->
            when (event) {
                LandingPresenter.Event.AddPresenterToBackstack -> {
                    backstack.push(BackstackChildPresenter(0))
                }

                LandingPresenter.Event.AuthPresenter -> {
                    backstack.push(authPresenter)
                }

                LandingPresenter.Event.NavigateToMain -> {
                    // Check if auth presenter is on the stack and remove it
                    val currentBackstack = backstack.lastBackstackChange.value.backstack

                    if (currentBackstack.lastOrNull() == authPresenter) {
                        backstack.pop()
                    }

                    // Now push the credential list presenter
                    backstack.push(credentialListPresenter)
                }
            }
        })
    }
}
