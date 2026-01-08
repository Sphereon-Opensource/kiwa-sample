package com.sphereon.kiwa.sample.app

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import software.amazon.app.platform.renderer.ComposeRendererFactory
import software.amazon.app.platform.scope.RootScopeProvider
import kotlin.experimental.ExperimentalObjCName

/**
 * This function is called from Swift to hook up the Compose Multiplatform UI.
 *
 * This is our entry point to start producing templates and hooking up our Renderer runtime.
 * Following the Amazon App Platform reference pattern for proper reactive state updates.
 */
@OptIn(ExperimentalObjCName::class)
@Suppress("FunctionNaming")
@ObjCName("MainViewController")
fun MainViewController(appComponent: IosAppComponent, appServices: AppServices): UIViewController =
    ComposeUIViewController {
        // Get the root scope provider for the renderer factory
        val rootScopeProvider = appComponent.rootScopeProvider
        val appServicesImpl = appServices as AppServicesImpl

        // Observe the current session instance - this changes on authentication
        val sessionInstance by appServicesImpl.authSessionService.sessionInstanceFlow.collectAsState()

        // Create/recreate the template provider when session changes
        // Use remember with sessionInstance as key to recreate when session changes
        val templateProvider = remember(sessionInstance) {
            // Cancel previous provider if exists
            appServicesImpl.currentTemplateProvider?.cancel()

            val provider = (sessionInstance.component as TemplateProviderComponent)
                .templateProviderFactory
                .createTemplateProvider()
            // Store reference for cleanup
            appServicesImpl.currentTemplateProvider = provider
            provider
        }

        DisposableEffect(Unit) {
            onDispose {
                // Cancel the provider when it's no longer needed.
                templateProvider.cancel()
            }
        }

        // Create a single renderer factory instance using remember
        val factory = remember {
            ComposeRendererFactory(rootScopeProvider as RootScopeProvider)
        }

        // Collect templates from the provider - this is now properly reactive
        // because both the provider and the collection happen within composition
        val template by templateProvider.templates.collectAsState()

        // Get renderer by template class (matching Amazon reference pattern)
        val renderer = factory.getRenderer(template::class)

        // Render the template
        renderer.renderCompose(template)
    }
