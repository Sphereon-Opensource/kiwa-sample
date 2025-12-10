package com.sphereon.kiwa.sample.app

import com.sphereon.kiwa.sample.ui.core.backstack.PresenterBackstackScope
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.concurrent.Volatile

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = NfcBackstackRegistry::class)
class NfcBackstackRegistryImpl : NfcBackstackRegistry {

    @Volatile
    private var backstackScope: PresenterBackstackScope? = null

    override fun setBackstackScope(scope: PresenterBackstackScope?) {
        println("NfcBackstackRegistry: Setting backstack scope: $scope")
        backstackScope = scope
    }

    override fun getBackstackScope(): PresenterBackstackScope? {
        println("NfcBackstackRegistry: Returning backstack scope: $backstackScope")
        return backstackScope
    }
}
