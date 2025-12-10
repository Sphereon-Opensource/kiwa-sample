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

/* * © 2025 Sphereon International B.V. *
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

import androidx.lifecycle.viewModelScope
import com.sphereon.core.api.AppLogManager
import com.sphereon.core.api.LogService
import com.sphereon.core.api.conf.DefaultPrincipalMapPropertySource
import com.sphereon.core.api.conf.DefaultTenantMapPropertySource
import com.sphereon.crypto.core.DefaultCallbacks
import com.sphereon.di.context.UserContextManager
import com.sphereon.di.session.SessionInstance
import com.sphereon.kiwa.sample.ui.auth.AuthSessionService
import com.sphereon.kiwa.sample.ui.auth.AuthSessionServiceImpl
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import platform.UIKit.UIApplication
import software.amazon.app.platform.presenter.BaseModel
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
     * Flow providing access to the current session instance.
     *
     * This flow provides access to session-scoped dependencies including the template provider factory.
     * Instance is always non-null (anonymous instance if not authenticated).
     */
    private val sessionInstanceFlow: StateFlow<SessionInstance>
        get() = authSessionService.sessionInstanceFlow

    /**
     * Currently active template provider.
     * This gets recreated when the session component changes.
     */
    override var currentTemplateProvider: TemplateProvider? = null


    /**
     * Stream of UI templates that define the application's user interface.
     *
     * This StateFlow emits BaseModel instances that represent the current UI state
     * and structure. The MainActivity observes this flow and renders the templates
     * using the compose renderer system.
     *
     * The templates are reactive and will automatically update when the underlying
     * application state changes, including when sessions change during authentication.
     *
     * Note: A session (anonymous or authenticated) is always initialized before this flow is accessed,
     * so sessionInstance should never be null.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val templates: StateFlow<BaseModel> by lazy {
        println("MainActivityViewModel: Initializing templates StateFlow")

        // Get the initial session instance and create initial template provider
        val initialSession = sessionInstanceFlow.value
        val initialProvider = (initialSession.component as TemplateProviderComponent).templateProviderFactory.createTemplateProvider()
        currentTemplateProvider = initialProvider

        // Create a reactive flow that recreates the template provider when session changes
        sessionInstanceFlow
            .map { sessionInstance ->
                println("MainActivityViewModel: Session instance changed: $sessionInstance")

                // Clean up the previous template provider
                currentTemplateProvider?.cancel()

                // Create new template provider from the current session component
                println("MainActivityViewModel: Creating new template provider from session component")
                val newProvider = (sessionInstance.component as TemplateProviderComponent).templateProviderFactory.createTemplateProvider()
                currentTemplateProvider = newProvider
                newProvider.templates
            }
            .flatMapLatest { it }
            .stateIn(
                scope = MainScope(),
                started = SharingStarted.Eagerly,
                initialValue = initialProvider.templates.value
            )
    }


    override fun onCreate(appComponent: IosAppComponent) {
        println("KiwaSampleApplication: === onCreate() START ===")

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
