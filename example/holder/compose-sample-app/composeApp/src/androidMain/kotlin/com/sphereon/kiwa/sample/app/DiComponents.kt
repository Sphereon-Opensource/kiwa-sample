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

import android.content.Context
import com.sphereon.core.defaults.app.DefaultRootScopeProvider
import com.sphereon.di.app.AbstractAppComponent
import com.sphereon.di.app.AppComponent
import com.sphereon.di.context.UserScope
import com.sphereon.di.session.SessionScope
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Android implementation for [AbstractAppComponent] and provides the package name as application
 * ID as well as the application and its context.
 *
 * This class is a singleton and automatically provided in the dependency graph whenever you
 * inject [AbstractAppComponent] through the [ContributesBinding] annotation.
 * This component serves as the root of the dependency injection hierarchy for the Android
 * application, providing access to the Android application context and establishing
 * the foundation for other scoped components.
 *
 * @param application The Android application instance, which must be of type [Context]
 * @param appId Unique identifier for the application (defaults to "test-app")
 * @param profile Application profile identifier (defaults to "testing")
 * @param version Application version string (defaults to "0.0.1")
 *
 * @throws IllegalArgumentException if the provided application is not a [Context] instance
 */
@SingleIn(AppScope::class)
@MergeComponent(AppScope::class)
@Component
abstract class AndroidAppComponent(
    application: Any,
    appId: String = "test-app",
    profile: String = "testing",
    version: String = "0.0.1",
) : AndroidAppComponentMerged,
    AbstractAppComponent(
        application,
        appId = appId,
        profile = profile,
        version = version,
        rootScopeProvider = DefaultRootScopeProvider()
    ),
    IAndroidAppComponent {
    init {
        require(application is Context) { "The application provided must be of type ${Context::class.java.simpleName}" }
    }

    /**
     * Provides access to the Android application context.
     *
     * This method safely casts the application object to a Context,
     * which is guaranteed to be valid due to the initialization check.
     *
     * @return The Android [Context] associated with this application component
     */
    override fun getContext(): Context = application as Context

    /**
     * Provides the [KiwaSampleApplication] instance for dependency injection.
     *
     * This allows other components to inject the application instance directly,
     * enabling access to application-level services.
     *
     * @return The [KiwaSampleApplication] instance
     * @throws IllegalStateException if the application is not an instance of [KiwaSampleApplication]
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideKiwaSampleApplication(): KiwaSampleApplication {
        require(application is KiwaSampleApplication) {
            "Application must be an instance of ${KiwaSampleApplication::class.java.simpleName}, " +
                    "but was ${application::class.java.simpleName}"
        }
        return application as KiwaSampleApplication
    }
}

/**
 * Interface defining the Android-specific extension of the base app component.
 *
 * This interface extends [AppComponent] to provide Android-specific functionality,
 * primarily access to the Android [Context] which is essential for Android-specific
 * operations throughout the application.
 */
interface IAndroidAppComponent : AppComponent {
    /**
     * Provides access to the Android application context.
     *
     * @return The Android [Context] for this application
     */
    fun getContext(): Context
}

/**
 * Android-specific context component that manages context-scoped dependencies.
 *
 * This component represents the second level in the dependency injection hierarchy,
 * sitting between the application component and session component. It manages
 * dependencies that have a context-level lifetime.
 *
 * @param appComponent The parent [AndroidAppComponent] that provides application-level dependencies
 */
@SingleIn(UserScope::class)
@MergeComponent(UserScope::class)
@Component
abstract class AndroidContextComponent(
    @Component
    val appComponent: AndroidAppComponent
) : AndroidContextComponentMerged

/**
 * Android-specific session component that manages session-scoped dependencies.
 *
 * This component represents the finest-grained level in the dependency injection hierarchy,
 * managing dependencies that have a session-level lifetime. Sessions typically correspond
 * to user authentication states or specific application workflows.
 *
 * @param appComponent The root [AndroidAppComponent] that provides application-level dependencies
 * @param contextComponent The parent [AndroidContextComponent] that provides context-level dependencies
 */
@MergeComponent(SessionScope::class)
@SingleIn(SessionScope::class)
@Component
abstract class AndroidSessionComponent(
    @Component
    val appComponent: AndroidAppComponent,
    @Component
    val contextComponent: AndroidContextComponent
) : AndroidSessionComponentMerged
