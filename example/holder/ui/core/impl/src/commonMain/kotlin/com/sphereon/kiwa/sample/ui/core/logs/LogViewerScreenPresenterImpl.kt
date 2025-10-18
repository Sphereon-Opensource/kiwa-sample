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

package com.sphereon.kiwa.sample.ui.core.logs

import androidx.compose.runtime.Composable
import com.sphereon.di.session.SessionScope
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

/**
 * Implementation of the log viewer screen presenter.
 */
@Inject
@ContributesBinding(SessionScope::class, boundType = LogViewerScreenPresenter::class)
class LogViewerScreenPresenterImpl(
    private val logViewerPresenter: LogViewerPresenter
) : LogViewerScreenPresenter {

    @Composable
    override fun present(input: Unit): LogViewerPresenter.Model {
        return logViewerPresenter.present(input)
    }
}
