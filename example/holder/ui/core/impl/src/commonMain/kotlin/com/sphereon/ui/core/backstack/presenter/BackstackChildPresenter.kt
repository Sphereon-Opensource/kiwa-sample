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

@file:Suppress("UndocumentedPublicProperty", "UndocumentedPublicClass")

package com.sphereon.ui.core.backstack.presenter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.renderer.ComposeRenderer
import com.sphereon.ui.core.appbar.BottomAppBarConfig
import com.sphereon.ui.core.appbar.IAppBarConfigModel
import com.sphereon.ui.core.appbar.TopAppBarConfig
import com.sphereon.ui.core.backstack.LocalBackstackScope
import com.sphereon.ui.core.backstack.presenter.BackstackChildPresenter.Model
import kotlin.time.Duration.Companion.seconds

/**
 * A presenter that is added to the backstack and has a button to put a new instance on top of the
 * stack.
 */
class BackstackChildPresenter(private val index: Int) : MoleculePresenter<Any, Model> {
    @Composable
    override fun present(input: Any): Model {
        val backstack = checkNotNull(LocalBackstackScope.current)

        var counter by rememberSaveable { mutableIntStateOf(0) }

        LaunchedEffect(Unit) {
            while (isActive) {
                delay(1.seconds)
                counter += 1
            }
        }

        return Model(index = index, counter = counter) {
            when (it) {
                Event.AddPresenterToBackstack -> backstack.push(BackstackChildPresenter(index = index + 1))
            }
        }
    }

    data class Model(val index: Int, val counter: Int, val onEvent: (Event) -> Unit) :
        BaseModel, IAppBarConfigModel {
        override fun topAppBarConfig(): TopAppBarConfig {
            return TopAppBarConfig(title = "Backstack")
        }

        override fun bottomAppBarConfig(): BottomAppBarConfig {
            return BottomAppBarConfig()
        }
    }

    sealed interface Event {
        data object AddPresenterToBackstack : Event
    }
}

@ContributesRenderer
class BackstackChildRenderer : ComposeRenderer<Model>() {
    @Composable
    override fun Compose(model: Model) {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Index: ${model.index}")
            Text("Counter: ${model.counter}")

            Button(onClick = { model.onEvent(BackstackChildPresenter.Event.AddPresenterToBackstack) }) {
                Text("Add presenter to backstack")
            }
        }
    }
}
