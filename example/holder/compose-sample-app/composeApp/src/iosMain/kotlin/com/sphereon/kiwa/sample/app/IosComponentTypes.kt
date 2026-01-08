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

package com.sphereon.kiwa.sample.app

import com.sphereon.di.context.UserContextManager
import platform.UIKit.UIApplication
import software.amazon.app.platform.scope.RootScopeProvider

/**
 * Expected iOS application component interface.
 * Actual implementations are provided in platform-specific source sets (iosArm64Main, iosX64Main, iosSimulatorArm64Main)
 * where KSP-generated merged components are available.
 *
 * This is a workaround where KSP generated code on which we depend in iosMain cannot be available
 * and needs to be generated in the actual targets
 *
 */
expect abstract class IosAppComponent

/**
 * Expected iOS user context component interface.
 * Actual implementations are provided in platform-specific source sets.
 */
expect abstract class IosUserContextComponent

/**
 * Expected iOS session context component interface.
 * Actual implementations are provided in platform-specific source sets.
 */
expect abstract class IosSessionContextComponent

/**
 * Extension properties to access members of IosAppComponent.
 * These are implemented as expect/actual to allow platform-specific access.
 */
expect val IosAppComponent.appServices: AppServices
expect val IosAppComponent.application: UIApplication
expect val IosAppComponent.userContextManager: UserContextManager
expect val IosAppComponent.rootScopeProvider: RootScopeProvider
