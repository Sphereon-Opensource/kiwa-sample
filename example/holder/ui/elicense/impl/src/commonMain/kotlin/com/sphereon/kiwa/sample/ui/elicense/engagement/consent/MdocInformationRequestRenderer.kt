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

package com.sphereon.kiwa.sample.ui.elicense.engagement.consent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sphereon.kiwa.sample.ui.card.CredentialCardRenderer
import com.sphereon.kiwa.sample.ui.card.CredentialCardPresenter
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.renderer.ComposeRenderer

@Inject
@ContributesRenderer(modelType = MdocInformationRequestPresenter.Model::class)
class MdocInformationRequestRenderer(
    private val credentialCardRenderer: CredentialCardRenderer
) : ComposeRenderer<MdocInformationRequestPresenter.Model>() {

    private val bg = Color(BG_COLOR)
    private val fg = Color(FG_COLOR)
    private val panel = Color(PANEL_COLOR)
    private val border = Color(BORDER_COLOR)
    private val accent = Color(ACCENT_COLOR)

    @Composable
    override fun Compose(model: MdocInformationRequestPresenter.Model) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(bottom = PADDING_BOTTOM.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = PADDING_HORIZONTAL.dp),
                verticalArrangement = Arrangement.Top
            ) {
                // Scrollable content
                val scrollState = rememberScrollState()
                Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(scrollState)) {
                    HeaderText()

                    model.docRequestSections.forEachIndexed { sectionIndex, section ->
                        DocRequestSectionContent(model, section, sectionIndex)
                    }
                }

                BottomButtons(model)
            }
        }
    }

    @Composable
    private fun HeaderText() {
        Text(
            text = "The following information will be shared",
            color = fg,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = PADDING_TOP.dp, bottom = PADDING_SECTION.dp)
        )
    }

    @Composable
    private fun DocRequestSectionContent(
        model: MdocInformationRequestPresenter.Model,
        section: MdocInformationRequestPresenter.DocRequestSection,
        sectionIndex: Int
    ) {
        section.description?.let { desc ->
            Text(
                text = desc,
                color = fg.copy(alpha = TEXT_ALPHA),
                modifier = Modifier.padding(bottom = SPACING_SMALL.dp)
            )
        }

        // Horizontal mini cards
        MiniCardsRow(
            docRequestSection = section,
            onSelect = { idx ->
                model.onEvent(
                    MdocInformationRequestPresenter.Event.OnCardSelected(sectionIndex, idx)
                )
            }
        )

        Spacer(modifier = Modifier.height(SPACING_SMALL.dp))

        SectionHeader(section)

        // Details panel
        if (section.cards.size > MIN_CARDS_FOR_SELECTION && section.selectedIndex == null) {
            SelectionPromptPanel()
        } else if (section.selectedIndex != null) {
            DetailsPanel(section)
        }

        Spacer(modifier = Modifier.height(SPACING_SECTION.dp))
    }

    @Composable
    private fun SectionHeader(section: MdocInformationRequestPresenter.DocRequestSection) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PADDING_HORIZONTAL.dp, vertical = PADDING_VERTICAL_SMALL.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(section.title, color = fg, fontWeight = FontWeight.SemiBold)
            val countSel = if (section.selectedIndex != null) {
                SELECTED_COUNT_ONE
            } else {
                SELECTED_COUNT_ZERO
            }
            Text("$countSel Selected", color = accent)
        }
    }

    @Composable
    private fun BottomButtons(model: MdocInformationRequestPresenter.Model) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            val gradientStart = Color(GRADIENT_START_COLOR)
            val gradientEnd = Color(GRADIENT_END_COLOR)
            Button(
                onClick = { model.onEvent(MdocInformationRequestPresenter.Event.OnShare) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = gradientStart,
                    contentColor = fg,
                    disabledContainerColor = gradientStart.copy(alpha = DISABLED_BUTTON_ALPHA),
                    disabledContentColor = fg.copy(alpha = DISABLED_TEXT_ALPHA)
                ),
                enabled = model.canContinue,
                modifier = Modifier.fillMaxWidth().height(BUTTON_HEIGHT.dp),
                shape = RoundedCornerShape(BUTTON_CORNER_RADIUS.dp)
            ) { Text("Share", fontWeight = FontWeight.Medium) }

            Spacer(Modifier.height(SPACING_SMALL.dp))

            // Outlined secondary with gradient text look (approximation)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BUTTON_HEIGHT.dp)
                    .border(BORDER_WIDTH_ONE.dp, gradientEnd, RoundedCornerShape(BUTTON_CORNER_RADIUS.dp))
                    .clickableNoIndication { model.onEvent(MdocInformationRequestPresenter.Event.OnDecline) },
                contentAlignment = Alignment.Center
            ) { Text("Decline", color = fg, fontWeight = FontWeight.Medium) }
        }
    }

    @Composable
    private fun MiniCardsRow(
        docRequestSection: MdocInformationRequestPresenter.DocRequestSection,
        onSelect: (Int) -> Unit
    ) {
        val shape: Shape = RoundedCornerShape(CORNER_RADIUS.dp)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp, Alignment.Start),
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
            itemsIndexed(
                docRequestSection.cards
            ) { index: Int, cardModel: CredentialCardPresenter.Model ->
                val selected = docRequestSection.selectedIndex == index
                val baseModifier = if (selected) {
                    Modifier.shadow(SPACING_SMALL.dp, shape)
                } else {
                    Modifier
                }
                Box(
                    modifier = baseModifier
                        .size(width = MINI_CARD_WIDTH.dp, height = MINI_CARD_HEIGHT.dp)
                        .border(
                            width = if (selected) {
                                BORDER_WIDTH_SELECTED.dp
                            } else {
                                BORDER_WIDTH_NORMAL.dp
                            },
                            color = if (selected) {
                                accent
                            } else {
                                border.copy(alpha = 0.5f)
                            },
                            shape = shape
                        )
                        .clip(shape)
                        .clickableNoIndication { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    // Delegate actual mini card rendering
                    credentialCardRenderer.renderCompose(cardModel)

                    // Selection indicator
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(INDICATOR_PADDING.dp)
                                .size(SELECTION_INDICATOR_SIZE.dp)
                                .clip(CircleShape)
                                .background(accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓", color = fg, fontWeight = FontWeight.Bold, fontSize = CHECKMARK_FONT_SIZE.sp)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun DetailsPanel(docRequestSection: MdocInformationRequestPresenter.DocRequestSection) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(topEnd = CORNER_RADIUS.dp, bottomEnd = CORNER_RADIUS.dp))
        ) {
            Box(
                modifier = Modifier
                    .width(GREEN_STRIPE_WIDTH.dp)
                    .fillMaxHeight()
                    .background(Color(GREEN_STRIPE_COLOR))
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        BORDER_WIDTH_NORMAL.dp,
                        border,
                        RoundedCornerShape(topEnd = CORNER_RADIUS.dp, bottomEnd = CORNER_RADIUS.dp)
                    )
                    .padding(PADDING_DETAILS.dp)
            ) {
                docRequestSection.details.forEach { row ->
                    Column(modifier = Modifier.padding(vertical = PADDING_DETAILS_ROW.dp)) {
                        Text(row.label, color = fg.copy(alpha = TEXT_ALPHA))
                        Text(row.value, color = fg, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    @Composable
    private fun SelectionPromptPanel() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(DETAILS_PANEL_HEIGHT.dp)
                .border(BORDER_WIDTH_NORMAL.dp, border, RoundedCornerShape(CORNER_RADIUS.dp))
                .background(panel, RoundedCornerShape(CORNER_RADIUS.dp))
                .padding(horizontal = PADDING_HORIZONTAL.dp, vertical = PADDING_VERTICAL_MEDIUM.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Select a card to view details", color = fg)
        }
    }

    // Minimal clickable without ripple to keep code self-contained
    @Composable
    private fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier =
        this.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) {
            onClick()
        }

    private companion object {
        const val BG_COLOR = 0xFF202537L
        const val FG_COLOR = 0xFFFBFBFBL
        const val PANEL_COLOR = 0xFF2C334BL
        const val BORDER_COLOR = 0xFF5D6990L
        const val ACCENT_COLOR = 0xFF0B81FFL
        const val GREEN_STRIPE_COLOR = 0xFF00C249L
        const val GRADIENT_START_COLOR = 0xFF7276F7L
        const val GRADIENT_END_COLOR = 0xFF7C40E8L
        const val MINI_CARD_WIDTH = 83
        const val MINI_CARD_HEIGHT = 58
        const val SELECTION_INDICATOR_SIZE = 14
        const val GREEN_STRIPE_WIDTH = 8
        const val DETAILS_PANEL_HEIGHT = 80
        const val BUTTON_HEIGHT = 48
        const val BUTTON_CORNER_RADIUS = 6
        const val BORDER_WIDTH_SELECTED = 2
        const val BORDER_WIDTH_NORMAL = 1
        const val BORDER_WIDTH_ONE = 1
        const val CHECKMARK_FONT_SIZE = 9
        const val PADDING_BOTTOM = 16
        const val PADDING_TOP = 16
        const val PADDING_SECTION = 12
        const val PADDING_HORIZONTAL = 8
        const val PADDING_VERTICAL_SMALL = 2
        const val PADDING_VERTICAL_MEDIUM = 10
        const val PADDING_DETAILS = 12
        const val PADDING_DETAILS_ROW = 4
        const val SPACING_SMALL = 8
        const val SPACING_MEDIUM = 16
        const val SPACING_SECTION = 12
        const val CORNER_RADIUS = 8
        const val INDICATOR_PADDING = 4
        const val TEXT_ALPHA = 0.9f
        const val DISABLED_BUTTON_ALPHA = 0.35f
        const val DISABLED_TEXT_ALPHA = 0.6f
        const val MIN_CARDS_FOR_SELECTION = 1
        const val SELECTED_COUNT_ZERO = 0
        const val SELECTED_COUNT_ONE = 1
        const val WEIGHT_ONE = 1f
    }
}
