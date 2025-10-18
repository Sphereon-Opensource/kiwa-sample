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
import com.sphereon.kiwa.sample.ui.auth.AuthSessionServiceImpl
import com.sphereon.kiwa.sample.ui.core.landing.LandingPresenter
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.renderer.ComposeRenderer

/** Renders the content for [LandingPresenter] on screen. */
@Inject
@ContributesRenderer
class LandingRenderer(val authSessionService: AuthSessionServiceImpl) : ComposeRenderer<LandingPresenter.Model>() {
    @Composable
    override fun Compose(model: LandingPresenter.Model) {
        // Render nothing - the LandingPresenter handles navigation based on auth state
        // This screen should never be visible; navigation happens in the presenter
    }
}
