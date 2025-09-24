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
import kotlinx.coroutines.flow.StateFlow
import software.amazon.app.platform.renderer.ComposeAndroidRendererFactory
import software.amazon.app.platform.renderer.getComposeRenderer
import software.amazon.app.platform.scope.RootScopeProvider
import com.sphereon.core.api.ISyncLogService
import com.sphereon.core.api.conf.DefaultPrincipalMapPropertySource
import com.sphereon.core.api.conf.DefaultTenantMapPropertySource
import com.sphereon.crypto.core.DefaultCallbacks
import com.sphereon.crypto.core.x509.X509VerifyServiceJvmAdapter
import com.sphereon.crypto.kms.keystore.memory.registerMemoryKeyStoreSerialization
import com.sphereon.crypto.kms.keystore.software.PlatformDirProvider
import com.sphereon.crypto.kms.keystore.software.registerSoftwareKeyStoreSerialization
import com.sphereon.crypto.kms.provider.software.registerSoftwareKmsSerialization
import com.sphereon.data.link.ble.AndroidBlePermissionsHelper
import com.sphereon.di.context.ISureContextComponent
import com.sphereon.di.session.ISureSessionComponent
import com.sphereon.ui.auth.IAuthSessionService
import java.io.File

class MainActivity : ComponentActivity() {

    init {
        // TODO: SDK should handle this instead of the end user/developer
        registerMemoryKeyStoreSerialization()
        registerSoftwareKeyStoreSerialization()
        registerSoftwareKmsSerialization()
    }

    @Suppress("UNCHECKED_CAST")
    companion object {
        lateinit var appComponent: AndroidAppComponent
        lateinit var nfcEngagementNavigationService: INfcEngagementNavigationService

        val authSessionService: IAuthSessionService
            get() = appComponent.authSessionService
        val contextComponentFlow: StateFlow<ISureContextComponent>
            get() {
                if (authSessionService.contextComponentFlow.value == null) {
                    authSessionService.authenticateAnonymous()
                }
                requireNotNull(authSessionService.contextComponentFlow.value) { "Context component not initialized" }
                return authSessionService.contextComponentFlow as StateFlow<ISureContextComponent>
            }

        val sessionComponentFlow: StateFlow<ISureSessionComponent>
            get() {
                if (authSessionService.sessionComponentFlow.value == null) {
                    contextComponentFlow // This already initializes the anonymous session
                }
                requireNotNull(authSessionService.sessionComponentFlow.value) { "Session component not initialized" }

                return authSessionService.sessionComponentFlow as StateFlow<ISureSessionComponent>
            }
        lateinit var rendererFactory: ComposeAndroidRendererFactory

        lateinit var log: ISyncLogService
    }

    private val viewModel by viewModels<MainActivityViewModel>()
    private lateinit var blePermissionsHelper: AndroidBlePermissionsHelper

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            blePermissionsHelper.onPermissionsResult(result)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent = AndroidAppComponent::class.create(applicationContext)
        appComponent.initRootScopeProvider()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        registerKeystoreDirProvider()
        // Principal props
        DefaultPrincipalMapPropertySource.addProperties(
            mapOf(
                "test.app.testing.kms.providers.kiwa.id" to "kiwa", // The KMS provider id. The sample app will use this value to identify the KMS provider.
                "test.app.testing.kms.providers.kiwa.type" to "software", // The KMS type. The KIWA API currently does not support a hardware KMS, because of the mtls approach of the Kiwa API
                "test.app.testing.kms.providers.kiwa.autocreatecertificate" to "true", // Ensures we create a certificate during key creation. Needed for this key store type!
                "test.app.testing.kms.providers.kiwa.keystore.id" to "kiwa", // Let's use the same id value as the KMS provider id
                "test.app.testing.kms.providers.kiwa.keystore.type" to "pkcs12",
                "test.app.testing.kms.providers.kiwa.keystore.password" to "S3cre3tPassword!",
                "test.app.testing.kms.providers.kiwa.keystore.persist" to "true",
                "test.app.testing.kms.providers.kiwa.keystore.path" to "{contextDir:kiwa}/keystore.p12",
                "test.app.testing.kms.providers.kiwa.keystore.keyvisibility" to "private",
                "test.app.testing.kms.providers.kiwa.keystore.overwritealias" to "true",
//                "kms.providers.kiwa.keystore.overwritealias" to "true",

            )
        )
        // Some tenant props
        DefaultTenantMapPropertySource.addProperties(
            mapOf(
                // FIXME: Prefix with tenant id automatically
                "kiwa.api.environment" to "acc",
                "kiwa.subscription.key.acc" to "d785f1024ad84484aeb37d1b13f57934",
//                "kms.providers.kiwa.keystore.overwritealias" to "true",
            )
        )

        DefaultCallbacks.setX509Default(X509VerifyServiceJvmAdapter())
        log = appComponent.appLogManager.withTagSync("test-app-main")
        rendererFactory = ComposeAndroidRendererFactory.createForComposeUi(rootScopeProvider = appComponent.rootScopeProvider as RootScopeProvider)

        blePermissionsHelper = AndroidBlePermissionsHelper(appComponent)
        blePermissionsHelper.setLauncher { permissions ->
            permissionLauncher.launch(permissions)
        }
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
        setContent {
            val template by viewModel.templates.collectAsState()
            val renderer = rendererFactory.getComposeRenderer(template)
            renderer.renderCompose(template)
        }
    }

    /**
     * Make sure we get Android specific directories so we can store keystore files in the app's private storage.
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

    override fun onResume() {
        super.onResume()
        NfcAdapter.getDefaultAdapter(this)?.let {
            val cardEmulation = CardEmulation.getInstance(it)
            val allowsForeground =
                cardEmulation.categoryAllowsForegroundPreference(CardEmulation.CATEGORY_OTHER)
            if (!allowsForeground) {
                log.warn(
                    "CardEmulation.categoryAllowsForegroundPreference(CATEGORY_OTHER) returned false. No foreground NFC processing supported"
                )
            } else {
                if (!cardEmulation.setPreferredService(this, ComponentName(this, MdocNfcService::class.java))) {
                    log.warn("CardEmulation.setPreferredService() returned false")
                }
                if (!cardEmulation.categoryAllowsForegroundPreference(CardEmulation.CATEGORY_OTHER)) {
                    log.warn("CardEmulation.categoryAllowsForegroundPreference(CATEGORY_OTHER) returned false")
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        NfcAdapter.getDefaultAdapter(this)?.let {
            val cardEmulation = CardEmulation.getInstance(it)
            if (!cardEmulation.unsetPreferredService(this)) {
                log.warn("CardEmulation.unsetPreferredService() return false")
            }
        }
    }
}
