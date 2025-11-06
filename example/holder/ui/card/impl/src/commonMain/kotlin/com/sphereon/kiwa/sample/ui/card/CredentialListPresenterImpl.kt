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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.sphereon.crypto.core.kms.KeyManagerService
import com.sphereon.crypto.kms.keystore.software.SoftwareKeyStoreService
import com.sphereon.crypto.kms.provider.software.SoftwareKmsProvider
import com.sphereon.di.session.SessionScope
import com.sphereon.kiwa.sample.ui.auth.AuthSessionService
import com.sphereon.kiwa.sample.ui.core.backstack.LocalBackstackScope
import com.sphereon.kiwa.sample.ui.core.logs.LogViewerScreenPresenter
import com.sphereon.kiwa.sample.ui.elicense.assignment.ElicenseAssignmentScreenPresenter
import com.sphereon.kiwa.sample.ui.elicense.engagement.qr.MdocEngagementPresenter
import com.sphereon.kiwa.sample.ui.elicense.issuance.TestPidIssuer
import com.sphereon.kiwa.sample.ui.elicense.keystore.KeyCleanupService
import com.sphereon.kiwa.sample.ui.elicense.settings.SubscriptionKeyPresenter
import com.sphereon.kiwa.sample.ui.elicense.store.SimpleMdocStore
import com.sphereon.kiwa.sample.ui.elicense.store.SimpleDocumentEntry
import com.sphereon.mdoc.data.device.Document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@Inject
@ContributesBinding(SessionScope::class, boundType = CredentialListPresenter::class)
@Suppress("LongParameterList") // DI constructor requires all these dependencies
class CredentialListPresenterImpl(
    val pidIssuer: TestPidIssuer,
    val storage: SimpleMdocStore,
    val kms: KeyManagerService,
    val detailsPresenter: CredentialDetailsPresenter,
    val mdocEngagementQrPresenter: MdocEngagementPresenter,
    val elicenseAssignmentScreenPresenter: ElicenseAssignmentScreenPresenter,
    val authSessionService: AuthSessionService,
    val subscriptionKeyPresenter: SubscriptionKeyPresenter,
    val logViewerScreenPresenter: LogViewerScreenPresenter,
    val keyCleanupService: KeyCleanupService,
) : CredentialListPresenter {
    @Composable
    override fun present(input: Unit): CredentialListPresenter.Model {
        println("CredentialListPresenter present")

        var documentEntries by remember { mutableStateOf<List<SimpleDocumentEntry>>(emptyList()) }
        var pendingDelete by remember { mutableStateOf<Document?>(null) }
        val scope = rememberCoroutineScope()
        val backstack = checkNotNull(LocalBackstackScope.current)

        LaunchedEffect(Unit) {
            storage.documentsFlow.collect { entries -> documentEntries = entries }
        }

        // Run key cleanup in background after login (when presenter is first created)
        // Use GlobalScope to ensure cleanup continues even when navigating away from this screen
        LaunchedEffect(Unit) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    println("Running background key cleanup after login")
                    keyCleanupService.cleanupEphemeralKeys()
                    println("Background key cleanup completed")
                } catch (e: Exception) {
                    println("Error during background key cleanup: ${e.message}")
                }
            }
        }

        val onStateEvent: (CredentialListPresenter.StateEvent) -> Unit = { event ->
            println("CredentialListPresenter: Received event: ${event.javaClass.simpleName}")
            when (event) {
                is CredentialListPresenter.StateEvent.CreateNewCredential -> {
                    scope.launch { pidIssuer.issuePid() }
                }

                is CredentialListPresenter.StateEvent.AssignLicense -> {
                    backstack.push(elicenseAssignmentScreenPresenter)
                }

                is CredentialListPresenter.StateEvent.OpenCredentialDetails -> {
                    backstack.push(CredentialDetailsScreenPresenter(event.document, detailsPresenter))
                }

                is CredentialListPresenter.StateEvent.ConfirmDelete -> {
                    scope.launch {
                        storage.removeDocument(event.document)
                        pendingDelete = null
                    }
                }

                is CredentialListPresenter.StateEvent.CancelDelete -> pendingDelete = null
                is CredentialListPresenter.StateEvent.AttendedPresentation -> {
                    backstack.push(
                        MdocEngagementScreenPresenter(mdocEngagementQrPresenter)
                    )
                }

                is CredentialListPresenter.StateEvent.GoToHome -> pendingDelete = null
                is CredentialListPresenter.StateEvent.SubscriptionKey -> {
                    backstack.push(SubscriptionKeyScreenPresenter(subscriptionKeyPresenter))
                }

                is CredentialListPresenter.StateEvent.Logout -> {
                    authSessionService.logout().also { backstack.clear() }
                }

                is CredentialListPresenter.StateEvent.ViewLogs -> {
                    backstack.push(logViewerScreenPresenter)
                }

                is CredentialListPresenter.StateEvent.ClearLicenses -> {
                    scope.launch { clearLicensesAndKeys() }
                }

                else -> {}
            }
        }

        val items = createCredentialRows(documentEntries) { doc -> pendingDelete = doc }

        return CredentialListPresenter.Model.CredentialList(
            items = items,
            pendingDelete = pendingDelete,
            onStateEvent = onStateEvent
        )
    }

    private suspend fun clearLicensesAndKeys() {
        try {
            withContext(Dispatchers.IO) {
                // Clear all documents from storage
                storage.clearAll()

                // Use the KeyCleanupService to clean up ephemeral keys
                // This will preserve system keys while removing document-related keys
                println("CredentialListPresenter: Running key cleanup after clearing all licenses")
                keyCleanupService.cleanupEphemeralKeys()
                println("CredentialListPresenter: Successfully cleared all licenses and cleaned up keys")
            }
        } catch (e: SecurityException) {
            println("CredentialListPresenter: Security error clearing licenses and keys: ${e.message}")
        } catch (e: java.io.IOException) {
            println("CredentialListPresenter: IO error clearing licenses and keys: ${e.message}")
        } catch (e: Exception) {
            println("CredentialListPresenter: Error clearing licenses and keys: ${e.message}")
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

    private class SubscriptionKeyScreenPresenter(
        private val delegate: SubscriptionKeyPresenter
    ) : MoleculePresenter<Any, SubscriptionKeyPresenter.Model> {

        @Composable
        override fun present(input: Any): SubscriptionKeyPresenter.Model {
            return delegate.present(Unit)
        }
    }

    private fun createCredentialRows(
        documentEntries: List<SimpleDocumentEntry>,
        onDelete: (Document) -> Unit
    ): List<CredentialListItemPresenter.Model.Row> {
        return documentEntries.map { entry ->
            CredentialListItemPresenter.Model.Row(
                CredentialCardPresenter.Input.fromDocument(
                    entry.document,
                    variant = CredentialCardPresenter.Variant.Mini
                ).toModel(),
                document = entry.document,
                onDelete = { onDelete(entry.document) }
            )
        }
    }

    /**
     * Navigates to the engagement presenter with an existing engagement instance.
     * This method can be called from external components like NFC services when
     * an NFC engagement has already been initiated.
     */
    fun navigateToEngagementWithNfc() {
        // We need access to the backstack from the current composition
        // This method would need to be called from within a composable context
        // For now, we'll store the engagement and trigger navigation on next recomposition
        // TODO: Implement proper navigation handling for NFC events
    }

    @ContributesTo(SessionScope::class)
    interface Component {
        val presenter: CredentialListPresenter

        /**
         * Provides access to navigation methods for external components like NFC services.
         * This allows NFC engagement events to trigger navigation to the engagement presenter
         * with an existing engagement instance.
         */
        val credentialListPresenter: CredentialListPresenterImpl
    }
}
