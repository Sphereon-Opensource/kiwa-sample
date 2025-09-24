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

package com.sphereon.ui.core.appbar

import software.amazon.app.platform.presenter.BaseModel

/** Can be implemented by a [BaseModel] class to change the configuration of the App Bar. */
interface IAppBarConfigModel {
    /** Returns the config that should be rendered. */
    fun topAppBarConfig(): TopAppBarConfig? = null

    fun bottomAppBarConfig(): BottomAppBarConfig? = null
}
