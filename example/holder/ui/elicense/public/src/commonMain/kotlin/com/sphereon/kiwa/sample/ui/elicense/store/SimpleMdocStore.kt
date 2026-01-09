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

import com.sphereon.crypto.core.ManagedKeyInfoType
import com.sphereon.mdoc.data.device.Document
import kotlinx.coroutines.flow.StateFlow

interface SimpleMdocStore {

    val documentsFlow: StateFlow<List<SimpleDocumentEntry>>

    suspend fun storeDocument(mdoc: Document, keyInfo: ManagedKeyInfoType<*>)
    suspend fun getDocuments(): List<SimpleDocumentEntry>
    suspend fun getDocumentById(id: String): SimpleDocumentEntry?
    suspend fun hasDocumentById(id: String): Boolean
    suspend fun hasDocument(mdoc: Document): Boolean
    suspend fun removeDocumentById(id: String): Boolean
    suspend fun removeDocument(mdoc: Document): Boolean
    suspend fun clearAll()
}
