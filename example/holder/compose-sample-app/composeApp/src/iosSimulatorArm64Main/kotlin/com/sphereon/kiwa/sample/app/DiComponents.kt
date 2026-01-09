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

import com.sphereon.core.defaults.app.DefaultRootScopeProvider
import com.sphereon.di.app.AbstractAppComponent
import com.sphereon.di.app.AppComponent
import com.sphereon.di.context.UserScope
import com.sphereon.di.session.SessionScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import platform.UIKit.UIApplication
import software.amazon.app.platform.presenter.PresenterCoroutineScope
import software.amazon.app.platform.presenter.molecule.MoleculeScopeFactory
import software.amazon.app.platform.presenter.molecule.backgesture.BackGestureDispatcherPresenter
import software.amazon.app.platform.scope.coroutine.MainCoroutineDispatcher
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.experimental.ExperimentalObjCName

/**
 * iOS implementation for [AbstractAppComponent] and provides the package name as application
 * ID as well as the application.
 *
 * This class is a singleton and automatically provided in the dependency graph whenever you
 * inject [AbstractAppComponent] through the [ContributesBinding] annotation.
 * This component serves as the root of the dependency injection hierarchy for the iOS
 * application, establishing the foundation for other scoped components.
 *
 * @param application The iOS application instance
 * @param appId Unique identifier for the application (defaults to "test-app")
 * @param profile Application profile identifier (defaults to "testing")
 * @param version Application version string (defaults to "0.0.1")
 */
@SingleIn(AppScope::class)
@MergeComponent(AppScope::class)
@Component
@OptIn(ExperimentalObjCName::class)
@ObjCName("IosAppComponent")
actual abstract class IosAppComponent(
    application: UIApplication,
    appId: String = "test-app",
    profile: String = "testing",
    version: String = "0.0.1",
) : IosAppComponentMerged,
    AbstractAppComponent(
        application,
        appId = appId,
        profile = profile,
        version = version,
        rootScopeProvider = DefaultRootScopeProvider()
    ) {

    companion object {
        fun init(
            application: UIApplication,
            appId: String = "sample-app",
            profile: String = "testing",
            version: String = "0.1.0"
        ): IosAppComponent {
            val appComponent = IosAppComponent::class.create(
                application = application,
                appId = appId,
                profile = profile,
                version = version
            )
            return appComponent.initRootScopeProvider() as IosAppComponent
        }
    }

    abstract override val appServices: AppServices

    @Provides
    @SingleIn(AppScope::class)
    public fun provideUIApplication(): UIApplication = application as UIApplication

    /**
     * Provides [ImmediateMoleculeScopeFactory] in the kotlin-inject graph as a singleton.
     *
     * Uses [RecompositionMode.Immediate] instead of [RecompositionMode.ContextClock] to ensure
     * Molecule recomposes immediately when state changes occur. This fixes issues with async
     * state updates (like BLE events) not triggering UI updates when showing static content.
     */
    @Provides
    @SingleIn(AppScope::class)
    public fun provideIosMoleculeScopeFactory(
        @PresenterCoroutineScope coroutineScopeFactory: () -> CoroutineScope
    ): MoleculeScopeFactory = ImmediateMoleculeScopeFactory(coroutineScopeFactory)

    @Provides
    @PresenterCoroutineScope
    public fun providePresenterCoroutineScope(
        @ForScope(AppScope::class) scope: CoroutineScope,
        @MainCoroutineDispatcher mainDispatcher: CoroutineDispatcher,
    ): CoroutineScope = scope + mainDispatcher

    @Provides
    @SingleIn(AppScope::class)
    public fun provideBackGestureDispatcherPresenter(): BackGestureDispatcherPresenter =
        BackGestureDispatcherPresenter.createNewInstance()


    /* *//** Gives access to the [TemplateProvider.Factory] from the object graph. *//*
    abstract val templateProviderFactory: TemplateProvider.Factory*/

}

/**
 * iOS-specific context component that manages context-scoped dependencies.
 *
 * This component represents the second level in the dependency injection hierarchy,
 * sitting between the application component and session component. It manages
 * dependencies that have a context-level lifetime.
 *
 * @param appComponent The parent [IosAppComponent] that provides application-level dependencies
 */
@SingleIn(UserScope::class)
@MergeComponent(UserScope::class)
@Component
@OptIn(ExperimentalObjCName::class)
@ObjCName("IosUserContextComponent")
actual abstract class IosUserContextComponent(
    @Component
    val appComponent: IosAppComponent
) : IosUserContextComponentMerged

/**
 * Android-specific session component that manages session-scoped dependencies.
 *
 * This component represents the finest-grained level in the dependency injection hierarchy,
 * managing dependencies that have a session-level lifetime. Sessions typically correspond
 * to user authentication states or specific application workflows.
 *
 * @param appComponent The root [IosAppComponent] that provides application-level dependencies
 * @param contextComponent The parent [IosUserContextComponent] that provides context-level dependencies
 */
@MergeComponent(SessionScope::class)
@SingleIn(SessionScope::class)
@Component
@OptIn(ExperimentalObjCName::class)
@ObjCName("IosSessionContextComponent")
actual abstract class IosSessionContextComponent(
    @Component
    val appComponent: IosAppComponent,
    @Component
    val contextComponent: IosUserContextComponent
) : IosSessionContextComponentMerged {
    /** Provides the [IosMoleculeScopeFactory] in the kotlin-inject graph as a singleton. */
  /*  @Provides
    @SingleIn(SessionScope::class)
    public fun provideIosMoleculeScopeFactory(
        @PresenterCoroutineScope coroutineScopeFactory: () -> CoroutineScope
    ): MoleculeScopeFactory = IosMoleculeScopeFactory(coroutineScopeFactory)*/
}
