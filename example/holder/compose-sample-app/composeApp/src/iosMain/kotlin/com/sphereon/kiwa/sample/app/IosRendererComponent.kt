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

import kotlin.reflect.KClass
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import software.amazon.app.platform.renderer.RendererComponent
import software.amazon.app.platform.renderer.RendererFactory
import software.amazon.app.platform.renderer.RendererScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * iOS-specific renderer component that merges all @ContributesRenderer bindings
 * into the renderer scope.
 *
 * This component is analogous to RendererComponentFinalAndroidAppComponent on Android,
 * providing the renderer factory with all discovered renderers.
 */
@MergeComponent(scope = RendererScope::class)
@SingleIn(scope = RendererScope::class)
abstract class IosRendererComponent(
    @Component
    val parentComponent: IosAppComponent,
    @get:Provides
    val factory: RendererFactory,
) : RendererComponent

/**
 * Factory function to instantiate the renderer component.
 * This is necessary because `iosMain` is a shared source folder and generated
 * components live in the x64, arm64 and simulatorArm64 source folders.
 */
@MergeComponent.CreateComponent
expect fun KClass<IosRendererComponent>.createComponent(
    parentComponent: IosAppComponent,
    factory: RendererFactory,
): IosRendererComponent
