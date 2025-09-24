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

import android.content.Context
import me.tatarka.inject.annotations.Component
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import com.sphereon.core.defaults.app.DefaultSureRootScopeProvider
import com.sphereon.di.app.AbstractAppComponent
import com.sphereon.di.app.IAppComponent
import com.sphereon.di.context.SureContextScope
import com.sphereon.di.session.SureSessionScope

/**
 * Android implementation for [AbstractAppComponent] and provides the package name as application
 * ID as well as the application and its context.
 *
 * This class is a singleton and automatically provided in the dependency graph whenever you
 * inject [AbstractAppComponent] through the [ContributesBinding] annotation.
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
        rootScopeProvider = DefaultSureRootScopeProvider()
    ),
    IAndroidAppComponent {
    init {
        require(application is Context) { "The application provided must be of type ${Context::class.java.simpleName}" }
    }

    override fun getContext(): Context = application as Context
}

interface IAndroidAppComponent : IAppComponent {
    fun getContext(): Context
}

@SingleIn(SureContextScope::class)
@MergeComponent(SureContextScope::class)
@Component
abstract class AndroidContextComponent(
    @Component
    val appComponent: AndroidAppComponent
) : AndroidContextComponentMerged

@MergeComponent(SureSessionScope::class)
@SingleIn(SureSessionScope::class)
@Component
abstract class AndroidSessionComponent(
    @Component
    val appComponent: AndroidAppComponent,
    @Component
    val contextComponent: AndroidContextComponent
) : AndroidSessionComponentMerged
