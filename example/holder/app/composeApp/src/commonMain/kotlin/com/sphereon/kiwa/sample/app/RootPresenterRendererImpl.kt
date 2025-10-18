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

package com.sphereon.kiwa.sample.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sphereon.kiwa.sample.ui.card.CredentialDetailsPresenter
import com.sphereon.kiwa.sample.ui.card.CredentialListPresenter
import com.sphereon.kiwa.sample.ui.core.appbar.BottomAppBarConfig
import com.sphereon.kiwa.sample.ui.core.appbar.TopAppBarConfig
import com.sphereon.kiwa.sample.ui.core.backstack.CrossSlideBackstackPresenter
import com.sphereon.kiwa.sample.ui.core.theme.AppColors
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.backgesture.BackGestureDispatcherPresenter
import software.amazon.app.platform.presenter.molecule.backgesture.ForwardBackPressEventsToPresenters
import software.amazon.app.platform.renderer.ComposeRenderer
import software.amazon.app.platform.renderer.RendererFactory
import software.amazon.app.platform.renderer.getComposeRenderer

/**
 * App-specific Compose renderer that extends ui-core CoreTemplateRenderer for TestAppTemplate.
 */
@Inject
@ContributesRenderer
class RootPresenterRendererImpl(
    private val rendererFactory: RendererFactory,
    private val backGestureDispatcherPresenter: BackGestureDispatcherPresenter,
) : ComposeRenderer<SampleAppTemplate>() {

    @Composable
    override fun Compose(model: SampleAppTemplate) {
        backGestureDispatcherPresenter.ForwardBackPressEventsToPresenters()

        when (model) {
            is SampleAppTemplate.AuthenticationTemplate -> {
                val renderer = rendererFactory.getComposeRenderer(model.model)
                renderer.renderCompose(model.model)
            }
            is SampleAppTemplate.FullScreenMdocEngagementTemplate -> {
                val renderer = rendererFactory.getComposeRenderer(model.model)
                renderer.renderCompose(model.model)
            }
            is SampleAppTemplate.MainScreenWithNavbarsTemplate -> FullScreen(model)
        }
    }

    @Composable
    private fun FullScreen(template: SampleAppTemplate.MainScreenWithNavbarsTemplate) {
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
                        RenderBottomBar(model)
                    }
                }
            },
        ) { innerPadding ->
            content(innerPadding)
        }
    }

    @Composable
    private fun RenderBottomBar(model: BaseModel) {
        val actualModel = getActualModel(model)
        val showNewLicenseButton = shouldShowNewLicenseButton(actualModel)

        // Render app-specific bottom bar
        BottomAppBar(
            containerColor = AppColors.Nav.background,
            contentColor = AppColors.Nav.content,
            scrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior(
                rememberBottomAppBarState(),
            ),
        ) {
            RenderShareButton(model)
            if (showNewLicenseButton) {
                RenderVerticalDivider()
                RenderNewLicenseButton(model)
            }
            RenderVerticalDivider()
            RenderHomeButton(model)
        }
    }

    private fun getActualModel(model: BaseModel): BaseModel {
        return if (model is CrossSlideBackstackPresenter.Model) {
            model.delegate
        } else {
            model
        }
    }

    private fun shouldShowNewLicenseButton(actualModel: BaseModel): Boolean {
        return when (actualModel) {
            is CredentialListPresenter.Model -> true
            is CredentialDetailsPresenter.Model -> true
            else -> false
        }
    }

    @Composable
    private fun RowScope.RenderShareButton(model: BaseModel) {
        IconButton(
            onClick = {
                val actualModel = getActualModel(model)
                handleSharePresentation(actualModel)
            },
            modifier = Modifier.weight(1f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share Presentation",
                    tint = AppColors.Nav.content
                )
                Text("Share", color = AppColors.Nav.content)
            }
        }
    }

    private fun handleSharePresentation(actualModel: BaseModel) {
        when (actualModel) {
            is CredentialListPresenter.Model.CredentialList -> {
                actualModel.onStateEvent(CredentialListPresenter.StateEvent.AttendedPresentation)
            }

            is CredentialListPresenter.Model -> {
                actualModel.onStateEvent(CredentialListPresenter.StateEvent.AttendedPresentation)
            }

            is CredentialDetailsPresenter.Model -> {
                actualModel.onEvent(CredentialDetailsPresenter.Event.AttendedPresentation)
            }
        }
    }

    @Composable
    private fun RowScope.RenderNewLicenseButton(model: BaseModel) {
        IconButton(
            onClick = {
                val actualModel = getActualModel(model)
                handleNewLicense(model, actualModel)
            },
            modifier = Modifier.weight(1f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.CreditCard,
                    contentDescription = "New License",
                    tint = AppColors.Nav.content
                )
                Text("New License", color = AppColors.Nav.content)
            }
        }
    }

    private fun handleNewLicense(model: BaseModel, actualModel: BaseModel) {
        when (actualModel) {
            is CredentialListPresenter.Model.CredentialList -> {
                actualModel.onStateEvent(CredentialListPresenter.StateEvent.AssignLicense)
            }
            is CredentialListPresenter.Model -> {
                actualModel.onStateEvent(CredentialListPresenter.StateEvent.AssignLicense)
            }

            else -> {
                if (model is CrossSlideBackstackPresenter.Model) {
                    model.backstackScope.clear()
                }
            }
        }
    }

    @Composable
    private fun RenderVerticalDivider() {
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

    @Composable
    private fun RowScope.RenderHomeButton(model: BaseModel) {
        IconButton(
            onClick = {
                val actualModel = getActualModel(model)
                handleHome(model, actualModel)
            },
            modifier = Modifier.weight(1f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = "Home",
                    tint = AppColors.Nav.content
                )
                Text("Home", color = AppColors.Nav.content)
            }
        }
    }

    private fun handleHome(model: BaseModel, actualModel: BaseModel) {
        when (actualModel) {
            is CredentialListPresenter.Model.CredentialList -> {
                actualModel.onStateEvent(CredentialListPresenter.StateEvent.GoToHome)
            }

            else -> {
                if (model is CrossSlideBackstackPresenter.Model) {
                    val backstackScope = model.backstackScope
                    if (backstackScope.lastBackstackChange.value.backstack.size > 1) {
                        backstackScope.clear()
                    }
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
