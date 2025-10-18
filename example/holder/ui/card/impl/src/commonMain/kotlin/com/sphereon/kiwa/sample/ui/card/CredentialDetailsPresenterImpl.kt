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
                        MdocEngagementScreenPresenter(
                            MdocEngagementPresenter.Input(),
                            mdocEngagementQrPresenter
                        )
                    )
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
                    val value = item.elementValue?.toString() ?: ""
                    items.add(
                        CredentialDetailsPresenter.VerifiedInfoItem(
                            namespace = ns.toString(),
                            label = label,
                            value = value
                        )
                    )
                }
            }
            items.toList()
        }

        return CredentialDetailsPresenter.Model.Content(
            card = cardModel,
            selectedTab = selectedTab,
            verifiedItems = verifiedItems,
            showDeleteModal = showDelete,
            onEvent = onEvent
        )
    }

    /**
     * Wrapper presenter to handle backstack navigation with input parameters for mdoc engagement.
     */
    private class MdocEngagementScreenPresenter(
        private val input: MdocEngagementPresenter.Input,
        private val delegate: MdocEngagementPresenter
    ) : MoleculePresenter<Any, MdocEngagementPresenter.Model> {

        @Composable
        override fun present(input: Any): MdocEngagementPresenter.Model {
            return delegate.present(this.input)
        }
    }
}
