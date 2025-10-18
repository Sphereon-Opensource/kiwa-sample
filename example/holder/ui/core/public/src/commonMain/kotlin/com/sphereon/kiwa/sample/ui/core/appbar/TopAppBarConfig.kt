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

package com.sphereon.kiwa.sample.ui.core.appbar

import androidx.compose.material.icons.Icons

/** Configures the look of the App Bar for the recipe app. */
data class TopAppBarConfig(
    /** The title shown in the center of the App Bar. */
    val title: String,
    /**
     * If not null, then the back arrow will be shown and the lambda invoked when there's an action.
     */
    val backArrowAction: (() -> Unit)? = null,

    /** A list of menu items that should be shown in the overflow menu if any. */
    val menuItems: List<MenuItem> = emptyList(),

    val show: Boolean = true,
) {

    val asHidden: TopAppBarConfig
        // getter to make sure we do not recurse infinitely
        get() = copy(show = false)

    /**
     * An element in the overflow menu with [text] as the title. [action] is invoked when this element
     * is pressed.
     */
    data class MenuItem(val text: String, val action: () -> Unit)

    companion object {
        /** The default configuration used when no presenter overrides the config. */
        val DEFAULT = TopAppBarConfig(title = "Sample App")
        val HIDDEN = DEFAULT.asHidden
    }
}

data class BottomAppBarConfig(
    val show: Boolean = true,
    val menuItems: List<MenuItem> = emptyList()
) {

    val asHidden: BottomAppBarConfig
        // getter to make sure we do not recurse infinitely
        get() = copy(show = false)

    data class MenuItem(val text: Icons, val action: () -> Unit)

    companion object {
        val DEFAULT = BottomAppBarConfig()
        val HIDDEN = DEFAULT.asHidden
    }
}
