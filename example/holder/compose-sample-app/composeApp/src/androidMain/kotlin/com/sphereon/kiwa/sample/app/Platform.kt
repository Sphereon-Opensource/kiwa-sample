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

import android.os.Build

/**
 * Android-specific implementation of the [Platform] interface.
 *
 * This class provides platform-specific information for Android devices,
 * implementing the common Platform interface used across different platforms
 * in the Kotlin Multiplatform project.
 *
 * The platform name includes both the operating system identifier ("Android")
 * and the specific API level (SDK_INT) of the current Android device.
 */
class AndroidPlatform : Platform {
    /**
     * Returns a descriptive name for the current Android platform.
     *
     * The name format is "Android {SDK_INT}" where SDK_INT represents
     * the Android API level of the device (e.g., "Android 34" for Android 14).
     *
     * @return A string identifying the Android platform and API level
     */
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

/**
 * Platform factory function that returns the Android-specific platform implementation.
 *
 * This is an `actual` implementation of the `expect` function declared in commonMain,
 * providing the Android-specific platform instance when called from shared code.
 *
 * @return An [AndroidPlatform] instance representing the current Android platform
 */
actual fun getPlatform(): Platform = AndroidPlatform()
