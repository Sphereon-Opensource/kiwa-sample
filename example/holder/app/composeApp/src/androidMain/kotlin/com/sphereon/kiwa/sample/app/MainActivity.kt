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
import com.sphereon.data.link.ble.AndroidBlePermissionsHelper
import software.amazon.app.platform.renderer.getComposeRenderer

/**
 * Main activity serving as the entry point for the Kiwa sample holder application.
 *
 * This activity extends [ComponentActivity] to provide Jetpack Compose integration and serves
 * as the primary coordinator for NFC configuration, UI rendering, and BLE permissions.
 *
 * Key responsibilities:
 * - Set up NFC card emulation for mobile document presentation
 * - Manage BLE permissions for wireless communication
 * - Provide template-based UI rendering through the renderer factory
 *
 * The activity uses a template-based UI architecture where the UI is defined by reactive
 * templates that are rendered using a compose renderer system. This allows for dynamic
 * UI updates based on application state changes.
 *
 * Note: Application-wide initialization (DI, crypto services, etc.) is handled by [KiwaSampleApplication].
 */
class MainActivity : ComponentActivity() {
    /** ViewModel managing UI templates and surviving configuration changes. */
    private val viewModel by viewModels<MainActivityViewModel>().also {
        println("MainActivity: ViewModel is being created")
    }

    /** Helper for managing BLE permission requests and status. */
    private lateinit var blePermissionsHelper: AndroidBlePermissionsHelper

    private lateinit var appServices: AppServices

    /** Reference to the application instance for accessing app-wide singletons. */
    private val app: KiwaSampleApplication
        get() = application as KiwaSampleApplication

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
     * This method performs activity-specific initialization including:
     * - BLE permission management
     * - UI content setup with template rendering
     *
     * @param savedInstanceState Previously saved instance state, or null if none exists
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        println("MainActivity: === onCreate() START ===")

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        appServices = app.appComponent.appServices
        println("MainActivity: super.onCreate() completed. Appservices: $appServices")

        // Set up BLE permissions management
        println("MainActivity: Setting up BLE permissions management")
        blePermissionsHelper = AndroidBlePermissionsHelper(app.appComponent)
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
            val renderer = app.rendererFactory.getComposeRenderer(template)
            println("MainActivity: Renderer obtained: $renderer")
            renderer.renderCompose(template)
            println("MainActivity: Template rendered successfully")
        }
        println("MainActivity: === onCreate() COMPLETE ===")
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
                app.log.warn(
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
                    app.log.warn("CardEmulation.setPreferredService() returned false")
                }

                // Double-check foreground preference after setting preferred service
                if (!cardEmulation.categoryAllowsForegroundPreference(CardEmulation.CATEGORY_OTHER)) {
                    app.log.warn("CardEmulation.categoryAllowsForegroundPreference(CATEGORY_OTHER) returned false")
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
                app.log.warn("CardEmulation.unsetPreferredService() return false")
            }
        }
    }

    companion object {
        private lateinit var applicationInstance: KiwaSampleApplication

        internal fun setApplication(app: KiwaSampleApplication) {
            applicationInstance = app
        }
    }
}
