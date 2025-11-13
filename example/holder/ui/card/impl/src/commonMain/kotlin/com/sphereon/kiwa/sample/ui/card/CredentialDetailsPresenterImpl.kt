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

package com.sphereon.kiwa.sample.ui.card

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sphereon.cbor.CborArray
import com.sphereon.cbor.CborItem
import com.sphereon.cbor.CborMap
import com.sphereon.di.session.SessionScope
import com.sphereon.kiwa.sample.ui.core.backstack.LocalBackstackScope
import com.sphereon.kiwa.sample.ui.elicense.engagement.qr.MdocEngagementPresenter
import com.sphereon.kiwa.sample.ui.elicense.store.SimpleMdocStore
import kotlinx.coroutines.runBlocking
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@Inject
@ContributesBinding(SessionScope::class, boundType = CredentialDetailsPresenter::class)
class CredentialDetailsPresenterImpl(
    private val storage: SimpleMdocStore,
    private val mdocEngagementQrPresenter: MdocEngagementPresenter
) : CredentialDetailsPresenter {

    @Composable
    override fun present(input: CredentialDetailsPresenter.Input): CredentialDetailsPresenter.Model {
        var selectedTab by remember { mutableStateOf(CredentialDetailsPresenter.Tab.VerifiedInfo) }
        var showDelete by remember { mutableStateOf(false) }
        var fullScreenImage by remember { mutableStateOf<Pair<ByteArray, String>?>(null) }
        val backstack = checkNotNull(LocalBackstackScope.current)

        val onEvent: (CredentialDetailsPresenter.Event) -> Unit = { event ->
            when (event) {
                is CredentialDetailsPresenter.Event.SelectTab -> selectedTab = event.tab
                is CredentialDetailsPresenter.Event.Back -> backstack.pop()
                is CredentialDetailsPresenter.Event.DeleteClicked -> showDelete = true
                is CredentialDetailsPresenter.Event.CancelDelete -> showDelete = false
                is CredentialDetailsPresenter.Event.ConfirmDelete -> {
                    // Remove and go back
                    // Using a snapshot side-effect here; storage is synchronous signature suspend, but presenter
                    // contract is composable. Keep it simple by removing directly; in real app consider launching.
                    // For now, remove synchronously via remember or launch? We'll invoke immediately assuming no heavy IO.
                    // If suspend is required, the store will provide a blocking behavior if on JVM/desktop; acceptable here.
                    // Alternatively, caller could confirm delete from list, but we implement here per request.
                    // Remove, then pop
                    runBlocking { storage.removeDocument(input.document) }
                    showDelete = false
                    backstack.pop()
                }

                is CredentialDetailsPresenter.Event.AttendedPresentation -> {
                    backstack.push(
                        MdocEngagementScreenPresenter(mdocEngagementQrPresenter)
                    )
                }

                is CredentialDetailsPresenter.Event.ViewImage -> {
                    fullScreenImage = event.imageData to event.label
                }

                is CredentialDetailsPresenter.Event.CloseImage -> {
                    fullScreenImage = null
                }
            }
        }

        val cardModel = CredentialCardPresenter.Input.fromDocument(
            document = input.document,
            variant = CredentialCardPresenter.Variant.Large
        ).toModel()

        val verifiedItems = remember(input.document) {
            val all = input.document.issuerSigned.getAllIssuerSignedItems()
            val items = mutableListOf<CredentialDetailsPresenter.VerifiedInfoItem>()
            all?.forEach { (ns, list) ->
                list.forEach { item ->
                    val label = item.elementIdentifier.toString()
                    items.add(convertToVerifiedInfoItem(ns.toString(), label, item.elementValue))
                }
            }
            items.toList()
        }

        return CredentialDetailsPresenter.Model.Content(
            card = cardModel,
            selectedTab = selectedTab,
            verifiedItems = verifiedItems,
            showDeleteModal = showDelete,
            fullScreenImage = fullScreenImage,
            onEvent = onEvent
        )
    }

    /**
     * Recursively converts a CBOR value into a VerifiedInfoItem structure.
     * Handles nested CborMap, CborArray, Map, and Array types.
     */
    private fun convertToVerifiedInfoItem(
        namespace: String,
        label: String,
        value: Any?
    ): CredentialDetailsPresenter.VerifiedInfoItem {
        return when (value) {
            is CborMap<*, *> -> {
                // CborMap has a value property that is a Map
                val children = mutableListOf<CredentialDetailsPresenter.VerifiedInfoItem>()
                value.value.forEach { (key, mapValue) ->
                    val keyStr = key.toString()
                    children.add(convertToVerifiedInfoItem(namespace, keyStr, mapValue))
                }
                CredentialDetailsPresenter.VerifiedInfoItem(
                    namespace = namespace,
                    label = label,
                    value = null,
                    children = children.ifEmpty { null }
                )
            }

            is Map<*, *> -> {
                val children = mutableListOf<CredentialDetailsPresenter.VerifiedInfoItem>()
                value.forEach { (key, mapValue) ->
                    val keyStr = key.toString()
                    children.add(convertToVerifiedInfoItem(namespace, keyStr, mapValue))
                }
                CredentialDetailsPresenter.VerifiedInfoItem(
                    namespace = namespace,
                    label = label,
                    value = null,
                    children = children.ifEmpty { null }
                )
            }

            is CborArray<*> -> {
                // CborArray has a value property that is an Array
                val children = mutableListOf<CredentialDetailsPresenter.VerifiedInfoItem>()
                val arrayValues = value.value
                arrayValues.forEachIndexed { index, arrayValue ->
                    // For single item, no label; for multiple items, use 1-based numbering
                    val itemLabel = if (arrayValues.size == 1) "" else "${index + 1}"
                    children.add(convertToVerifiedInfoItem(namespace, itemLabel, arrayValue))
                }
                CredentialDetailsPresenter.VerifiedInfoItem(
                    namespace = namespace,
                    label = label,
                    value = null,
                    children = children.ifEmpty { null }
                )
            }

            is Array<*> -> {
                val children = mutableListOf<CredentialDetailsPresenter.VerifiedInfoItem>()
                value.forEachIndexed { index, arrayValue ->
                    // For single item, no label; for multiple items, use 1-based numbering
                    val itemLabel = if (value.size == 1) "" else "${index + 1}"
                    children.add(convertToVerifiedInfoItem(namespace, itemLabel, arrayValue))
                }
                CredentialDetailsPresenter.VerifiedInfoItem(
                    namespace = namespace,
                    label = label,
                    value = null,
                    children = children.ifEmpty { null }
                )
            }

            is List<*> -> {
                val children = mutableListOf<CredentialDetailsPresenter.VerifiedInfoItem>()
                value.forEachIndexed { index, arrayValue ->
                    // For single item, no label; for multiple items, use 1-based numbering
                    val itemLabel = if (value.size == 1) "" else "${index + 1}"
                    children.add(convertToVerifiedInfoItem(namespace, itemLabel, arrayValue))
                }
                CredentialDetailsPresenter.VerifiedInfoItem(
                    namespace = namespace,
                    label = label,
                    value = null,
                    children = children.ifEmpty { null }
                )
            }

            is ByteArray -> {
                // Handle ByteArray specially
                if (value.size < 16) {
                    // Short byte arrays: convert to UTF-8 string
                    val stringValue = try {
                        value.decodeToString()
                    } catch (e: Exception) {
                        // If UTF-8 decoding fails, show as hex
                        value.joinToString(" ") { byte -> "%02X".format(byte) }
                    }
                    CredentialDetailsPresenter.VerifiedInfoItem(
                        namespace = namespace,
                        label = label,
                        value = stringValue,
                        children = null
                    )
                } else {
                    // Longer byte arrays: check if it's an image
                    if (isImage(value)) {
                        CredentialDetailsPresenter.VerifiedInfoItem(
                            namespace = namespace,
                            label = label,
                            value = null,
                            children = null,
                            imageData = value
                        )
                    } else {
                        // Not an image, show byte count
                        CredentialDetailsPresenter.VerifiedInfoItem(
                            namespace = namespace,
                            label = label,
                            value = "[Binary data: ${value.size} bytes]",
                            children = null
                        )
                    }
                }
            }

            is CborItem<*> -> {
                // CborItem wraps a primitive value
                val itemValue = value.value
                if (itemValue is Map<*, *> || itemValue is Array<*> || itemValue is List<*> || itemValue is ByteArray) {
                    convertToVerifiedInfoItem(namespace, label, itemValue)
                } else {
                    CredentialDetailsPresenter.VerifiedInfoItem(
                        namespace = namespace,
                        label = label,
                        value = itemValue?.toString() ?: "",
                        children = null
                    )
                }
            }

            null -> {
                CredentialDetailsPresenter.VerifiedInfoItem(
                    namespace = namespace,
                    label = label,
                    value = "",
                    children = null
                )
            }

            else -> {
                // For primitive types (String, Number, Boolean, etc.)
                CredentialDetailsPresenter.VerifiedInfoItem(
                    namespace = namespace,
                    label = label,
                    value = value.toString(),
                    children = null
                )
            }
        }
    }

    /**
     * Checks if a ByteArray is an image by examining magic bytes.
     * Supports JPEG, PNG, GIF, WebP, and BMP formats.
     */
    private fun isImage(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false

        return when {
            // JPEG: FF D8 FF
            bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte() -> true

            // PNG: 89 50 4E 47
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() -> true

            // GIF: 47 49 46 38
            bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte() && bytes[3] == 0x38.toByte() -> true

            // WebP: check for "RIFF" and "WEBP"
            bytes.size >= 12 &&
                    bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte() && bytes[3] == 0x46.toByte() &&
                    bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte() && bytes[10] == 0x42.toByte() && bytes[11] == 0x50.toByte() -> true

            // BMP: 42 4D
            bytes[0] == 0x42.toByte() && bytes[1] == 0x4D.toByte() -> true

            else -> false
        }
    }

    private class MdocEngagementScreenPresenter(
        private val delegate: MdocEngagementPresenter
    ) : MoleculePresenter<Any, MdocEngagementPresenter.Model> {

        @Composable
        override fun present(input: Any): MdocEngagementPresenter.Model {
            return delegate.present(MdocEngagementPresenter.Input)
        }
    }
}
