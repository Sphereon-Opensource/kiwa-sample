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

package com.sphereon.ui.auth.settings

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@OptIn(ExperimentalSettingsApi::class)
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class UserPreferences() : IUserPreferences {

    /**
     * Obviously using prefs to store a password is not a good idea. But this is just a sample app which is not about authentication itself
     *
     */
    private val settings: Settings = Settings()

    override val username: String
        get() = settings.getString(USERNAME_KEY, "")

    override val rememberMe: Boolean
        get() = settings.getBoolean(REMEMBER_ME_KEY, false)

    override val isOnboarded: Boolean
        get() = settings.getStringOrNull(PASSWORD_KEY).isNullOrEmpty().not() && username.isNotEmpty()

    override val kiwaSubscriptionKey: String
        get() = settings.getString(KIWA_SUBSCRIPTION_KEY, DEFAULT_KIWA_SUBSCRIPTION_KEY)

    override fun setUsername(username: String?) {
        if (username.isNullOrEmpty()) {
            settings.remove(
                USERNAME_KEY
            )
        } else {
            settings.putString(USERNAME_KEY, username.lowercase().trim())
        }
    }

    override fun setPassword(password: String?) {
        if (password.isNullOrEmpty()) settings.remove(PASSWORD_KEY) else settings.putString(PASSWORD_KEY, password)
    }

    override fun setRememberMe(enabled: Boolean) {
        settings.putBoolean(REMEMBER_ME_KEY, enabled)
    }

    override fun setKiwaSubscriptionKey(key: String?) {
        if (key.isNullOrEmpty()) {
            settings.remove(KIWA_SUBSCRIPTION_KEY)
        } else {
            settings.putString(KIWA_SUBSCRIPTION_KEY, key)
        }
    }

    override fun checkCredentials(username: String?, password: String?): Boolean {
        return username.isNullOrEmpty().not() && username.lowercase().trim() == this.username && password == settings.getStringOrNull(PASSWORD_KEY)
    }

    override fun clearAll() {
        settings.remove(USERNAME_KEY)
        settings.remove(PASSWORD_KEY)
        settings.remove(REMEMBER_ME_KEY)
        settings.remove(KIWA_SUBSCRIPTION_KEY)
    }

    private companion object Keys {
        const val USERNAME_KEY = "email"
        const val PASSWORD_KEY = "password"
        const val REMEMBER_ME_KEY = "remember_me"
        const val KIWA_SUBSCRIPTION_KEY = "kiwa_subscription_key"
        const val DEFAULT_KIWA_SUBSCRIPTION_KEY = "d785f1024ad84484aeb37d1b13f57934"
    }

    @ContributesTo(AppScope::class)
    interface Component {
        val userPrefs: IUserPreferences
    }
}
