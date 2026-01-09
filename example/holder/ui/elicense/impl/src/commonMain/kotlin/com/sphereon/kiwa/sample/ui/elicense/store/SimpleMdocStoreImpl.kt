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

import com.sphereon.core.api.context.SessionExecution
import com.sphereon.core.api.encodeToHex
import com.sphereon.crypto.core.ManagedKeyInfoType
import com.sphereon.crypto.core.generic.hash
import com.sphereon.di.app.App
import com.sphereon.di.session.SessionScope
import com.sphereon.mdoc.data.device.Document
import io.github.irgaly.kottage.Kottage
import io.github.irgaly.kottage.KottageEnvironment
import io.github.irgaly.kottage.KottageListDirection
import io.github.irgaly.kottage.add
import io.github.irgaly.kottage.getOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * A concrete implementation of the ISimpleMdocStore interface that provides functionality to
 * manage and store mobile documents (mDocs) using Kottage for storage management.
 *
 * This class operates within the SessionScope using dependency injection and provides a
 * stateful flow of document entries for reactive access.
 *
 * @property app An instance of the App interface used for accessing application-specific
 * functionalities and dependencies.
 * @constructor Initializes the SimpleMdocStore with Kottage storage and a coroutine scope for its operations.
 */
@Inject
@SingleIn(SessionScope::class)
@ContributesBinding(SessionScope::class, boundType = SimpleMdocStore::class)
class SimpleMdocStoreImpl(val app: App, execution: SessionExecution) : SimpleMdocStore {

    val log = execution.log.logManager.withTag("SimpleMdocStore")

    /**
     * A [CoroutineScope] used for managing coroutines within this class. This scope is tied to the
     * lifecycle of the enclosing context and should be used for executing asynchronous tasks
     * on the [Dispatchers.IO] dispatcher.
     *
     * [Dispatchers.IO] is typically used for offloading blocking IO operations, such as disk or
     * network access, ensuring these tasks do not block the main thread.
     *
     */
    val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    /**
     * The `environment` is an instance of `KottageEnvironment` for the application.
     *
     * It provides the runtime environment configuration required by the `Kottage` library
     * to function within the context of the given application (`app`).
     */
    val environment = kottageEnvironment(app)

    /**
     * An instance of `Kottage` configured for use within the `SimpleMdocStore` class.
     *
     * This variable is initialized with parameters including application context, a unique Kottage name,
     * coroutine scope, environment, and an optional directory path.
     */
    val kottage = kottage(
        app = app,
        kottageName = "sample_app",
        scope = scope,
        environment = environment,
        directoryPath = null
    )

    /**
     * Represents the storage instance used to manage documents associated with the `SimpleMdocStore`.
     * This storage is utilized for operations such as storing, retrieving, checking existence, and removing documents.
     *
     * The storage is identified by the name "mdoc_storage" and serves as the underlying data store for the document management
     * functionalities provided by the `SimpleMdocStore` class.
     */
    private val storage = kottage.storage("mdoc_storage")

    /**
     * A reference to a Kottage list specifically used for managing document entries.
     *
     * The primary use cases include:
     * - Storing a new document entry with metadata in the list.
     * - Retrieving all or specific subsets of document entries from the list.
     * - Querying a document entry using its unique identifier directly from the storage.
     */
    private val list = storage.list("entries")

    /**
     * Represents the current internal state of documents managed by the `SimpleMdocStore`.
     *
     * This state is a reactive stream of a list of `SimpleDocumentEntry` objects. It is backed by a
     * `MutableStateFlow`, allowing real-time updates and reactivity within the application.
     *
     * The `documentsState` is updated whenever documents are added, removed, or modified within the store
     * using functions like `storeDocument`, `removeDocumentById`, or similar operations.
     *
     * Exposed via the `documentsFlow` function as a `StateFlow` to provide an immutable, observable stream
     * of the current document state. This design supports integration with declarative UI frameworks or
     * other reactive flows.
     *
     */
    private val documentsState = MutableStateFlow<List<SimpleDocumentEntry>>(emptyList())

    /**
     * Provides a flow of the current state of documents.
     *
     * This function returns a [StateFlow] that emits updates to the list of documents managed by the store.
     * It allows observing document changes in real-time to ensure the UI or other dependent components
     * stay in sync with the latest document state.
     *
     * @return A [StateFlow] emitting updates to the list of [SimpleDocumentEntry] instances.
     */
    override val documentsFlow = documentsState.asStateFlow()

    init {
        // Load initial state
        scope.launch {
            try {
                val docs = getDocuments()
                documentsState.value = docs
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Stores the given document and associates it with the provided key information.
     *
     * @param mdoc The document to be stored. Represents a self-contained data structure containing
     *        information related to a specific document type, signed issuer data, and optionally device
     *        signed data and associated errors.
     * @param keyInfo The managed key information to associate with the document. This includes details
     *        about the cryptographic key (e.g., alias and providerId) and its associated key management system.
     */
    override suspend fun storeDocument(mdoc: Document, keyInfo: ManagedKeyInfoType<*>) = withContext(Dispatchers.IO) {
        val docId = determineId(mdoc)
        val x509Cert = keyInfo.key.getX509Certificate()
        val documentEntry = SimpleDocumentEntry(
            id = docId,
            document = mdoc,
            providerId = keyInfo.providerId,
            keyAlias = keyInfo.alias,
            certAlias = x509Cert?.let { keyInfo.alias },
        )
        log.info("Storing document with id ${documentEntry.id}: $documentEntry")
        val encodedEntry = documentEntry.encodeCbor()
        list.add(documentEntry.id, encodedEntry)
        // Update state directly with the new entry instead of re-reading from database
        // This ensures the UI sees the update immediately without database transaction timing issues
        val currentDocs = documentsState.value
        if (currentDocs.none { it.id == documentEntry.id }) {
            documentsState.value = currentDocs + documentEntry
        }
        log.info("Document ${documentEntry.id} stored successfully")
    }

    /**
     * Determines a unique identifier for a given document by encoding it into CBOR format,
     * hashing the result, and converting the hash to a hexadecimal string.
     *
     * @param mdoc The document for which the unique identifier is to be determined.
     * @return A string representing the unique identifier of the document.
     */
    private fun determineId(mdoc: Document): String {
        val cbor = mdoc.encodeCbor()
        val hashResult = hash(cbor)
        val result = hashResult.encodeToHex()
        return result
    }

    /**
     * Retrieves all documents as a list of SimpleDocumentEntry objects.
     *
     * This method fetches all entries from the underlying list with no pagination,
     * processes them, and decodes them to produce SimpleDocumentEntry instances.
     *
     * @return A list of SimpleDocumentEntry objects representing the stored documents.
     */
    override suspend fun getDocuments(): List<SimpleDocumentEntry> =
        // One big page, demo app!
        list.getPageFrom(
            positionId = null,
            pageSize = Long.MAX_VALUE,
            direction = KottageListDirection.Forward
        ).items.map { SimpleDocumentEntry.decodeCbor(it.value()) }

    /**
     * Retrieves a document by its unique identifier from the storage.
     *
     * @param id The unique identifier of the document to be retrieved.
     * @return The document entry if found, or null if no document exists with the given identifier.
     */
    override suspend fun getDocumentById(id: String): SimpleDocumentEntry? = list.storage.getOrNull<ByteArray>(
        id
    )?.let { SimpleDocumentEntry.decodeCbor(it) }

    /**
     * Checks if a document with the specified ID exists in the storage.
     *
     * @param id The unique identifier of the document to check for existence.
     * @return Returns true if a document with the specified ID exists, otherwise false.
     */
    override suspend fun hasDocumentById(id: String): Boolean = storage.exists(id)

    /**
     * Checks if the given document exists in the storage.
     *
     * @param mdoc the `Document` to be checked for existence.
     * @return `true` if the document exists, `false` otherwise.
     */
    override suspend fun hasDocument(mdoc: Document): Boolean = withContext(Dispatchers.IO) {
        val id = determineId(mdoc)
        val result = hasDocumentById(id)
        result
    }

    /**
     * Removes a document from the storage by its unique identifier.
     * After the document is removed, the documents state is refreshed
     * to reflect the current list of documents.
     *
     * @param id The unique identifier of the document to be removed.
     */
    override suspend fun removeDocumentById(id: String) = storage.remove(id).also {
        log.info("Removed document with id $id")
        // Emit refresh
        documentsState.value = getDocuments()
    }

    /**
     * Removes the specified document from the storage.
     *
     * This method identifies the document's unique ID via the `determineId` method
     * and subsequently removes it using `removeDocumentById`. The removal process
     * triggers an update in the documents state to reflect the changes.
     *
     * @param mdoc The document to be removed.
     */
    override suspend fun removeDocument(mdoc: Document) = removeDocumentById(determineId(mdoc))

    /**
     * Clears all documents from the storage.
     *
     * This method removes all stored documents and refreshes the documents state
     * to reflect the empty state. Useful for account deletion scenarios.
     */
    override suspend fun clearAll() {
        log.info("Removing all documents")
        storage.removeAll()

        // Emit refresh to empty state*/
        documentsState.value = emptyList()
    }

    /**
     * Represents a component that contributes to the SessionScope.
     * This component provides access to specific functionalities or services
     * required within the lifecycle scope of a plugin or service in the application.
     *
     * It helps in managing dependencies and isolating resources appropriately.
     */
    @ContributesTo(SessionScope::class)
    interface Component {
        /**
         * Provides an instance of `ISimpleMdocStore` for managing operations related to
         * Mobile Identification Documents (mDocs). This store is responsible for storing,
         * retrieving, checking, and removing mDocs, as well as managing a flow of documents' states.
         *
         * The scope in which this store is provided is tied to a specific session lifecycle,
         * as defined by the `SessionScope`.
         */
        val mdocStore: SimpleMdocStore
    }
}

/**
 * Creates a platform-specific instance of the KottageEnvironment.
 *
 * @param app The application instance, providing the app-specific context and properties.
 * @return A KottageEnvironment object configured for the provided application context.
 */
expect fun kottageEnvironment(app: App): KottageEnvironment

/**
 * Creates a Kottage instance with the specified configuration.
 *
 * @param app An instance of the application [App] that provides application context and other properties.
 * @param kottageName The name of the Kottage instance to be created.
 * @param scope A [CoroutineScope] that controls the lifecycle of the created Kottage instance.
 * @param environment An optional [KottageEnvironment] to configure the Kottage instance; if null, a default environment will be used.
 * @param directoryPath An optional directory path for storing data; if null, a default path will be determined.
 * @return A configured [Kottage] instance.
 */
expect fun kottage(
    app: App,
    kottageName: String,
    scope: CoroutineScope,
    environment: KottageEnvironment? = null,
    directoryPath: String? = null
): Kottage
