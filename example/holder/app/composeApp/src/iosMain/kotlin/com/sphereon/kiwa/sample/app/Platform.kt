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

import platform.UIKit.UIDevice

/**
 * iOS-specific implementation of the [Platform] interface.
 *
 * This class provides platform-specific information for iOS devices,
 * implementing the common Platform interface used across different platforms
 * in the Kotlin Multiplatform project.
 */
class IOSPlatform : Platform {
    /**
     * Returns a descriptive name for the current iOS platform.
     *
     * The name format includes the system name (e.g., "iOS") and version.
     *
     * @return A string identifying the iOS platform and version
     */
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

/**
 * Platform factory function that returns the iOS-specific platform implementation.
 *
 * This is an `actual` implementation of the `expect` function declared in commonMain,
 * providing the iOS-specific platform instance when called from shared code.
 *
 * @return An [IOSPlatform] instance representing the current iOS platform
 */
actual fun getPlatform(): Platform = IOSPlatform()
