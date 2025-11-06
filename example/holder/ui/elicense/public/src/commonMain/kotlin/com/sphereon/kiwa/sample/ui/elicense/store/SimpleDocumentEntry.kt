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

package com.sphereon.kiwa.sample.ui.elicense.store

import com.sphereon.cbor.CDDL
import com.sphereon.cbor.CborBuilder
import com.sphereon.cbor.CborItem
import com.sphereon.cbor.CborMap
import com.sphereon.cbor.CborString
import com.sphereon.cbor.CborStructure
import com.sphereon.cbor.HasFromCborWithOriginal
import com.sphereon.cbor.StringLabel
import com.sphereon.cbor.toCborString
import com.sphereon.mdoc.data.device.Document
import com.sphereon.mdoc.data.device.DocumentWithKeyAlias
import com.sphereon.mdoc.data.mso.ValidityInfo

data class SimpleDocumentEntry(
    val id: String,
    override val document: Document,
    override val providerId: String,
    override val keyAlias: String,
    val certAlias: String?
) : CborStructure<SimpleDocumentEntry, CborMap<StringLabel, CborItem<*>>>(CDDL.map), DocumentWithKeyAlias {

    val validityInfo: ValidityInfo = document.MSO.validityInfo

    override fun cborBuilder(): CborBuilder<SimpleDocumentEntry> {
        return CborMap.builder(this)
            .put(ID, id.toCborString())
            .put(MDOC, document)
            .put(KMS, providerId.toCborString())
            .put(KEY_ALIAS, keyAlias.toCborString())
            .put(CERT_ALIAS, certAlias?.toCborString(), true).end()
    }

    override fun toString(): String {
        return "SimpleDocumentEntry(id='$id', document=$document, providerId='$providerId', keyAlias='$keyAlias', certAlias=$certAlias, validityInfo=$validityInfo)"
    }

    companion object : HasFromCborWithOriginal<CborMap<StringLabel, CborItem<*>>, SimpleDocumentEntry> {
        val ID = StringLabel("id")
        val MDOC = StringLabel("mdoc")
        val KMS = StringLabel("kms")
        val KEY_ALIAS = StringLabel("keyAlias")
        val CERT_ALIAS = StringLabel("certAlias")
        override fun fromCborStructureWithOriginal(
            structure: CborMap<StringLabel, CborItem<*>>,
            original: ByteArray?
        ): SimpleDocumentEntry = fromCborStructure(structure)

        override fun fromCborStructure(structure: CborMap<StringLabel, CborItem<*>>) = SimpleDocumentEntry(
            id = ID.required<CborString>(structure).value,
            document = Document.fromCborStructure(MDOC.required<CborMap<StringLabel, CborItem<*>>>(structure)),
            providerId = KMS.required<CborString>(structure).value,
            keyAlias = KEY_ALIAS.required<CborString>(structure).value,
            certAlias = CERT_ALIAS.optional<CborString>(structure)?.value
        )
    }



}
