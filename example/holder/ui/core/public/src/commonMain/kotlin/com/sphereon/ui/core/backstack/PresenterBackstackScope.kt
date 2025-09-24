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

package com.sphereon.ui.core.backstack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.presenter.molecule.returningCompositionLocalProvider
import com.sphereon.ui.core.backstack.PresenterBackstackScope.BackstackChange
import com.sphereon.ui.core.backstack.PresenterBackstackScope.BackstackChange.Action
import com.sphereon.ui.core.saveable.rememberReturningSaveableStateHolder

/**
 * Receiver scope for content lambda for [presenterBackstack]. In this scope, [lastBackstackChange]
 * can be used to observe the state of the backstack, or to [push] and [pop] presenters.
 */
interface PresenterBackstackScope {

    /** Provides the last change made to the backstack. */
    val lastBackstackChange: State<BackstackChange>

    /** Pushes a new presenter to the top of the backstack. This will update [lastBackstackChange]. */
    fun push(presenter: MoleculePresenter<*, out BaseModel>)

    /**
     * Removes the top presenter from the backstack. This will update [lastBackstackChange]. Note that
     * the stack will always contain the initial presenter provided in [presenterBackstack] and this
     * presenter cannot be popped.
     */
    fun pop()

    /** Describes the current state of the backstack with the last change. */
    interface BackstackChange {

        /**
         * The stack of presenters. This list will always contain at least one element, which is the
         * initial presenter provided in [presenterBackstack].
         */
        val backstack: List<MoleculePresenter<Any, out BaseModel>>

        /**
         * The last executed for the backstack. Knowing what kind of change was made is helpful to
         * animate changes in the UI.
         */
        val action: Action

        /** Describes all actions that can be applied to the backstack. */
        enum class Action {
            /** A new presenter was added to the top of the backstack. */
            PUSH,

            /** The last top presenter in the stack was removed. */
            POP,
        }
    }

    fun clear()
}

private class PresenterBackstackScopeImpl(val initial: MoleculePresenter<*, out BaseModel>) :
    PresenterBackstackScope {
    private var _lastBackstackChange =
        mutableStateOf(BackstackChangeImpl(listOf(initial) as List<MoleculePresenter<Any, out BaseModel>>, Action.PUSH))
    override val lastBackstackChange: State<BackstackChange> = _lastBackstackChange

    override fun push(presenter: MoleculePresenter<*, out BaseModel>) {
        _lastBackstackChange.value =
            BackstackChangeImpl(
                backstack = (_lastBackstackChange.value.backstack + presenter) as List<MoleculePresenter<Any, out BaseModel>>,
                action = Action.PUSH,
            )
    }

    override fun pop() {
        val oldStack = _lastBackstackChange.value.backstack
        if (oldStack.size > 1) {
            _lastBackstackChange.value =
                BackstackChangeImpl(backstack = oldStack.subList(0, oldStack.size - 1), action = Action.POP)
        }
    }

    override fun clear() {
        _lastBackstackChange.value = BackstackChangeImpl(listOf(initial) as List<MoleculePresenter<Any, out BaseModel>>, Action.PUSH)
    }

    private class BackstackChangeImpl(
        override val backstack: List<MoleculePresenter<Any, out BaseModel>>,
        override val action: Action,
    ) : BackstackChange
}

/**
 * Provides the backstack closest to this presenter or `null` if there's none. Presenters build a
 * tree. When the backstack is queried, then the backstack first found while walking the tree to the
 * root is returned. In the tree there can be multiple backstacks.
 *
 * ```
 * @Composable
 * override fun present(input: Unit): Model {
 *   val backstack = checkNotNull(LocalBackstackScope.current)
 *   ...
 * }
 * ```
 */
val LocalBackstackScope = compositionLocalOf<PresenterBackstackScope?> { null }

/**
 * Creates a new backstack for presenters. A backstack always contains [initialPresenter] as an
 * element.
 *
 * [content] is invoked with the [BaseModel] of the top presenter in the stack. The lambda returns
 * the model of the presenter this function [presenterBackstack] is called in. The receiver type
 * [PresenterBackstackScope] allows you to modify the backstack or get information about changes.
 *
 * ```
 * class SomePresenter : MoleculePresenter<Unit, SomeModel> {
 *   @Composable
 *   override fun present(input: Unit): SomeModel {
 *     val initialPresenter = ...
 *     return presenterBackstack(initialPresenter) { model ->
 *       SomeModel(...)
 *     }
 *   }
 *
 *   data class SomeModel(...) : BaseModel
 * }
 *
 * ```
 *
 * Child presenters get access to the backstack through [LocalBackstackScope], e.g.
 *
 * ```
 * @Composable
 * override fun present(input: Unit): Model {
 *   val backstack = checkNotNull(LocalBackstackScope.current)
 *   ...
 * }
 * ```
 *
 * To use a cross-slide animation by default for backstack changes, consider using
 * [CrossSlideBackstackPresenter].
 */
@Composable
fun <ModelT : BaseModel> presenterBackstack(
    initialPresenter: MoleculePresenter<Any, out BaseModel>,
    content: @Composable PresenterBackstackScope.(model: BaseModel) -> ModelT,
): ModelT {
    val scope = remember { PresenterBackstackScopeImpl(initialPresenter) }
    val saveableStateHolder = rememberReturningSaveableStateHolder()

    val presenter = scope.lastBackstackChange.value.backstack.last()

    return saveableStateHolder.SaveableStateProvider(key = presenter) {
        returningCompositionLocalProvider(LocalBackstackScope provides scope) {
            val backstackModel = presenter.present(Unit)
            content.invoke(scope, backstackModel)
        }
    }
}
