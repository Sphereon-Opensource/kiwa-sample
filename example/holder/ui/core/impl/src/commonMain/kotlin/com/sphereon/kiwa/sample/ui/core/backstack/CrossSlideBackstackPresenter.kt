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

package com.sphereon.kiwa.sample.ui.core.backstack

import androidx.compose.runtime.Composable
import com.sphereon.kiwa.sample.ui.core.appbar.BottomAppBarConfig
import com.sphereon.kiwa.sample.ui.core.appbar.IAppBarConfigModel
import com.sphereon.kiwa.sample.ui.core.appbar.TopAppBarConfig
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.presenter.molecule.backgesture.BackHandlerPresenter

/**
 * A generic presenter that wraps a presenter backstack to play a cross-fade animation whenever a
 * presenter is pushed to the stack or popped from the stack. A backstack always contains
 * [initialPresenter] as an element.
 */
class CrossSlideBackstackPresenter(
    private val initialPresenter: MoleculePresenter<Any, out BaseModel>
) : MoleculePresenter<Any, CrossSlideBackstackPresenter.Model> {
    @Composable
    override fun present(input: Any): Model {
        return presenterBackstack(initialPresenter) { model ->
            // Pop the top presenter on a back press event.
            BackHandlerPresenter(enabled = lastBackstackChange.value.backstack.size > 1) { pop() }

            Model(delegate = model, backstackScope = this)
        }
    }

    /**
     * The model containing all information about the backstack to play a cross-slide animation in the
     * renderer. [delegate] refers to the model of the top most presenter in the stack.
     * [backstackScope] contains the backstack and allows you to modify the stack.
     */
    data class Model(val delegate: BaseModel, val backstackScope: PresenterBackstackScope) :
        BaseModel, IAppBarConfigModel {
        override fun topAppBarConfig(): TopAppBarConfig? {
            return if (delegate is IAppBarConfigModel) {
                delegate.topAppBarConfig()
            } else {
                null
//        TopAppBarConfig.DEFAULT
            }
        }

        override fun bottomAppBarConfig(): BottomAppBarConfig? {
            return if (delegate is IAppBarConfigModel) {
                delegate.bottomAppBarConfig()
            } else {
                null
            }
        }
    }
}
