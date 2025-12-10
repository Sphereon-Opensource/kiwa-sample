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

import android.app.Application
import android.content.Intent
import com.sphereon.core.api.LogService
import com.sphereon.core.api.conf.DefaultPrincipalMapPropertySource
import com.sphereon.core.api.conf.DefaultTenantMapPropertySource
import com.sphereon.crypto.core.DefaultCallbacks
import com.sphereon.crypto.core.x509.X509VerifyServiceJvmAdapter
import com.sphereon.crypto.kms.keystore.software.PlatformDirProvider
import com.sphereon.kiwa.sample.ui.auth.AuthSessionService
import software.amazon.app.platform.renderer.ComposeAndroidRendererFactory
import java.io.File

/**
 * Custom Application class for the Kiwa sample holder application.
 *
 * This class is created once when the app starts and lives for the entire lifetime of the app process.
 * It provides globally accessible singleton instances that survive activity lifecycle events and are
 * available to all components including activities, services, and broadcast receivers.
 *
 * Key responsibilities:
 * - Initialize the dependency injection component hierarchy
 * - Configure cryptographic services and key management systems
 * - Provide access to core application services
 * - Maintain application-wide configuration
 */
class KiwaSampleApplication : Application() {

    /** Root application component providing access to application-scoped dependencies. */
    lateinit var appComponent: AndroidAppComponent
        private set

    /** Service for managing NFC engagement navigation and coordination. */
    lateinit var nfcEngagementNavigationService: NfcEngagementNavigationService


    /**
     * Authentication session service for managing user authentication and sessions.
     *
     * This property provides access to the authentication service through the app component,
     * enabling session management and user authentication workflows.
     * 
     * The AuthSessionService contains flows for context and session components, as well as
     * authentication state. Use authSessionService.contextComponentFlow and 
     * authSessionService.sessionComponentFlow to access the current components.
     */
    val authSessionService: AuthSessionService
        get() {
            println("===============>> Auth session service requested")
            return appComponent.authSessionService
        }

    /** Factory for creating Compose renderers for template-based UI rendering. */
    lateinit var rendererFactory: ComposeAndroidRendererFactory
        private set

    /** Application-wide logging service for operation tracking and debugging. */
    lateinit var log: LogService
        private set

    override fun onCreate() {
        super.onCreate()
        println("KiwaSampleApplication: === onCreate() START ===")

        // Set up backward compatibility with MainActivity companion object
        MainActivity.setApplication(this)

        println("KiwaSampleApplication: Serialization handlers registered")

        // Initialize the dependency injection hierarchy
        println("KiwaSampleApplication: Initializing dependency injection components")
        appComponent = AndroidAppComponent::class.create(this)
        appComponent.initRootScopeProvider()
        log = appComponent.appLogManager.withTag("test-app-main")
        println("KiwaSampleApplication: Dependency injection components initialized")

        // Configure platform-specific directory providers for keystore management
        registerKeystoreDirProvider()
        println("KiwaSampleApplication: Keystore directory provider registered")

        // Configure principal-level properties for KMS providers
        println("KiwaSampleApplication: Configuring principal-level properties")
        DefaultPrincipalMapPropertySource.addProperties(
            mapOf(
                // KMS provider configuration for Kiwa integration
                "test.app.testing.kms.providers.kiwa.id" to "kiwa",
                "test.app.testing.kms.providers.kiwa.type" to "software",
                "test.app.testing.kms.providers.kiwa.autocreatecertificate" to "true",
                "test.app.testing.kms.providers.kiwa.keystore.id" to "kiwa",
                "test.app.testing.kms.providers.kiwa.keystore.type" to "pkcs12",
                "test.app.testing.kms.providers.kiwa.keystore.password" to "S3cre3tPassword!",
                "test.app.testing.kms.providers.kiwa.keystore.persist" to "true",
                "test.app.testing.kms.providers.kiwa.keystore.path" to "{contextDir:kiwa}/keystore.p12",
                "test.app.testing.kms.providers.kiwa.keystore.keyvisibility" to "private",
                "test.app.testing.kms.providers.kiwa.keystore.overwritealias" to "true",
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
        DefaultCallbacks.setX509Default(X509VerifyServiceJvmAdapter())

        // Initialize logging and renderer factory
        log.debug("KiwaSampleApplication: Initializing logging and renderer factory")
        rendererFactory = ComposeAndroidRendererFactory.createForComposeUi(
            rootScopeProvider = appComponent.rootScopeProvider as software.amazon.app.platform.scope.RootScopeProvider
        )
        log.debug("KiwaSampleApplication: Renderer factory created")

        // Start NFC service early to initialize it at app startup
        log.debug("KiwaSampleApplication: Starting NFC service")
        try {
            startService(Intent(this, MdocNfcService::class.java))
            println("KiwaSampleApplication: NFC service started successfully")
        } catch (e: Exception) {
            log.error("KiwaSampleApplication: Failed to start NFC service: ${e.message}", exception = e)
        }

        log.debug("KiwaSampleApplication: === onCreate() COMPLETE ===")
    }

    /**
     * Configures Android-specific directory providers for keystore file storage.
     *
     * This method sets up platform-specific directory providers that allow the keystore
     * system to access appropriate Android directories for storing keystore files securely
     * within the application's private storage space.
     */
    private fun registerKeystoreDirProvider() {
        PlatformDirProvider.set(object : PlatformDirProvider.Provider {
            override fun filesDir(): File = applicationContext.filesDir
            override fun cacheDir(): File = applicationContext.cacheDir
            override fun contextDir(name: String): File =
                applicationContext.getDir(name, MODE_PRIVATE)
            override fun tempDir(): File = applicationContext.cacheDir
        })
    }
}
