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

import android.content.ComponentName
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sphereon.core.api.SyncLogService
import com.sphereon.core.api.conf.DefaultPrincipalMapPropertySource
import com.sphereon.core.api.conf.DefaultTenantMapPropertySource
import com.sphereon.crypto.core.DefaultCallbacks
import com.sphereon.crypto.core.x509.X509VerifyServiceJvmAdapter
import com.sphereon.crypto.kms.keystore.memory.registerMemoryKeyStoreSerialization
import com.sphereon.crypto.kms.keystore.software.PlatformDirProvider
import com.sphereon.crypto.kms.keystore.software.registerSoftwareKeyStoreSerialization
import com.sphereon.crypto.kms.provider.software.registerSoftwareKmsSerialization
import com.sphereon.data.link.ble.AndroidBlePermissionsHelper
import com.sphereon.di.context.UserContextComponent
import com.sphereon.di.session.SessionComponent
import com.sphereon.kiwa.sample.ui.auth.AuthSessionService
import kotlinx.coroutines.flow.StateFlow
import software.amazon.app.platform.renderer.ComposeAndroidRendererFactory
import software.amazon.app.platform.renderer.getComposeRenderer
import software.amazon.app.platform.scope.RootScopeProvider
import java.io.File

/**
 * Main activity serving as the entry point for the Kiwa sample holder application.
 *
 * This activity extends [ComponentActivity] to provide Jetpack Compose integration and serves
 * as the primary coordinator for the application's initialization, dependency injection setup,
 * NFC configuration, and UI rendering.
 *
 * Key responsibilities:
 * - Initialize the dependency injection component hierarchy
 * - Configure cryptographic services and key management systems
 * - Set up NFC card emulation for mobile document presentation
 * - Manage BLE permissions for wireless communication
 * - Provide template-based UI rendering through the renderer factory
 * - Maintain static references to core application services
 *
 * The activity uses a template-based UI architecture where the UI is defined by reactive
 * templates that are rendered using a compose renderer system. This allows for dynamic
 * UI updates based on application state changes.
 */
class MainActivity : ComponentActivity() {
    /** ViewModel managing UI templates and surviving configuration changes. */
    private val viewModel by viewModels<MainActivityViewModel>().also {
        println("MainActivity: ViewModel is being created")
    }

    /** Helper for managing BLE permission requests and status. */
    private lateinit var blePermissionsHelper: AndroidBlePermissionsHelper

    init {
        // TODO: SDK should handle this instead of the end user/developer
        /**
         * Register serialization handlers for different keystore types.
         *
         * This initialization is currently required by the application but should
         * ideally be handled internally by the SDK to reduce integration complexity.
         */
        registerMemoryKeyStoreSerialization()
        registerSoftwareKeyStoreSerialization()
        registerSoftwareKmsSerialization()
    }

    /**
     * Activity result launcher for handling multiple permission requests.
     *
     * This launcher is specifically used for BLE permissions but can be extended
     * to handle other permission types. It delegates the permission result handling
     * to the BLE permissions helper.
     */
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            blePermissionsHelper.onPermissionsResult(result)
        }

    /**
     * Called when the activity is being created.
     *
     * This method performs comprehensive application initialization including:
     * - Dependency injection component setup
     * - Cryptographic service configuration
     * - Property source configuration for KMS and API settings
     * - BLE permission management
     * - UI content setup with template rendering
     *
     * @param savedInstanceState Previously saved instance state, or null if none exists
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        println("MainActivity: === onCreate() START ===")
        println("###################################################################################################")

        // Initialize the dependency injection hierarchy
        println("MainActivity: Initializing dependency injection components")
        appComponent = AndroidAppComponent::class.create(applicationContext)
        appComponent.initRootScopeProvider()
        println("MainActivity: Dependency injection components initialized")

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        println("MainActivity: super.onCreate() completed")

        // Configure platform-specific directory providers for keystore management
        registerKeystoreDirProvider()
        println("MainActivity: Keystore directory provider registered")

        // Configure principal-level properties for KMS providers
        println("MainActivity: Configuring principal-level properties")
        DefaultPrincipalMapPropertySource.addProperties(
            mapOf(
                // KMS provider configuration for Kiwa integration
                "test.app.testing.kms.providers.kiwa.id" to "kiwa", // The KMS provider id. The sample app will use this value to identify the KMS provider.
                "test.app.testing.kms.providers.kiwa.type" to "software", // The KMS type. The KIWA API currently does not support a hardware KMS, because of mtls
                "test.app.testing.kms.providers.kiwa.autocreatecertificate" to "true", // Ensures we create a certificate during key creation. Needed for this key store type!
                "test.app.testing.kms.providers.kiwa.keystore.id" to "kiwa", // Let's use the same id value as the KMS provider id
                "test.app.testing.kms.providers.kiwa.keystore.type" to "pkcs12",
                "test.app.testing.kms.providers.kiwa.keystore.password" to "S3cre3tPassword!",
                "test.app.testing.kms.providers.kiwa.keystore.persist" to "true",
                "test.app.testing.kms.providers.kiwa.keystore.path" to "{contextDir:kiwa}/keystore.p12",
                "test.app.testing.kms.providers.kiwa.keystore.keyvisibility" to "private",
                "test.app.testing.kms.providers.kiwa.keystore.overwritealias" to "true",
            )
        )

        // Configure tenant-level properties for API integration
        println("MainActivity: Configuring tenant-level properties")
        DefaultTenantMapPropertySource.addProperties(
            mapOf(
                // FIXME: Prefix with tenant id automatically
                "kiwa.api.environment" to "acc",
                "kiwa.subscription.key.acc" to "d785f1024ad84484aeb37d1b13f57934",
            )
        )

        // Set up cryptographic verification services
        println("MainActivity: Setting up cryptographic verification services")
        DefaultCallbacks.setX509Default(X509VerifyServiceJvmAdapter())

        // Initialize logging and renderer factory
        println("MainActivity: Initializing logging and renderer factory")
        log = appComponent.appLogManager.withTagSync("test-app-main")
        rendererFactory = ComposeAndroidRendererFactory.createForComposeUi(rootScopeProvider = appComponent.rootScopeProvider as RootScopeProvider)
        println("MainActivity: Renderer factory created")

        // Set up BLE permissions management
        println("MainActivity: Setting up BLE permissions management")
        blePermissionsHelper = AndroidBlePermissionsHelper(appComponent)
        blePermissionsHelper.setLauncher { permissions ->
            permissionLauncher.launch(permissions)
        }

        // Request BLE permissions if not already granted
        if (!blePermissionsHelper.hasPermissions()) {
            println("Requesting BLE permissions")
            blePermissionsHelper.requestPermissions { granted ->
                if (granted) {
                    println("BLE permissions granted")
                } else {
                    println("BLE permissions NOT granted")
                }
            }
        }
        println("MainActivity: BLE permissions setup complete")

        // Set up the UI content with template-based rendering
        println("MainActivity: About to call setContent()")
        setContent {
            println("MainActivity: === Inside setContent block - starting template observation ===")
            println("MainActivity: Accessing viewModel.templates")
            val template by viewModel.templates.collectAsState()
            println("MainActivity: Template collected: $template")
            val renderer = rendererFactory.getComposeRenderer(template)
            println("MainActivity: Renderer obtained: $renderer")
            renderer.renderCompose(template)
            println("MainActivity: Template rendered successfully")
        }
        println("MainActivity: === onCreate() COMPLETE ===")
    }

    /**
     * Configures Android-specific directory providers for keystore file storage.
     *
     * This method sets up platform-specific directory providers that allow the keystore
     * system to access appropriate Android directories for storing keystore files securely
     * within the application's private storage space.
     *
     * The provider implementation ensures that keystore files are stored in locations
     * that are only accessible by the application, maintaining security and preventing
     * unauthorized access to cryptographic materials.
     */
    private fun registerKeystoreDirProvider() {
        PlatformDirProvider.set(object : PlatformDirProvider.Provider {
            /** Returns the application's private files directory. */
            override fun filesDir(): File = applicationContext.filesDir

            /** Returns the application's cache directory. */
            override fun cacheDir(): File = applicationContext.cacheDir

            /**
             * Returns a context-specific directory within the application's private storage.
             *
             * @param name The name identifier for the context-specific directory
             * @return A File representing the private directory for the given context
             */
            override fun contextDir(name: String): File =
                applicationContext.getDir(name, MODE_PRIVATE)

            /** Returns the temporary directory (cache directory). */
            override fun tempDir(): File = applicationContext.cacheDir
        })
    }

    /**
     * Called when the activity becomes visible to the user.
     *
     * This method configures NFC card emulation settings to enable the device to act
     * as a contactless card for mobile document presentation. It sets up foreground
     * NFC processing and registers the MdocNfcService as the preferred service for
     * handling NFC interactions.
     */
    override fun onResume() {
        super.onResume()
        NfcAdapter.getDefaultAdapter(this)?.let { nfcAdapter ->
            val cardEmulation = CardEmulation.getInstance(nfcAdapter)
            val allowsForeground =
                cardEmulation.categoryAllowsForegroundPreference(CardEmulation.CATEGORY_OTHER)

            if (!allowsForeground) {
                log.warn(
                    """
                    CardEmulation.categoryAllowsForegroundPreference(CATEGORY_OTHER) 
                    returned false. No foreground NFC processing supported
                    """.trimIndent()
                )
            } else {
                // Set the MdocNfcService as the preferred service for NFC interactions
                if (!cardEmulation.setPreferredService(
                        this,
                        ComponentName(this, MdocNfcService::class.java)
                    )
                ) {
                    log.warn("CardEmulation.setPreferredService() returned false")
                }

                // Double-check foreground preference after setting preferred service
                if (!cardEmulation.categoryAllowsForegroundPreference(CardEmulation.CATEGORY_OTHER)) {
                    log.warn("CardEmulation.categoryAllowsForegroundPreference(CATEGORY_OTHER) returned false")
                }
            }
        }
    }

    /**
     * Called when the activity is no longer visible to the user.
     *
     * This method cleans up NFC card emulation settings by unregistering the preferred
     * service, allowing other applications to handle NFC interactions when this activity
     * is not in the foreground.
     */
    override fun onPause() {
        super.onPause()
        NfcAdapter.getDefaultAdapter(this)?.let { nfcAdapter ->
            val cardEmulation = CardEmulation.getInstance(nfcAdapter)
            if (!cardEmulation.unsetPreferredService(this)) {
                log.warn("CardEmulation.unsetPreferredService() return false")
            }
        }
    }

    /**
     * Companion object providing static access to core application services and components.
     *
     * This companion object maintains singleton references to essential services that need
     * to be accessed from various parts of the application, including services instantiated
     * by the Android framework (like NFC services) that cannot use dependency injection.
     */
    @Suppress("UNCHECKED_CAST")
    companion object {
        /** Root application component providing access to application-scoped dependencies. */
        lateinit var appComponent: AndroidAppComponent

        /** Service for managing NFC engagement navigation and coordination. */
        lateinit var nfcEngagementNavigationService: NfcEngagementNavigationService

        /**
         * NFC authentication bridge for routing NFC events between contexts.
         *
         * This bridge operates at app scope and routes NFC engagement events from the
         * anonymous NFC service context to the authenticated user's session context.
         */
        val nfcAuthenticationBridge: NfcAuthenticationBridge
            get() = appComponent.nfcAuthenticationBridge

        /**
         * Authentication session service for managing user authentication and sessions.
         *
         * This property provides access to the authentication service through the app component,
         * enabling session management and user authentication workflows.
         */
        val authSessionService: AuthSessionService
            get() {
                println("===============>> Auth session service requested")
                return appComponent.authSessionService
            }

        /**
         * StateFlow providing access to the current context component.
         *
         * This flow automatically initializes an anonymous session if no context component
         * is currently available, ensuring that context-scoped dependencies are always accessible.
         *
         * @return StateFlow emitting the current context component
         * @throws IllegalArgumentException if the context component cannot be initialized
         */
        val contextComponentFlow: StateFlow<UserContextComponent>
            get() {
                if (authSessionService.contextComponentFlow.value == null) {
                    authSessionService.authenticateAnonymous()
                }
                requireNotNull(authSessionService.contextComponentFlow.value) {
                    "Context component not initialized"
                }
                return authSessionService.contextComponentFlow as StateFlow<UserContextComponent>
            }

        /**
         * StateFlow providing access to the current session component.
         *
         * This flow automatically initializes the session through the context component flow
         * if no session component is currently available, ensuring that session-scoped
         * dependencies are always accessible.
         *
         * @return StateFlow emitting the current session component
         * @throws IllegalArgumentException if the session component cannot be initialized
         */
        val sessionComponentFlow: StateFlow<SessionComponent>
            get() {
                if (authSessionService.sessionComponentFlow.value == null) {
                    contextComponentFlow // This already initializes the anonymous session
                }
                requireNotNull(authSessionService.sessionComponentFlow.value) {
                    "Session component not initialized"
                }

                return authSessionService.sessionComponentFlow as StateFlow<SessionComponent>
            }

        /** Factory for creating Compose renderers for template-based UI rendering. */
        lateinit var rendererFactory: ComposeAndroidRendererFactory

        /** Application-wide logging service for operation tracking and debugging. */
        lateinit var log: SyncLogService
    }
}
