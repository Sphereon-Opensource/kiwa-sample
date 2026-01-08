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

import software.amazon.app.platform.renderer.RendererComponent
import software.amazon.app.platform.renderer.RendererFactory
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

/**
 * Contributes [RendererComponent.Parent] to the iOS AppScope.
 *
 * This interface provides the factory method for creating [IosRendererComponent]
 * which merges all @ContributesRenderer bindings into the iOS component hierarchy.
 */
@ContributesTo(AppScope::class)
interface IosRendererComponentParent : RendererComponent.Parent {
    override fun rendererComponent(factory: RendererFactory): RendererComponent {
        return IosRendererComponent::class.createComponent(this as IosAppComponent, factory)
    }
}
