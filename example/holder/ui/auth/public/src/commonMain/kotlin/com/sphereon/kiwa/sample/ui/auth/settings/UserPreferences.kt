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

package com.sphereon.kiwa.sample.ui.auth.settings

/**
 * Provides read-only access to user preference data
 */
interface IUserPreferencesData {
    val username: String
    val rememberMe: Boolean
    val isOnboarded: Boolean
    val kiwaSubscriptionKey: String
}

/**
 * Manages credential-related operations
 */
interface IUserCredentialsManager {
    fun setUsername(username: String?)
    fun setPassword(password: String?)
    fun checkCredentials(username: String?, password: String?): Boolean
}

/**
 * Manages user settings and preferences
 */
interface IUserSettingsManager {
    fun setRememberMe(enabled: Boolean)
    fun setKiwaSubscriptionKey(key: String?)
    fun clearAll()
}

/**
 * Complete user preferences interface combining all functionality
 */
interface UserPreferences : IUserPreferencesData, IUserCredentialsManager, IUserSettingsManager
