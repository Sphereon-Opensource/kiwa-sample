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
import com.sphereon.mdoc.data.device.Document
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter

/**
 * List item presenter contract for credential ListView row, including mini card and info column.
 */
interface CredentialListItemPresenter : MoleculePresenter<CredentialListItemPresenter.Input, CredentialListItemPresenter.Model> {
    @Immutable
    sealed interface Input {
        @Immutable
        data class Row(
            val card: CredentialCardPresenter.Input,
            val document: Document,
        ) : Input
    }

    sealed interface Model : BaseModel {
        @Immutable
        data class Row(
            val card: CredentialCardPresenter.Model,
            val document: Document,
            val onDelete: (() -> Unit)? = null,
        ) : Model
    }
}
