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

package com.sphereon.kiwa.sample.ui.elicense.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sphereon.core.api.conf.DefaultTenantMapPropertySource
import com.sphereon.di.session.SessionScope
import com.sphereon.kiwa.sample.ui.auth.settings.UserPreferences
import com.sphereon.kiwa.sample.ui.core.backstack.LocalBackstackScope
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@Inject
@ContributesBinding(SessionScope::class, boundType = SubscriptionKeyPresenter::class)
class SubscriptionKeyPresenterImpl(
    private val userPreferences: UserPreferences
) : SubscriptionKeyPresenter {

    @Composable
    override fun present(input: Unit): SubscriptionKeyPresenter.Model {
        var subscriptionKey by remember { mutableStateOf(userPreferences.kiwaSubscriptionKey) }
        var isLoading by remember { mutableStateOf(false) }
        val backstack = checkNotNull(LocalBackstackScope.current)

        val onEvent: (SubscriptionKeyPresenter.Event) -> Unit = { event ->
            when (event) {
                is SubscriptionKeyPresenter.Event.SubscriptionKeyChanged -> {
                    subscriptionKey = event.key
                }

                is SubscriptionKeyPresenter.Event.Submit -> {
                    isLoading = true
                    try {
                        // Save to user preferences
                        userPreferences.setKiwaSubscriptionKey(subscriptionKey)

                        // Set the property in DefaultTenantMapPropertySource
                        DefaultTenantMapPropertySource.addProperty("kiwa.subscription.key.acc", subscriptionKey)

                        // Navigate back
                        backstack.pop()
                    } finally {
                        isLoading = false
                    }
                }

                is SubscriptionKeyPresenter.Event.Cancel -> {
                    backstack.pop()
                }
            }
        }

        return SubscriptionKeyPresenter.Model.SubscriptionKeyForm(
            subscriptionKey = subscriptionKey,
            isLoading = isLoading,
            onEvent = onEvent
        )
    }
}
