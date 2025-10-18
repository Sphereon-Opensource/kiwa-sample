package com.sphereon.kiwa.sample.app

import androidx.compose.ui.window.ComposeUIViewController

// iOS interop function - must start with uppercase as per iOS/Swift conventions
@Suppress("FunctionNaming")
fun MainViewController() = ComposeUIViewController { App() }
