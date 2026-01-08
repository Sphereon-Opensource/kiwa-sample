package com.sphereon.kiwa.sample.app

import com.sphereon.di.session.SessionScope
import com.sphereon.kiwa.sample.ui.core.backstack.PresenterBackstackScope
import com.sphereon.kiwa.sample.ui.elicense.engagement.qr.MdocEngagementPresenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock
import me.tatarka.inject.annotations.Inject
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

    override fun setBackstackScope(backstackScope: PresenterBackstackScope?) {
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

        // Push presenter directly (no wrapper) - matches how CredentialListPresenter works
        scope.push(mdocEngagementPresenter)

        // Notify that navigation was triggered
        _navigationTriggered.value = Clock.System.now().toEpochMilliseconds()

        println("NfcNavigationTrigger: Successfully pushed NFC engagement presenter to backstack")
        return true
    }
}
