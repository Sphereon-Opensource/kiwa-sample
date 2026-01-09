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
import platform.UIKit.UIApplication
import software.amazon.app.platform.renderer.ComposeRendererFactory
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import kotlin.experimental.ExperimentalObjCName

/**
 * Injectable singleton providing access to application-wide services.
 *
 * This class wraps the services from [KiwaSampleApplication] and makes them
 * available through dependency injection. It provides a clean separation between
 * the Android Application lifecycle and the dependency injection framework.
 *
 * All services except appComponent are exposed here for injection into other classes.
 *
 * Note: Context and session component flows are available through authSessionService.
 * Use authSessionService.contextComponentFlow and authSessionService.sessionComponentFlow
 * to access the current context and session components.
 */
@OptIn(ExperimentalObjCName::class)
@ObjCName("AppServices")
interface AppServices {
   /* *//** Service for managing NFC engagement navigation and coordination. *//*
    val nfcEngagementNavigationService: NfcEngagementNavigationService*/


    /**
     * Authentication session service for managing user authentication and sessions.
     *
     * This service provides:
     * - authSessionService.contextComponentFlow: Current user context component
     * - authSessionService.sessionComponentFlow: Current session component
     * - authSessionService.authenticatedFlow: Authentication state
     * - authSessionService.isAuthenticated(): Check if user is authenticated (not anonymous)
     */
    val authSessionService: AuthSessionService

    /**
     * Factory for creating Compose renderers for template-based UI rendering.
     */
    val rendererFactory: ComposeRendererFactory

    /**
     * Application-wide logging service for operation tracking and debugging.
     */
    val log: LogService

    val userContextManager: UserContextManager

    val application: UIApplication

    fun onCreate(appComponent: IosAppComponent)

    @ContributesTo(AppScope::class)
    @OptIn(ExperimentalObjCName::class)
    @ObjCName("AppServicesComponent")
    interface Component {
        val appServices: AppServices
    }

    var currentTemplateProvider: TemplateProvider?
}
