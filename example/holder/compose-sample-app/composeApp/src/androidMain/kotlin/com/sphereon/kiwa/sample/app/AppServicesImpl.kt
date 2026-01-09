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

/* * © 2026 Sphereon International B.V. *
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

import com.sphereon.core.api.log.LogService
import com.sphereon.di.context.UserContextManager
import com.sphereon.kiwa.sample.ui.auth.AuthSessionService
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.renderer.ComposeAndroidRendererFactory
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Implementation of [AppServices] that delegates to [KiwaSampleApplication].
 *
 * This class is automatically bound as a singleton in the AppScope and provides
 * dependency injection access to all application-wide services. It acts as a bridge
 * between the Android Application class and the DI framework.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AppServicesImpl(
    override val application: KiwaSampleApplication
) : AppServices {

    override val nfcEngagementNavigationService: NfcEngagementNavigationService
        get() = application.nfcEngagementNavigationService


    override val authSessionService: AuthSessionService
        get() = application.authSessionService

    override val rendererFactory: ComposeAndroidRendererFactory
        get() = application.rendererFactory

    override val log: LogService
        get() = application.log

    override val userContextManager: UserContextManager
        get() = application.appComponent.userContextManager
}
