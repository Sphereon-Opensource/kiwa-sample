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

package com.sphereon.ui.elicense.assignment

import androidx.compose.runtime.Composable
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import com.sphereon.di.session.SureSessionScope
import com.sphereon.ui.core.backstack.LocalBackstackScope

@Inject
@ContributesBinding(SureSessionScope::class, boundType = IElicenseAssignmentScreenPresenter::class)
class ElicenseAssignmentScreenPresenter(
    private val delegate: IElicenseAssignmentPresenter
) : IElicenseAssignmentScreenPresenter {

    @Composable
    override fun present(input: Any): IElicenseAssignmentPresenter.Model {
        val backstack = checkNotNull(LocalBackstackScope.current)

        return delegate.present(
            IElicenseAssignmentPresenter.Input(
                onAssignmentComplete = { result ->
                    // On successful assignment, go back to credential list
                    // The storage flow will automatically update the list with the new license
                    backstack.pop()
                },
                onCancel = {
                    // User cancelled, go back to credential list
                    backstack.pop()
                }
            )
        )
    }

    /*  @ContributesTo(SureSessionScope::class)
      interface Component {
          val elicenseAssignmentScreenPresenter: ElicenseAssignmentScreenPresenter
      }*/
}
