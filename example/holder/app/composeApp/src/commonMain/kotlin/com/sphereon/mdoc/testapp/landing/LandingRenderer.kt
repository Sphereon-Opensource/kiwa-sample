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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.renderer.ComposeRenderer
import com.sphereon.ui.auth.AuthSessionService
import com.sphereon.ui.core.landing.ILandingPresenter

/** Renders the content for [ILandingPresenter] on screen. */
@Inject
@ContributesRenderer
class LandingRenderer(val authSessionService: AuthSessionService) : ComposeRenderer<ILandingPresenter.Model>() {
    @Composable
    override fun Compose(model: ILandingPresenter.Model) {
        val authenticated = authSessionService.authenticatedFlow.collectAsState().value

        // Drive navigation solely from a side-effect to avoid re-entrant navigation during composition
        LaunchedEffect(key1 = authenticated) {
            if (authenticated) {
                model.onEvent(ILandingPresenter.Event.NavigateToMain)
            } else {
                model.onEvent(ILandingPresenter.Event.AuthPresenter)
            }
        }

        // Render nothing - this screen should never be visible; navigation happens immediately via effect
    }
}
