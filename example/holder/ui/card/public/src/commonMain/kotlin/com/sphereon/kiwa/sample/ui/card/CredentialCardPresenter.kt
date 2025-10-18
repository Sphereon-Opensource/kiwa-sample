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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import com.sphereon.mdoc.data.device.Document
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter

/**
 * Credential Card presenter contract using a unified input/model.
 * Renderer decides which layout to render based on [Variant].
 */
interface CredentialCardPresenter : MoleculePresenter<CredentialCardPresenter.Input, CredentialCardPresenter.Model> {

    @Immutable
    enum class Variant { Mini, Large }

    @Immutable
    data class Input(
        val variant: Variant,
        val backgroundColor: Long? = null,
        val backgroundImage: ImageBitmap? = null,
        val issuerLogo: ImageBitmap? = null,
        // Large-only fields (optional for Mini)
        val title: String? = null,
        val subtitle: String? = null,
        val issuerName: String? = null,
        val expires: String? = null,
        val statusText: String = "Status",
        // Moved up from list item to card-level
        val validFrom: String = "",
        val statusColor: Long = 0xFF00C249,
    ) {
        // Mapping helpers between Input and Model
        fun toModel(): Model = Model(
            variant = this.variant,
            backgroundColor = this.backgroundColor,
            backgroundImage = this.backgroundImage,
            issuerLogo = this.issuerLogo,
            title = this.title,
            subtitle = this.subtitle,
            issuerName = this.issuerName,
            expires = this.expires,
            statusText = this.statusText,
            validFrom = this.validFrom,
            statusColor = this.statusColor,
        )

        companion object {
            fun fromDocument(document: Document, variant: Variant = Variant.Mini): Input {
                val mso = document.MSO
                return Input(
                    variant = variant,
                    title = mso.docType.toString(),
                    subtitle = "Example",
                    issuerName = "Sample issuer",
                    expires = mso.validityInfo.validUntil.toString(),
                    validFrom = mso.validityInfo.validFrom.toString(),
                    statusColor = 0xFF00C249,
                )
            }
        }
    }

    @Immutable
    data class Model(
        val variant: Variant,
        val backgroundColor: Long? = null,
        val backgroundImage: ImageBitmap? = null,
        val issuerLogo: ImageBitmap? = null,
        // Large-only fields (optional for Mini)
        val title: String? = null,
        val subtitle: String? = null,
        val issuerName: String? = null,
        val expires: String? = null,
        val statusText: String = "Status",
        // Moved up from list item to card-level
        val validFrom: String = "",
        val statusColor: Long = 0xFFC4C4C4,
    ) : BaseModel {

        fun toInput(): Input = Input(
            variant = this.variant,
            backgroundColor = this.backgroundColor,
            backgroundImage = this.backgroundImage,
            issuerLogo = this.issuerLogo,
            title = this.title,
            subtitle = this.subtitle,
            issuerName = this.issuerName,
            expires = this.expires,
            statusText = this.statusText,
            validFrom = this.validFrom,
            statusColor = this.statusColor,
        )
    }
}
