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

package com.sphereon.kiwa.sample.ui.elicense.store

import com.sphereon.di.app.App
import io.github.irgaly.kottage.Kottage
import io.github.irgaly.kottage.KottageEnvironment
import io.github.irgaly.kottage.platform.KottageContext
import kotlinx.coroutines.CoroutineScope
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual fun kottageEnvironment(app: App): KottageEnvironment {
    return KottageEnvironment(KottageContext())
}

/**
 * Returns the iOS Documents directory path for persistent storage.
 */
private fun getDocumentsDirectoryPath(): String {
    val fileManager = NSFileManager.defaultManager
    val urls = fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
    @Suppress("UNCHECKED_CAST")
    val documentsUrl = (urls as List<NSURL>).firstOrNull() ?: error("Could not find Documents directory")
    return documentsUrl.path ?: error("Could not get path from Documents URL")
}

actual fun kottage(
    app: App,
    kottageName: String,
    scope: CoroutineScope,
    environment: KottageEnvironment?,
    directoryPath: String?
): Kottage {
    val path = directoryPath ?: getDocumentsDirectoryPath()
    return Kottage(kottageName, path, environment ?: kottageEnvironment(app), scope)
}
