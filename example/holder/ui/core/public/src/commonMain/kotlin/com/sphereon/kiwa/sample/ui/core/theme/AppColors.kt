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

package com.sphereon.kiwa.sample.ui.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Shared application color palette for screens, nav bars, lists and accents.
 * Keep UI colors centralized to ensure consistency across modules.
 *
 * For a production app a injectable service with settings support would make more sense
 */
object AppColors {

    private const val COLOR_NAV_BACKGROUND = 0xFF202537
    private const val COLOR_NAV_CONTENT = 0xFFFBFBFB
    private const val COLOR_NAV_BORDER = 0x1FFFFFFF
    private const val COLOR_SCREEN_BACKGROUND = 0xFF202537
    private const val COLOR_SCREEN_FOREGROUND = 0xFFFBFBFB
    private const val COLOR_LIST_ROW_DARK = 0xFF202537
    private const val COLOR_LIST_ROW_LIGHT = 0xFF2B3354
    private const val COLOR_ACCENT_PRIMARY = 0xFF7276F7
    private const val COLOR_SURFACE_SHEET = 0xFFFFFFFF
    private const val COLOR_MISC_TAB_BACKGROUND = 0x2639425F
    private const val COLOR_MISC_MUTED_TEXT = 0xFF5A5F7A
    private const val COLOR_MISC_TITLE_TEXT = 0xFF202537

    object Nav {
        val background = Color(COLOR_NAV_BACKGROUND)
        val content = Color(COLOR_NAV_CONTENT)

        // Subtle 1px/1dp top/bottom borders
        val border = Color(COLOR_NAV_BORDER)
    }

    object Screen {
        val background = Color(COLOR_SCREEN_BACKGROUND)
        val foreground = Color(COLOR_SCREEN_FOREGROUND)
    }

    object List {
        val rowDark = Color(COLOR_LIST_ROW_DARK)
        val rowLight = Color(COLOR_LIST_ROW_LIGHT)
    }

    object Accent {
        val primary = Color(COLOR_ACCENT_PRIMARY)
    }

    object Surface {
        val sheet = Color(COLOR_SURFACE_SHEET)
    }

    object Misc {
        // Semi-transparent tab background overlay used on dark screens
        val tabBackground = Color(COLOR_MISC_TAB_BACKGROUND)
        val mutedText = Color(COLOR_MISC_MUTED_TEXT)
        val titleText = Color(COLOR_MISC_TITLE_TEXT)
    }
}
