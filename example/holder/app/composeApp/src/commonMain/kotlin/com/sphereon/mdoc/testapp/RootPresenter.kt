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

package com.sphereon.mdoc.testapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.presenter.molecule.backgesture.BackGestureDispatcherPresenter
import software.amazon.app.platform.presenter.molecule.backgesture.LocalBackGestureDispatcherPresenter
import software.amazon.app.platform.presenter.molecule.returningCompositionLocalProvider
import software.amazon.app.platform.presenter.template.toTemplate
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import com.sphereon.di.session.SureSessionScope
import com.sphereon.ui.core.appbar.BottomAppBarConfig
import com.sphereon.ui.core.appbar.IAppBarConfigModel
import com.sphereon.ui.core.appbar.TopAppBarConfig
import com.sphereon.ui.core.backstack.CrossSlideBackstackPresenter
import com.sphereon.ui.core.landing.ILandingPresenter

interface IRootPresenter : MoleculePresenter<Unit, TestAppTemplate>

@Inject
@ContributesBinding(SureSessionScope::class, boundType = IRootPresenter::class)
class RootPresenter(
    private val landingPresenter: ILandingPresenter,
    private val backGestureDispatcherPresenter: BackGestureDispatcherPresenter,
) : IRootPresenter {
    @Composable
    override fun present(input: Unit): TestAppTemplate {
        return returningCompositionLocalProvider(
            LocalBackGestureDispatcherPresenter provides backGestureDispatcherPresenter
        ) {
            val backstackPresenter = remember { CrossSlideBackstackPresenter(landingPresenter) }
            val backstackModel = backstackPresenter.present(Unit)

            backstackModelToTemplate(backstackModel)
        }
    }

    @Composable
    private fun backstackModelToTemplate(
        backstackModel: CrossSlideBackstackPresenter.Model
    ): TestAppTemplate {
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

            TestAppTemplate.FullScreenTemplate(
                model = model,
                topAppBarConfig = topAppBarConfig,
                bottomAppBarConfig = bottomAppBarConfig
            )
        }
    }
}
