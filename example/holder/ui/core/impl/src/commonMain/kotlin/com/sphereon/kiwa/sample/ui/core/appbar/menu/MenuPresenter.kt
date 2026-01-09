/*
 * © 2026 Sphereon International B.V.
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

package com.sphereon.kiwa.sample.ui.core.appbar.menu

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sphereon.kiwa.sample.ui.core.appbar.BottomAppBarConfig
import com.sphereon.kiwa.sample.ui.core.appbar.IAppBarConfigModel
import com.sphereon.kiwa.sample.ui.core.appbar.TopAppBarConfig
import com.sphereon.kiwa.sample.ui.core.appbar.menu.MenuPresenter.Model
import kotlinx.coroutines.delay
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.renderer.ComposeRenderer
import kotlin.time.Duration.Companion.seconds

/** This presenter provides a custom menu in the App Bar. */
class MenuPresenter : MoleculePresenter<Any, Model> {
    @Composable
    override fun present(input: Any): Model {
        var itemCount by remember { mutableIntStateOf(2) }
        var pressedItem by remember { mutableStateOf<Int?>(null) }

        val items =
            List(itemCount) {
                val number = it + 1
                TopAppBarConfig.MenuItem(text = "Option $number", action = { pressedItem = number })
            }

        LaunchedEffect(pressedItem) {
            delay(3.seconds)
            pressedItem = null
        }

        return Model(items, pressedItem) {
            when (it) {
                Event.AddMenuItem -> itemCount += 1
            }
        }
    }

    data class Model(
        private val menuItems: List<TopAppBarConfig.MenuItem>,
        val pressedItem: Int?,
        val onEvent: (Event) -> Unit,
    ) : BaseModel, IAppBarConfigModel {
        override fun topAppBarConfig(): TopAppBarConfig {
            return TopAppBarConfig(title = "Menu items", menuItems = menuItems)
        }

        override fun bottomAppBarConfig(): BottomAppBarConfig {
            return BottomAppBarConfig()
        }
    }

    sealed interface Event {
        data object AddMenuItem : Event
    }
}

@ContributesRenderer
class MenuRenderer : ComposeRenderer<Model>() {
    @Composable
    override fun Compose(model: Model) {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = { model.onEvent(MenuPresenter.Event.AddMenuItem) }) { Text("Add menu item") }

            AnimatedContent(targetState = model, contentKey = { it.pressedItem != null }) { targetModel ->
                if (targetModel.pressedItem != null) {
                    Text(
                        text = "Pressed option ${targetModel.pressedItem}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
}
