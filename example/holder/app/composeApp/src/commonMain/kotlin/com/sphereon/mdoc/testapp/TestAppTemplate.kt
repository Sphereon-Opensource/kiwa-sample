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

import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.template.Template
import com.sphereon.ui.auth.login.IAuthPresenter
import com.sphereon.ui.core.appbar.BottomAppBarConfig
import com.sphereon.ui.core.appbar.TopAppBarConfig
import com.sphereon.ui.elicense.engagement.qr.IMdocEngagementQrPresenter

/** All [software.amazon.app.platform.presenter.template.Template]s implemented in the sample application. */
sealed interface TestAppTemplate : Template {
    /** A template that hosts a single model, which should rendered as full-screen element. */
    data class FullScreenMdocEngagementQrTemplate(
        /** The model to be rendered fullscreen. */
        val model: IMdocEngagementQrPresenter.Model,
    ) : TestAppTemplate

    data class AuthenticationTemplate(
        val model: IAuthPresenter.Model,
    ) : TestAppTemplate

    /** A template that hosts a single model, which should rendered as full-screen element. */
    data class FullScreenTemplate(
        /** The model to be rendered fullscreen. */
        val model: BaseModel,
        /** The configuration for the app bar of the test app. */
        val topAppBarConfig: TopAppBarConfig? = null,
        val bottomAppBarConfig: BottomAppBarConfig? = null,
    ) : TestAppTemplate
}
