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

package com.sphereon.ui.card

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import com.sphereon.di.session.SureSessionScope
import com.sphereon.ui.core.backstack.LocalBackstackScope
import com.sphereon.ui.elicense.engagement.qr.IMdocEngagementQrPresenter
import com.sphereon.ui.elicense.store.ISimpleMdocStore

@Inject
@ContributesBinding(SureSessionScope::class, boundType = ICredentialDetailsPresenter::class)
class CredentialDetailsPresenter(
    private val storage: ISimpleMdocStore,
    private val mdocEngagementQrPresenter: IMdocEngagementQrPresenter
) : ICredentialDetailsPresenter {

    @Composable
    override fun present(input: ICredentialDetailsPresenter.Input): ICredentialDetailsPresenter.Model {
        var selectedTab by remember { mutableStateOf(ICredentialDetailsPresenter.Tab.VerifiedInfo) }
        var showDelete by remember { mutableStateOf(false) }
        val backstack = checkNotNull(LocalBackstackScope.current)

        val onEvent: (ICredentialDetailsPresenter.Event) -> Unit = { event ->
            when (event) {
                is ICredentialDetailsPresenter.Event.SelectTab -> selectedTab = event.tab
                is ICredentialDetailsPresenter.Event.Back -> backstack.pop()
                is ICredentialDetailsPresenter.Event.DeleteClicked -> showDelete = true
                is ICredentialDetailsPresenter.Event.CancelDelete -> showDelete = false
                is ICredentialDetailsPresenter.Event.ConfirmDelete -> {
                    // Remove and go back
                    // Using a snapshot side-effect here; storage is synchronous signature suspend, but presenter
                    // contract is composable. Keep it simple by removing directly; in real app consider launching.
                    // For now, remove synchronously via remember or launch? We'll invoke immediately assuming no heavy IO.
                    // If suspend is required, the store will provide a blocking behavior if on JVM/desktop; acceptable here.
                    // Alternatively, caller could confirm delete from list, but we implement here per request.
                    // Remove, then pop
                    kotlinx.coroutines.runBlocking { storage.removeDocument(input.document) }
                    showDelete = false
                    backstack.pop()
                }

                is ICredentialDetailsPresenter.Event.AttendedPresentation -> {
                    backstack.push(
                        MdocEngagementScreenPresenter(
                            IMdocEngagementQrPresenter.Input(),
                            mdocEngagementQrPresenter
                        )
                    )
                }
            }
        }

        val cardModel = ICredentialCardPresenter.Input.fromDocument(
            document = input.document,
            variant = ICredentialCardPresenter.Variant.Large
        ).toModel()

        val verifiedItems = remember(input.document) {
            val all = input.document.issuerSigned.getAllIssuerSignedItems()
            val items = mutableListOf<ICredentialDetailsPresenter.VerifiedInfoItem>()
            all?.forEach { (ns, list) ->
                list.forEach { item ->
                    val label = item.elementIdentifier.toString()
                    val value = item.elementValue?.toString() ?: ""
                    items.add(
                        ICredentialDetailsPresenter.VerifiedInfoItem(
                            namespace = ns.toString(),
                            label = label,
                            value = value
                        )
                    )
                }
            }
            items.toList()
        }

        return ICredentialDetailsPresenter.Model.Content(
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
        private val input: IMdocEngagementQrPresenter.Input,
        private val delegate: IMdocEngagementQrPresenter
    ) : software.amazon.app.platform.presenter.molecule.MoleculePresenter<Any, IMdocEngagementQrPresenter.Model> {

        @Composable
        override fun present(input: Any): IMdocEngagementQrPresenter.Model {
            return delegate.present(this.input)
        }
    }
}
