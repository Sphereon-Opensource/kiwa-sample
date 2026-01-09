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

import com.sphereon.di.app.App
import com.sphereon.di.app.AppComponent
import com.sphereon.di.context.UserContextManager
import platform.UIKit.UIApplication
import software.amazon.app.platform.scope.RootScopeProvider

/**
 * Actual extension properties for iosX64 target.
 */
actual val IosAppComponent.appServices: AppServices
    get() = (this as AppServices.Component).appServices

actual val IosAppComponent.application: UIApplication
    get() = (this as App).application as UIApplication

actual val IosAppComponent.userContextManager: UserContextManager
    get() = (this as AppComponent).userContextManager

actual val IosAppComponent.rootScopeProvider: RootScopeProvider
    get() = (this as App).rootScopeProvider as RootScopeProvider
