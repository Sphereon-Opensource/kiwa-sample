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

import com.sphereon.core.api.log.AppLogManager
import com.sphereon.core.api.log.LogService
import com.sphereon.core.api.conf.DefaultPrincipalMapPropertySource
import com.sphereon.core.api.conf.DefaultTenantMapPropertySource
import com.sphereon.di.context.UserContextManager
import com.sphereon.kiwa.sample.ui.auth.AuthSessionService
import com.sphereon.kiwa.sample.ui.auth.AuthSessionServiceImpl
import me.tatarka.inject.annotations.Inject
import platform.UIKit.UIApplication
import software.amazon.app.platform.renderer.ComposeRendererFactory
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.experimental.ExperimentalObjCName

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
@OptIn(ExperimentalObjCName::class)
@ObjCName("AppServicesImpl")
class AppServicesImpl(
    override val application: UIApplication,
    appLogManager: AppLogManager
) : AppServices {

    lateinit var appComponent: IosAppComponent

    override val authSessionService: AuthSessionService
        get() = (appComponent as AuthSessionServiceImpl.Component).authSessionService

    override val rendererFactory: ComposeRendererFactory by lazy {
        ComposeRendererFactory(
            rootScopeProvider = appComponent.rootScopeProvider as software.amazon.app.platform.scope.RootScopeProvider
        )
    }


    override val log: LogService = appLogManager.withTag("test-app-main")

    override val userContextManager: UserContextManager
        get() = appComponent.userContextManager

    /**
     * Currently active template provider.
     * This is created and managed inside the Compose composition in MainViewController.
     */
    override var currentTemplateProvider: TemplateProvider? = null


    override fun onCreate(appComponent: IosAppComponent) {
        println("KiwaSampleApplication: === onCreate() START ===")

        // Store the app component reference for use by other properties
        this.appComponent = appComponent

        // Configure principal-level properties for KMS providers
        println("KiwaSampleApplication: Configuring principal-level properties")

        DefaultPrincipalMapPropertySource.addProperties(
            mapOf(
                // KMS provider configuration for Kiwa integration
                "sample.app.testing.kms.providers.kiwa.id" to "kiwa",
                "sample.app.testing.kms.providers.kiwa.type" to "software",
                "sample.app.testing.kms.providers.kiwa.autocreatecertificate" to "true",
                "sample.app.testing.kms.providers.kiwa.keystore.id" to "kiwa",
                "sample.app.testing.kms.providers.kiwa.keystore.type" to "apple",
                "sample.app.testing.kms.providers.kiwa.keystore.keyvisibility" to "public",
                "sample.app.testing.kms.providers.kiwa.keystore.overwritealias" to "true",
//               "sample.app.testing.kms.providers.kiwa.keystore.password" to "notused",
//               "sample.app.testing.kms.providers.kiwa.keystore.scopebinding" to "APP"
            )
        )

        // Configure tenant-level properties for API integration
        println("KiwaSampleApplication: Configuring tenant-level properties")
        DefaultTenantMapPropertySource.addProperties(
            mapOf(
                "kiwa.api.environment" to "acceptance",
                "kiwa.subscription.key.acceptance" to "d785f1024ad84484aeb37d1b13f57934",
            )
        )

        // Set up cryptographic verification services
        log.debug("KiwaSampleApplication: Setting up cryptographic verification services")
//        DefaultCallbacks.setX509Default(X509VerifyServiceAdapter())

        // Initialize logging and renderer factory
        log.debug("KiwaSampleApplication: Initializing logging and renderer factory")
        log.debug("KiwaSampleApplication: Renderer factory created")

        log.debug("KiwaSampleApplication: === onCreate() COMPLETE ===")
    }

}
