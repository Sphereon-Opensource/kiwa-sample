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

@file:OptIn(ExperimentalMaterial3Api::class)

package com.sphereon.mdoc.testapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.backgesture.BackGestureDispatcherPresenter
import software.amazon.app.platform.presenter.molecule.backgesture.ForwardBackPressEventsToPresenters
import software.amazon.app.platform.renderer.ComposeRenderer
import software.amazon.app.platform.renderer.RendererFactory
import software.amazon.app.platform.renderer.getComposeRenderer
import com.sphereon.ui.card.ICredentialDetailsPresenter
import com.sphereon.ui.card.ICredentialListPresenter
import com.sphereon.ui.core.appbar.BottomAppBarConfig
import com.sphereon.ui.core.appbar.TopAppBarConfig
import com.sphereon.ui.core.theme.AppColors

/**
 * App-specific Compose renderer that extends ui-core CoreTemplateRenderer for TestAppTemplate.
 */
@Inject
@ContributesRenderer
class RootPresenterRenderer(
    private val rendererFactory: RendererFactory,
    private val backGestureDispatcherPresenter: BackGestureDispatcherPresenter,
) : ComposeRenderer<TestAppTemplate>() {

    @Composable
    override fun Compose(model: TestAppTemplate) {
        backGestureDispatcherPresenter.ForwardBackPressEventsToPresenters()

        when (model) {
            is TestAppTemplate.AuthenticationTemplate -> Authentication(model)
            is TestAppTemplate.FullScreenMdocEngagementQrTemplate -> FullScreenMdocEngagementQr(model)
            is TestAppTemplate.FullScreenTemplate -> FullScreen(model)
        }
    }

    @Composable
    private fun Authentication(template: TestAppTemplate.AuthenticationTemplate) {
        val renderer = rendererFactory.getComposeRenderer(template.model)
        renderer.renderCompose(template.model)
    }

    @Composable
    private fun FullScreenMdocEngagementQr(template: TestAppTemplate.FullScreenMdocEngagementQrTemplate) {
        val renderer = rendererFactory.getComposeRenderer(template.model)
        renderer.renderCompose(template.model)
    }

    @Composable
    private fun FullScreen(template: TestAppTemplate.FullScreenTemplate) {
        CenterAlignedWithAppBars(
            topAppBarConfig = template.topAppBarConfig,
            bottomAppBarConfig = template.bottomAppBarConfig,
            model = template.model
        ) {
            Box(modifier = Modifier.padding(it)) {
                val renderer = rendererFactory.getComposeRenderer(template.model)
                renderer.renderCompose(template.model)
            }
        }
    }

    @Composable
    private fun CenterAlignedWithAppBars(
        topAppBarConfig: TopAppBarConfig? = null,
        bottomAppBarConfig: BottomAppBarConfig? = null,
        model: BaseModel,
        content: @Composable (PaddingValues) -> Unit,
    ) {
        Scaffold(
            topBar = {
                if (topAppBarConfig?.show == true) {
                    Column(Modifier.fillMaxWidth()) {
                        CenterAlignedTopAppBar(
                            colors =
                            TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = AppColors.Nav.background,
                                titleContentColor = AppColors.Nav.content,
                                navigationIconContentColor = AppColors.Nav.content,
                                actionIconContentColor = AppColors.Nav.content,
                            ),
                            title = {
                                Text(
                                    topAppBarConfig.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = AppColors.Nav.content
                                )
                            },
                            navigationIcon = {
                                if (topAppBarConfig.backArrowAction != null) {
                                    IconButton(onClick = topAppBarConfig.backArrowAction!!) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = AppColors.Nav.content,
                                        )
                                    }
                                }
                            },
                            actions = {
                                if (topAppBarConfig.menuItems.isNotEmpty()) {
                                    MinimalDropdownMenu(topAppBarConfig.menuItems)
                                }
                            },
                        )
                        HorizontalDivider(color = AppColors.Nav.border, thickness = 1.dp)
                    }
                }
            },
            bottomBar = {
                if (bottomAppBarConfig?.show == true) {
                    Column(Modifier.fillMaxWidth()) {
                        HorizontalDivider(color = AppColors.Nav.border, thickness = 1.dp)
                        renderBottomBar(bottomAppBarConfig, model)
                    }
                }
            },
        ) { innerPadding ->
            content(innerPadding)
        }
    }

    @Composable
    private fun renderBottomBar(bottomAppBarConfig: BottomAppBarConfig, model: BaseModel) {
        // Handle CrossSlideBackstackPresenter.Model wrapper
        val actualModel = if (model is com.sphereon.ui.core.backstack.CrossSlideBackstackPresenter.Model) {
            model.delegate
        } else {
            model
        }

        // Determine if New License button should be shown based on current screen
        val showNewLicenseButton = when (actualModel) {
            is ICredentialListPresenter.Model -> true // Show on credential list screens
            is ICredentialDetailsPresenter.Model -> true // Show on credential details screens
            else -> false // Hide on other screens (presentation, etc.)
        }

        // Render app-specific bottom bar
        BottomAppBar(
            containerColor = AppColors.Nav.background,
            contentColor = AppColors.Nav.content,
            scrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior(
                rememberBottomAppBarState(),
            ),
        ) {
            // Share presentation button
            IconButton(
                onClick = {
                    // Handle CrossSlideBackstackPresenter.Model wrapper
                    val actualModel = if (model is com.sphereon.ui.core.backstack.CrossSlideBackstackPresenter.Model) {
                        model.delegate
                    } else {
                        model
                    }

                    when (actualModel) {
                        is ICredentialListPresenter.Model.CredentialList -> {
                            actualModel.onStateEvent(ICredentialListPresenter.StateEvent.AttendedPresentation)
                        }

                        is ICredentialListPresenter.Model -> {
                            actualModel.onStateEvent(ICredentialListPresenter.StateEvent.AttendedPresentation)
                        }

                        is ICredentialDetailsPresenter.Model -> {
                            actualModel.onEvent(ICredentialDetailsPresenter.Event.AttendedPresentation)
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share Presentation",
                        tint = AppColors.Nav.content
                    )
                    Text("Share", color = AppColors.Nav.content)
                }
            }
            // New License Button
            if (showNewLicenseButton) {
                IconButton(
                    onClick = {
                        // Handle CrossSlideBackstackPresenter.Model wrapper
                        val actualModel = if (model is com.sphereon.ui.core.backstack.CrossSlideBackstackPresenter.Model) {
                            model.delegate
                        } else {
                            model
                        }

                        when (actualModel) {
                            is ICredentialListPresenter.Model.CredentialList -> {
                                // Directly trigger assign license when on credential list
                                actualModel.onStateEvent(ICredentialListPresenter.StateEvent.AssignLicense)
                            }

                            is ICredentialListPresenter.Model -> {
                                // Directly trigger assign license when on credential list
                                actualModel.onStateEvent(ICredentialListPresenter.StateEvent.AssignLicense)
                            }

                            else -> {
                                // For other screens, navigate back to credential list first
                                if (model is com.sphereon.ui.core.backstack.CrossSlideBackstackPresenter.Model) {
                                    val backstackScope = model.backstackScope
                                    // Clear backstack to go back to credential list
                                    backstackScope.clear()
                                    // Note: This will take user back to credential list where they can use the New License button
                                    // or the top bar "Assign license" menu item to access the assign license functionality
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.CreditCard,
                            contentDescription = "New License",
                            tint = AppColors.Nav.content
                        )
                        Text("New License", color = AppColors.Nav.content)
                    }
                }
            }
            // Divider (only show if New License button is visible)
            if (showNewLicenseButton) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp),
                        color = AppColors.Nav.border
                    )
                }
            }
            // Divider
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 8.dp)
            ) {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp),
                    color = AppColors.Nav.border
                )
            }
            // Home button
            IconButton(
                onClick = {
                    // Handle CrossSlideBackstackPresenter.Model wrapper
                    val actualModel = if (model is com.sphereon.ui.core.backstack.CrossSlideBackstackPresenter.Model) {
                        model.delegate
                    } else {
                        model
                    }

                    when (actualModel) {
                        is ICredentialListPresenter.Model.CredentialList -> {
                            actualModel.onStateEvent(ICredentialListPresenter.StateEvent.GoToHome)
                        }

                        else -> {
                            // We're on a different screen, so pop the backstack to go back to credential list
                            if (model is com.sphereon.ui.core.backstack.CrossSlideBackstackPresenter.Model) {
                                val backstackScope = model.backstackScope
                                if (backstackScope.lastBackstackChange.value.backstack.size > 1) {
                                    backstackScope.clear()
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = "Home",
                        tint = AppColors.Nav.content
                    )
                    Text("Home", color = AppColors.Nav.content)
                }
            }
        }
    }

    @Composable
    private fun MinimalDropdownMenu(menuItems: List<TopAppBarConfig.MenuItem>) {
        var expanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.padding(16.dp)) {
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More options",
                    tint = AppColors.Nav.content
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                menuItems.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.text) },
                        onClick = {
                            expanded = false
                            item.action()
                        },
                    )
                }
            }
        }
    }
}
