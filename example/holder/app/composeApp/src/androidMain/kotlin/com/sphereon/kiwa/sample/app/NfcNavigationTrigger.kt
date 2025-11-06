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
 */

package com.sphereon.kiwa.sample.app

import androidx.compose.runtime.Composable
import com.sphereon.di.session.SessionScope
import com.sphereon.kiwa.sample.ui.elicense.engagement.qr.MdocEngagementPresenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@Inject
@ContributesBinding(SessionScope::class, boundType = INfcNavigationTrigger::class)
class NfcNavigationTrigger(
    private val mdocEngagementPresenter: MdocEngagementPresenter,
    private val backstackRegistry: NfcBackstackRegistry
) : INfcNavigationTrigger {

//    private var backstackScope: com.sphereon.kiwa.sample.ui.core.backstack.PresenterBackstackScope? = null
    private val _navigationTriggered = MutableStateFlow(0L)
    override val navigationTriggeredFlow: StateFlow<Long> = _navigationTriggered

    override fun setBackstackScope(backstackScope: com.sphereon.kiwa.sample.ui.core.backstack.PresenterBackstackScope?) {
        println("NfcNavigationTrigger: Setting backstack scope: $backstackScope")
        backstackRegistry.setBackstackScope(backstackScope)
    }

    override fun navigateToNfcEngagement(): Boolean {
        println("NfcNavigationTrigger: navigateToNfcEngagement called on instance: $this")
        println("NfcNavigationTrigger: Instance hashCode: ${this.hashCode()}")
        val scope = backstackRegistry.getBackstackScope()
        if (scope == null) {
            println("NfcNavigationTrigger: No backstack scope available, cannot navigate")
            return false
        }

        println("NfcNavigationTrigger: Navigating to NFC engagement")

        // Push the engagement presenter with a wrapper to convert Any input to MdocEngagementPresenter.Input
        // The engagement manager already tracks the active engagement globally
        scope.push(NfcEngagementScreenPresenter(mdocEngagementPresenter))

        // Notify that navigation was triggered
        _navigationTriggered.value = System.currentTimeMillis()

        println("NfcNavigationTrigger: Successfully pushed NFC engagement presenter to backstack")
        return true
    }

    private class NfcEngagementScreenPresenter(
        private val delegate: MdocEngagementPresenter
    ) : MoleculePresenter<Any, MdocEngagementPresenter.Model> {

        @Composable
        override fun present(input: Any): MdocEngagementPresenter.Model {
            return delegate.present(MdocEngagementPresenter.Input)
        }
    }
}