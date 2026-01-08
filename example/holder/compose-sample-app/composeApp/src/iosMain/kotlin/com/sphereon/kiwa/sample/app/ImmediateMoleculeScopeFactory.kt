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
 *
 */

package com.sphereon.kiwa.sample.app

import app.cash.molecule.RecompositionMode
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import software.amazon.app.platform.presenter.molecule.MoleculeScope
import software.amazon.app.platform.presenter.molecule.MoleculeScopeFactory

/**
 * A custom [MoleculeScopeFactory] for iOS that uses [RecompositionMode.Immediate] instead of
 * [RecompositionMode.ContextClock].
 *
 * On iOS, the default [IosMoleculeScopeFactory] uses ContextClock mode, which ties recomposition
 * to CADisplayLink frame ticks. When showing static content (like a QR code), CADisplayLink stops
 * firing, preventing Molecule from processing state changes from async flows (like BLE events).
 *
 * This factory uses Immediate mode, which recomposes immediately when state changes occur,
 * ensuring UI updates happen promptly regardless of the frame clock state.
 */
class ImmediateMoleculeScopeFactory(
    private val coroutineScopeFactory: () -> CoroutineScope
) : MoleculeScopeFactory {

    override fun createMoleculeScope(): MoleculeScope {
        return MoleculeScope(coroutineScopeFactory(), RecompositionMode.Immediate)
    }

    override fun createMoleculeScopeFromCoroutineScope(
        coroutineScope: CoroutineScope,
        coroutineContext: CoroutineContext
    ): MoleculeScope {
        return MoleculeScope(coroutineScope, RecompositionMode.Immediate)
    }
}
