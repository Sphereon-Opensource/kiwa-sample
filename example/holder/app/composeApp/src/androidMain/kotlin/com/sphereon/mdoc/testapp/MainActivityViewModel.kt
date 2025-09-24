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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import com.sphereon.di.session.SureSessionScope

/**
 * `ViewModel` that hosts the stream of templates and survives configuration changes. Note that we
 * use [application] to get access to the root scope.
 */
class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val componentFlow = MainActivity.sessionComponentFlow
    private val templateProvider: TemplateProvider = (componentFlow.value as TemplateProviderComponent).templateProviderFactory.createTemplateProvider()

    /** The stream of templates that are rendered by [MainActivity]. */
    val templates: StateFlow<BaseModel> = templateProvider.templates

    override fun onCleared() {
        templateProvider.cancel()
    }

    /** Component interface to give us access to objects from the app component. */
    @ContributesTo(SureSessionScope::class)
    interface TemplateProviderComponent {
        /** Gives access to the [TemplateProvider.Factory] from the object graph. */
        val templateProviderFactory: TemplateProvider.Factory
    }
}
