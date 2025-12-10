package com.sphereon.kiwa.sample.app

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import software.amazon.app.platform.renderer.ComposeRendererFactory
import software.amazon.app.platform.renderer.getComposeRenderer
import kotlin.experimental.ExperimentalObjCName

// iOS interop function - must start with uppercase as per iOS/Swift conventions
@OptIn(ExperimentalObjCName::class)
@Suppress("FunctionNaming")
@ObjCName("MainViewController")
fun MainViewController(appComponent: IosAppComponent, appServices: AppServices): UIViewController =
    ComposeUIViewController {

        /**
         * Application services providing access to session component flow.
         */
//        val appServices: AppServices = appComponent.appServices

        val rootScopeProvider = appComponent.rootScopeProvider

       /* *//**
         * Flow providing access to the current session instance.
         *
         * This flow provides access to session-scoped dependencies including the template provider factory.
         * Instance is always non-null (anonymous instance if not authenticated).
         *//*
        val sessionInstanceFlow = appServices.authSessionService.sessionInstanceFlow
*/

       /* // Create a single instance.
        val templateProvider = remember {
            rootScopeProvider.rootScope
                .kotlinInjectComponent<IosAppComponent>()
                .templateProviderFactory
                .createTemplateProvider()
        }*/

        DisposableEffect(Unit) {
            onDispose {
                // Cancel the provider when it's no longer needed.
                appServices.currentTemplateProvider?.cancel()
            }
        }

        // Only a single factory is needed.
        val factory = remember { ComposeRendererFactory(rootScopeProvider) }

        // Render templates using our Renderer runtime.
        val template by appServices.currentTemplateProvider!!.templates.collectAsState()

        val renderer = factory.getComposeRenderer(template::class)
        renderer.renderCompose(template)

    }
