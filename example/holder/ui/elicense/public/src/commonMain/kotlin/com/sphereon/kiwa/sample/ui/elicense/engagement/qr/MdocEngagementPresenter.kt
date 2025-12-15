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

package com.sphereon.kiwa.sample.ui.elicense.engagement.qr

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import com.sphereon.kiwa.sample.ui.core.appbar.BottomAppBarConfig
import com.sphereon.kiwa.sample.ui.core.appbar.IAppBarConfigModel
import com.sphereon.kiwa.sample.ui.core.appbar.TopAppBarConfig
import com.sphereon.kiwa.sample.ui.elicense.engagement.consent.MdocInformationRequestPresenter
import com.sphereon.mdoc.engagement.MdocEngagementEvent
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter

/**
 * A presenter is the glue between our business logic and UI. A presenter injects
 * service objects, data repositories and other presenters to compute a model to
 * represent what should be shown to the user. Presenters are reactive. If its
 * internal state or state of injected dependencies change, then a new model is
 * emitted. Presenters are composable, meaning that one presenter can inject other
 * presenters and combine their emitted models to a single model. This enables to
 * implement model-driven navigation. By decoupling presenters from UI and Android
 * components like Activities, Fragments and ViewModels we make them easier to test.
 * Business logic and UI integration can evolve independently.
 */

interface MdocEngagementPresenter : MoleculePresenter<MdocEngagementPresenter.Input, MdocEngagementPresenter.Model> {

    @Immutable
    object Input

    sealed interface Model : BaseModel, IAppBarConfigModel {
        val onStateEvent: (event: UiStateEvent) -> Unit

        override fun topAppBarConfig(): TopAppBarConfig {
            return TopAppBarConfig(title = "Attended presentation")
        }

        override fun bottomAppBarConfig(): BottomAppBarConfig? {
            return BottomAppBarConfig(show = true)
        }

        @Immutable
        data class Initial(
            val showQr: Boolean = false,
            val showQrScanner: Boolean = false,
            val onQrScanned: (String) -> Unit = {},
            override val onStateEvent: (event: UiStateEvent) -> Unit
        ) : Model

        @Immutable
        data class Engagement(
            val qrImage: ImageBitmap? = null, // If filled, a QR code would be shown
            val engagementEvent: MdocEngagementEvent? = null,
            val showQr: Boolean = false,
            val showQrScanner: Boolean = false,
            val onQrScanned: (String) -> Unit = {},

            // Callback option from UI. Current presenter is just a test code presenter, combining buttons with QR display etc.
            override val onStateEvent: (event: UiStateEvent) -> Unit
        ) : Model

        @Immutable
        data class Selecting(
            val consentModel:
            MdocInformationRequestPresenter.Model,
            override val onStateEvent: (event: UiStateEvent) -> Unit
        ) : Model

        @Immutable
        data class Connecting(
            val engagementEvent: MdocEngagementEvent? = null,
            override val onStateEvent: (event: UiStateEvent) -> Unit
        ) : Model

        @Immutable
        data class Stopped(
            val engagementEvent: MdocEngagementEvent? = null,
            override val onStateEvent: (event: UiStateEvent) -> Unit
        ) : Model

        @Immutable
        data class Sharing(
            override val onStateEvent: (event: UiStateEvent) -> Unit
        ) : Model

        @Immutable
        data class Success(override val onStateEvent: (event: UiStateEvent) -> Unit) : Model
    }

    sealed interface UiStateEvent {
        /*data object Initial : UiStateEvent
        data object Engagement : UiStateEvent // Kickof a new engagement. So Initial -> Engagement
        data object Select : UiStateEvent // Navigate to the Information Request selection screen

        data object Connecting : UiStateEvent
        data object Sharing : UiStateEvent // Processing and sending mdoc device response*/
        data object Stopped : UiStateEvent // Stop existing engagement, moving from Engagement -> Initial
        data object ShowQr : UiStateEvent // Switch UI from NFC icon to QR code
        data object ShowQrScanner : UiStateEvent // Switch UI to QR scanner mode for reverse engagement
//        data object Success : UiStateEvent // Successful share - show toast and navigate back
        data object SuccessComplete : UiStateEvent // Success toast completed - navigate back to credential list
    }
}
