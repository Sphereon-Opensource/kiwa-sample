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

package com.sphereon.kiwa.sample.app

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sphereon.di.session.SessionScope
import kotlinx.coroutines.flow.*
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

/**
 * ViewModel that manages the UI templates and survives Android configuration changes.
 *
 * This ViewModel extends [AndroidViewModel] to gain access to the Android [Application] context,
 * which is used to integrate with the dependency injection system and retrieve the root scope.
 * It serves as the bridge between the Android UI layer and the business logic layer.
 *
 * The ViewModel manages a stream of UI templates that define the application's user interface
 * structure and state. These templates are provided by a [TemplateProvider] which is obtained
 * from the session-scoped dependency injection component.
 *
 * IMPORTANT: This ViewModel is now reactive to session changes, recreating the template provider
 * whenever the session component changes (e.g., during authentication transitions).
 *
 * Key responsibilities:
 * - Provide a lifecycle-aware stream of UI templates to the Activity
 * - Survive Android configuration changes (device rotation, etc.)
 * - React to session changes and recreate template providers accordingly
 * - Manage template provider lifecycle and cleanup
 * - Bridge the Android framework with the application's DI system
 *
 * @param application The Android [Application] instance used to access the root scope
 */
class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MainActivityViewModel"
    }

    /**
     * Application services providing access to session component flow.
     */
    private val appServices: AppServices = (application as KiwaSampleApplication).appComponent.appServices

    /**
     * Flow providing access to the current session instance.
     *
     * This flow provides access to session-scoped dependencies including the template provider factory.
     * Instance is always non-null (anonymous instance if not authenticated).
     */
    private val sessionInstanceFlow = appServices.authSessionService.sessionInstanceFlow

    /**
     * Currently active template provider.
     * This gets recreated when the session component changes.
     */
    private var currentTemplateProvider: TemplateProvider? = null

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
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = initialProvider.templates.value
            )
    }

    /**
     * Called when the ViewModel is about to be destroyed.
     *
     * This method ensures proper cleanup by cancelling the current template provider,
     * which stops any ongoing coroutines and releases resources to prevent
     * memory leaks.
     */
    override fun onCleared() {
        println("MainActivityViewModel: ViewModel being cleared, cancelling template provider")
        currentTemplateProvider?.cancel()
        super.onCleared()
    }

    /**
     * Component interface providing access to template provider factory from the DI graph.
     *
     * This interface is contributed to the [SessionScope], making the template
     * provider factory available as a session-scoped dependency. It allows the
     * ViewModel to create template providers with access to session-scoped services.
     */
    @ContributesTo(SessionScope::class)
    interface TemplateProviderComponent {
        /**
         * Factory for creating [TemplateProvider] instances.
         *
         * The factory is injected from the session-scoped dependency graph,
         * ensuring that created template providers have access to all necessary
         * session-scoped services and configuration.
         */
        val templateProviderFactory: TemplateProvider.Factory
    }
}
