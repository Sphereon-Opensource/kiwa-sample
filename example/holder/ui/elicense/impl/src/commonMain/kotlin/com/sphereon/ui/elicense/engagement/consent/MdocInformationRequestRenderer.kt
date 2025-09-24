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

package com.sphereon.ui.elicense.engagement.consent

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
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.renderer.ComposeRenderer
import com.sphereon.ui.card.CredentialCardRenderer

@Inject
@ContributesRenderer(modelType = IMdocInformationRequestPresenter.Model::class)
class MdocInformationRequestRenderer(
    private val credentialCardRenderer: CredentialCardRenderer
) : ComposeRenderer<IMdocInformationRequestPresenter.Model>() {

    private val bg = Color(0xFF202537)
    private val fg = Color(0xFFFBFBFB)
    private val panel = Color(0xFF2C334B)
    private val border = Color(0xFF5D6990)
    private val accent = Color(0xFF0B81FF)

    @Composable
    override fun Compose(model: IMdocInformationRequestPresenter.Model) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                // Scrollable content
                val scrollState = rememberScrollState()
                Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(scrollState)) {
                    Text(
                        text = "The following information will be shared",
                        color = fg,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
                    )

                    model.docRequestSections.forEachIndexed { sectionIndex, section ->
                        section.description?.let { desc ->
                            Text(
                                text = desc,
                                color = fg.copy(alpha = 0.9f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        // Horizontal mini cards
                        MiniCardsRow(
                            docRequestSection = section,
                            onSelect = { idx ->
                                model.onEvent(
                                    IMdocInformationRequestPresenter.Event.OnCardSelected(sectionIndex, idx)
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Header row under cards
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(section.title, color = fg, fontWeight = FontWeight.SemiBold)
                            val countSel = section.selectedIndex?.let { 1 } ?: 0
                            Text("$countSel Selected", color = accent)
                        }

                        // Details panel
                        when {
                            section.cards.size > 1 && section.selectedIndex == null -> {
                                SelectionPromptPanel()
                            }

                            section.selectedIndex != null -> {
                                DetailsPanel(section)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Content ends; bottom buttons below remain visible
                // Bottom buttons
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    val gradientStart = Color(0xFF7276F7)
                    val gradientEnd = Color(0xFF7C40E8)
                    Button(
                        onClick = { model.onEvent(IMdocInformationRequestPresenter.Event.OnShare) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = gradientStart,
                            contentColor = fg,
                            disabledContainerColor = gradientStart.copy(alpha = 0.35f),
                            disabledContentColor = fg.copy(alpha = 0.6f)
                        ),
                        enabled = model.canContinue,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) { Text("Share", fontWeight = FontWeight.Medium) }

                    Spacer(Modifier.height(12.dp))

                    // Outlined secondary with gradient text look (approximation)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .border(1.dp, gradientEnd, RoundedCornerShape(6.dp))
                            .clickableNoIndication { model.onEvent(IMdocInformationRequestPresenter.Event.OnDecline) },
                        contentAlignment = Alignment.Center
                    ) { Text("Decline", color = fg, fontWeight = FontWeight.Medium) }
                }
            }
        }
    }

    @Composable
    private fun MiniCardsRow(
        docRequestSection: IMdocInformationRequestPresenter.DocRequestSection,
        onSelect: (Int) -> Unit
    ) {
        val shape: Shape = RoundedCornerShape(6.dp)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
            itemsIndexed(
                docRequestSection.cards
            ) { index: Int, cardModel: com.sphereon.ui.card.ICredentialCardPresenter.Model ->
                val selected = docRequestSection.selectedIndex == index
                val baseModifier = if (selected) Modifier.shadow(8.dp, shape) else Modifier
                Box(
                    modifier = baseModifier
                        .size(width = 83.dp, height = 58.dp)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) accent else border.copy(alpha = 0.5f),
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
                                .padding(4.dp)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓", color = fg, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun DetailsPanel(docRequestSection: IMdocInformationRequestPresenter.DocRequestSection) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
        ) {
            Box(modifier = Modifier.width(8.dp).fillMaxHeight().background(Color(0xFF00C249)))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, border, RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                    .padding(12.dp)
            ) {
                docRequestSection.details.forEach { row ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(row.label, color = fg.copy(alpha = 0.9f))
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
                .height(80.dp)
                .border(1.dp, border, RoundedCornerShape(8.dp))
                .background(panel, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Select a card to view details", color = fg)
        }
    }
}

// Minimal clickable without ripple to keep code self-contained
@Composable
private fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier =
    this.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClick() }
