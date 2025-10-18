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

import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculeScope
import software.amazon.app.platform.presenter.molecule.MoleculeScopeFactory
import software.amazon.app.platform.presenter.molecule.launchMoleculePresenter

/**
 * App-specific template provider that works with TestAppTemplate and the app's IRootPresenter.
 * Inject [Factory] to create a new instance. Once the instance is no longer needed, call [cancel]
 * to clean up any resources.
 */
@Inject
class TemplateProvider(
    presenter: MoleculeRootPresenter,
    @Assisted private val moleculeScope: MoleculeScope,
) {

    /** The templates that should be rendered in the UI. */
    val templates: StateFlow<BaseModel> by lazy {
        moleculeScope
            .launchMoleculePresenter(
                presenter = presenter,
                input = Unit,
            )
            .model
    }

    /** Releases all resources and stops [templates] from updating further. */
    fun cancel() {
        moleculeScope.cancel()
    }

    /** Factory class to create a new instance of [TemplateProvider]. */
    @Inject
    class Factory(
        private val moleculeScopeFactory: MoleculeScopeFactory,
        private val templateProvider: (MoleculeScope) -> TemplateProvider,
    ) {
        /**
         * Creates a new instance of [TemplateProvider]. Call [TemplateProvider.cancel] when the
         * instance not needed anymore to avoid leaking resources.
         */
        fun createTemplateProvider(): TemplateProvider {
            return templateProvider(moleculeScopeFactory.createMoleculeScope())
        }
    }
}
