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

package com.sphereon.kiwa.sdk

import me.tatarka.inject.annotations.Component
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import com.sphereon.core.defaults.app.DefaultRootScopeProvider
import com.sphereon.di.app.AbstractAppComponent
import com.sphereon.di.context.UserScope
import com.sphereon.di.session.SessionScope
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Pre-generated AllLibraries AppComponent that merges all library modules.
 *
 * This component is intended for external developers who do not want to use KSP
 * to build their own components. It provides a ready-to-use dependency injection
 * setup that includes all library modules in the project.
 *
 * This class is a singleton and provides all bindings from all libraries
 * through the [MergeComponent] annotation.
 */
@OptIn(ExperimentalObjCName::class)
@ObjCName("KiwaSdkAppComponent", exact = true)
@SingleIn(AppScope::class)
@MergeComponent(AppScope::class)
@Component
abstract class KiwaSdkAppComponent(
    application: Any,
    appId: String = "all-libraries-app",
    profile: String = "profile",
    version: String = "version-example",
) : KiwaSdkAppComponentMerged,
    AbstractAppComponent(
        application,
        appId = appId,
        profile = profile,
        version = version,
        rootScopeProvider = DefaultRootScopeProvider()
    ) {
    companion object {
        fun init(
            application: Any,
            appId: String = "all-libraries-app",
            profile: String = "profile",
            version: String = "version-example"
        ): KiwaSdkAppComponentMerged {
            val appComponent = KiwaSdkAppComponent::class.create(
                application = application,
                appId = appId,
                profile = profile,
                version = version
            )
            return appComponent.initRootScopeProvider() as KiwaSdkAppComponentMerged
        }
    }
}

/**
 * Pre-generated AllLibraries UserContext Component that merges all library modules.
 *
 * This component handles user-scoped dependencies and is a child of the AppComponent.
 */
@OptIn(ExperimentalObjCName::class)
@ObjCName("KiwaSdkUserContextComponent", exact = true)
@SingleIn(UserScope::class)
@MergeComponent(UserScope::class)
@Component
abstract class KiwaSdkUserContextComponent(
    @Component
    val appComponent: KiwaSdkAppComponent
) : KiwaSdkUserContextComponentMerged

/**
 * Pre-generated AllLibraries Session Component that merges all library modules.
 *
 * This component handles session-scoped dependencies and is a child of both
 * the AppComponent and UserContextComponent.
 */
@OptIn(ExperimentalObjCName::class)
@ObjCName("KiwaSdkSessionComponent", exact = true)
@MergeComponent(SessionScope::class)
@SingleIn(SessionScope::class)
@Component
abstract class KiwaSdkSessionComponent(
    @Component
    val appComponent: KiwaSdkAppComponent,
    @Component
    val userContextComponent: KiwaSdkUserContextComponent
) : KiwaSdkSessionComponentMerged {}
