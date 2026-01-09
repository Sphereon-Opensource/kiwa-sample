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

package com.sphereon.kiwa.sample.ui.elicense.assignment

import androidx.compose.runtime.Composable
import com.sphereon.di.session.SessionScope
import com.sphereon.kiwa.sample.ui.core.backstack.LocalBackstackScope
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@Inject
@ContributesBinding(SessionScope::class, boundType = ElicenseAssignmentScreenPresenter::class)
class ElicenseAssignmentScreenPresenterImpl(
    private val delegate: ElicenseAssignmentPresenter
) : ElicenseAssignmentScreenPresenter {

    @Composable
    override fun present(input: Any): ElicenseAssignmentPresenter.Model {
        val backstack = checkNotNull(LocalBackstackScope.current)

        return delegate.present(
            ElicenseAssignmentPresenter.Input(
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

    /*  @ContributesTo(SessionScope::class)
      interface Component {
          val elicenseAssignmentScreenPresenter: ElicenseAssignmentScreenPresenter
      }*/
}
