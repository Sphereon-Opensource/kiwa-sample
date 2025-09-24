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

package com.sphereon.mdoc.testapp.landing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import com.sphereon.di.session.SureSessionScope
import com.sphereon.ui.auth.IAuthSessionService
import com.sphereon.ui.auth.login.IAuthPresenter
import com.sphereon.ui.card.ICredentialListPresenter
import com.sphereon.ui.core.backstack.LocalBackstackScope
import com.sphereon.ui.core.backstack.presenter.BackstackChildPresenter
import com.sphereon.ui.core.landing.ILandingPresenter
import com.sphereon.ui.elicense.engagement.qr.IMdocEngagementQrPresenter

/** The presenter that is responsible to show the content of the landing page in the Recipes app. */
@Inject
@ContributesBinding(SureSessionScope::class, boundType = ILandingPresenter::class)
class LandingPresenter(
    val authSessionService: IAuthSessionService,
    val authPresenter: IAuthPresenter,
    val credentialListPresenter: ICredentialListPresenter,
    val mdocEngagementQrPresenter: IMdocEngagementQrPresenter,
) : ILandingPresenter {
    @Composable
    override fun present(input: Any): ILandingPresenter.Model {
        val backstack = checkNotNull(LocalBackstackScope.current)

        // Observe the session flow to trigger recomposition when auth state changes

        // Use the clean isAuthenticated() method for actual logic
        val authenticated = authSessionService.authenticatedFlow.collectAsState().value
        /* if (authenticated && backstack.lastBackstackChange.value != credentialListPresenter) {
             backstack.push(credentialListPresenter)
         }*/

        return ILandingPresenter.Model(authenticated = authenticated, onEvent = { event ->
            when (event) {
                ILandingPresenter.Event.AddPresenterToBackstack -> {
                    backstack.push(BackstackChildPresenter(0))
                }

                ILandingPresenter.Event.AuthPresenter -> {
                    backstack.push(authPresenter)
                }

                ILandingPresenter.Event.NavigateToMain -> {
                    // Pop all presenters except the initial one (LandingPresenter)
                    backstack.clear()

                    // Now push only the credential list presenter
                    backstack.push(credentialListPresenter)
                }
            }
        })
    }
}
