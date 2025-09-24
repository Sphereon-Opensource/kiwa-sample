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

package com.sphereon.ui.elicense.store

import io.github.irgaly.kottage.Kottage
import io.github.irgaly.kottage.KottageEnvironment
import io.github.irgaly.kottage.platform.KottageContext
import kotlinx.coroutines.CoroutineScope
import com.sphereon.di.app.IApp

actual fun kottageEnvironment(app: IApp): KottageEnvironment {
    return KottageEnvironment(KottageContext())
}

actual fun kottage(
    app: IApp,
    kottageName: String,
    scope: CoroutineScope,
    environment: KottageEnvironment?,
    directoryPath: String?
): Kottage {
    return Kottage(kottageName, directoryPath ?: "", environment ?: kottageEnvironment(app), scope)
}
