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
 */

package com.sphereon.kiwa.sample.app

import com.sphereon.kiwa.sample.ui.core.backstack.PresenterBackstackScope
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn


@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = NfcBackstackRegistry::class)
class NfcBackstackRegistryImpl : NfcBackstackRegistry {

    @Volatile
    private var backstackScope: PresenterBackstackScope? = null

    override fun setBackstackScope(scope: PresenterBackstackScope?) {
        println("NfcBackstackRegistry: Setting backstack scope: $scope")
        backstackScope = scope
    }

    override fun getBackstackScope(): PresenterBackstackScope? {
        println("NfcBackstackRegistry: Returning backstack scope: $backstackScope")
        return backstackScope
    }
}