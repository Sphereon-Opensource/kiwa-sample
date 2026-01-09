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

package com.sphereon.kiwa.sample.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.sphereon.di.session.SessionScope
import com.sphereon.kiwa.sample.ui.auth.AuthSessionService
import com.sphereon.kiwa.sample.ui.auth.login.AuthPresenter
import com.sphereon.kiwa.sample.ui.card.CredentialListPresenter
import com.sphereon.kiwa.sample.ui.core.appbar.BottomAppBarConfig
import com.sphereon.kiwa.sample.ui.core.appbar.IAppBarConfigModel
import com.sphereon.kiwa.sample.ui.core.appbar.TopAppBarConfig
import com.sphereon.kiwa.sample.ui.core.backstack.CrossSlideBackstackPresenter
import com.sphereon.kiwa.sample.ui.core.landing.LandingPresenter
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.presenter.molecule.backgesture.BackGestureDispatcherPresenter
import software.amazon.app.platform.presenter.molecule.backgesture.LocalBackGestureDispatcherPresenter
import software.amazon.app.platform.presenter.molecule.returningCompositionLocalProvider
import software.amazon.app.platform.presenter.template.toTemplate
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

interface MoleculeRootPresenter : MoleculePresenter<Unit, SampleAppTemplate>

@Inject
@ContributesBinding(SessionScope::class, boundType = MoleculeRootPresenter::class)
class RootPresenterImpl(
    private val landingPresenter: LandingPresenter,
    private val backGestureDispatcherPresenter: BackGestureDispatcherPresenter,
    private val authSessionService: AuthSessionService,
    private val credentialListPresenter: CredentialListPresenter,
    private val backstackRegistry: NfcBackstackRegistry,
) : MoleculeRootPresenter {
    @Composable
    override fun present(input: Unit): SampleAppTemplate {
        println("RootPresenter: present() method called")
        return returningCompositionLocalProvider(
            LocalBackGestureDispatcherPresenter provides backGestureDispatcherPresenter
        ) {
            val backstackPresenter = remember { CrossSlideBackstackPresenter(landingPresenter) }
            val backstackModel = backstackPresenter.present(Unit)
            val backstackScope = backstackModel.backstackScope

            // Observe authentication state 
            val authenticated = authSessionService.authenticatedFlow.collectAsState().value
            println("RootPresenter: Authentication state = $authenticated")

            // Handle navigation when auth state changes
            LaunchedEffect(authenticated) {
                println("RootPresenter: LaunchedEffect triggered, authenticated = $authenticated")
                val currentTop = backstackScope.lastBackstackChange.value.backstack.lastOrNull()
                if (authenticated && currentTop is AuthPresenter) {
                    backstackScope.pop() // Remove auth presenter
                    backstackScope.push(credentialListPresenter) // Add credential list
                }
            }

            // Set up the navigation trigger with the current backstack scope
            // This allows background NFC services to push presenters directly to the backstack
            // Use SideEffect to set the scope on every successful composition
            // Don't clear on dispose - the registry is a singleton that gets overwritten
            SideEffect {
                println("RootPresenter: Setting backstack scope on navigation trigger: $backstackRegistry")
                backstackRegistry.setBackstackScope(backstackScope)
            }

            backstackModelToTemplate(backstackModel)
        }
    }

    @Composable
    private fun backstackModelToTemplate(
        backstackModel: CrossSlideBackstackPresenter.Model
    ): SampleAppTemplate {
        val backstackScope = backstackModel.backstackScope
        val showBackArrow = backstackScope.lastBackstackChange.value.backstack.size > 2

        val backArrowAction =
            if (showBackArrow) {
                { backstackScope.pop() }
            } else {
                null
            }

        return backstackModel.toTemplate { model ->
            val topAppBarConfig =
                if (model is IAppBarConfigModel) {
                    val delegate = model.topAppBarConfig()
                    delegate?.copy(backArrowAction = backArrowAction)
                } else {
                    TopAppBarConfig(title = TopAppBarConfig.DEFAULT.title, backArrowAction = backArrowAction)
                }
            val bottomAppBarConfig =
                if (model is IAppBarConfigModel) {
                    model.bottomAppBarConfig()
                } else {
                    BottomAppBarConfig()
                }

            SampleAppTemplate.MainScreenWithNavbarsTemplate(
                model = model,
                topAppBarConfig = topAppBarConfig,
                bottomAppBarConfig = bottomAppBarConfig
            )
        }
    }
}
