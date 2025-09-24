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

package com.sphereon.ui.elicense.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import com.sphereon.core.api.conf.DefaultTenantMapPropertySource
import com.sphereon.di.session.SureSessionScope
import com.sphereon.ui.auth.settings.IUserPreferences
import com.sphereon.ui.core.backstack.LocalBackstackScope

@Inject
@ContributesBinding(SureSessionScope::class, boundType = ISubscriptionKeyPresenter::class)
class SubscriptionKeyPresenter(
    private val userPreferences: IUserPreferences
) : ISubscriptionKeyPresenter {

    @Composable
    override fun present(input: Unit): ISubscriptionKeyPresenter.Model {
        var subscriptionKey by remember { mutableStateOf(userPreferences.kiwaSubscriptionKey) }
        var isLoading by remember { mutableStateOf(false) }
        val backstack = checkNotNull(LocalBackstackScope.current)

        val onEvent: (ISubscriptionKeyPresenter.Event) -> Unit = { event ->
            when (event) {
                is ISubscriptionKeyPresenter.Event.SubscriptionKeyChanged -> {
                    subscriptionKey = event.key
                }

                is ISubscriptionKeyPresenter.Event.Submit -> {
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

                is ISubscriptionKeyPresenter.Event.Cancel -> {
                    backstack.pop()
                }
            }
        }

        return ISubscriptionKeyPresenter.Model.SubscriptionKeyForm(
            subscriptionKey = subscriptionKey,
            isLoading = isLoading,
            onEvent = onEvent
        )
    }
}
