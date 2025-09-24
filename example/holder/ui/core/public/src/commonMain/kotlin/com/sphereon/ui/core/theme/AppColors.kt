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

package com.sphereon.ui.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Shared application color palette for screens, nav bars, lists and accents.
 * Keep UI colors centralized to ensure consistency across modules.
 *
 * For a production app a injectable service with settings support would make more sense
 */
object AppColors {

    object Nav {
        val background = Color(0xFF202537)
        val content = Color(0xFFFBFBFB)

        // Subtle 1px/1dp top/bottom borders
        val border = Color(0x1FFFFFFF)
    }

    object Screen {
        val background = Color(0xFF202537)
        val foreground = Color(0xFFFBFBFB)
    }

    object List {
        val rowDark = Color(0xFF202537)
        val rowLight = Color(0xFF2B3354)
    }

    object Accent {
        val primary = Color(0xFF7276F7)
    }

    object Surface {
        val sheet = Color(0xFFFFFFFF)
    }

    object Misc {
        // Semi-transparent tab background overlay used on dark screens
        val tabBackground = Color(0x2639425F)
        val mutedText = Color(0xFF5A5F7A)
        val titleText = Color(0xFF202537)
    }
}
