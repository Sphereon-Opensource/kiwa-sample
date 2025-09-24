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

package com.sphereon.ui.elicense.engagement.qr

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.renderer.ComposeRenderer
import com.sphereon.ui.card.CredentialCardRenderer
import com.sphereon.ui.elicense.engagement.consent.IMdocInformationRequestPresenter

@Inject
@ContributesRenderer(modelType = IMdocEngagementQrPresenter.Model::class)
class MdocEngagementRenderer(
    private val credentialCardRenderer: CredentialCardRenderer
) : ComposeRenderer<IMdocEngagementQrPresenter.Model>() {
    @Composable
    override fun Compose(model: IMdocEngagementQrPresenter.Model) {
        val bg = Color(0xFF202537)
        val fg = Color(0xFFFBFBFB)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (model) {
                        is IMdocEngagementQrPresenter.Model.Initial -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = true),
                                contentAlignment = Alignment.Center
                            ) { NfcOrQrContent(showQr = false, qr = null) }
                            Spacer(modifier = Modifier.height(16.dp))
                            ButtonsRow(model, fg)
                        }

                        is IMdocEngagementQrPresenter.Model.Engagement -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = true),
                                contentAlignment = Alignment.Center
                            ) { NfcOrQrContent(showQr = model.showQr, qr = model.qrImage) }

                            if (!model.showQr) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "Please hold your phone to the Reader or use the QR code.",
                                    color = fg,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            ButtonsRow(model, fg)
                        }

                        is IMdocEngagementQrPresenter.Model.Connecting -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = true),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color(0xFF7C40E8))
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "Connecting...",
                                        color = fg,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { model.onStateEvent(IMdocEngagementQrPresenter.UiStateEvent.Stopped) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = fg.copy(alpha = 0.12f),
                                    contentColor = fg
                                ),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) { Text("Stop") }
                        }

                        is IMdocEngagementQrPresenter.Model.Selecting -> {
                            val consentModel = model.consentModel
                            val scrollState = rememberScrollState()
                            Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(scrollState)) {
                                Text(
                                    text = "The following information will be shared",
                                    color = fg,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(12.dp))
                                consentModel.docRequestSections.forEachIndexed { sectionIndex, section ->
                                    section.description?.let { desc ->
                                        Text(text = desc, color = fg.copy(alpha = 0.9f))
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    MiniCardsRowInline(
                                        docRequestSection = section,
                                        onSelect = { idx ->
                                            consentModel.onEvent(
                                                IMdocInformationRequestPresenter.Event.OnCardSelected(sectionIndex, idx)
                                            )
                                        },
                                        accent = Color(0xFF0B81FF),
                                        fg = fg,
                                        credentialCardRenderer = credentialCardRenderer
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(section.title, color = fg, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = if (section.selectedIndex != null) "1 Selected" else "0 Selected",
                                            color = Color(0xFF0B81FF)
                                        )
                                    }
                                    if (section.cards.size > 1 && section.selectedIndex == null) {
                                        SelectionPromptInline(fg = fg)
                                    } else if (section.selectedIndex != null) {
                                        DetailsPanelInline(docRequestSection = section, fg = fg)
                                    }
                                    Spacer(Modifier.height(16.dp))
                                }
                            }
                            Button(
                                onClick = { consentModel.onEvent(IMdocInformationRequestPresenter.Event.OnShare) },
                                enabled = consentModel.canContinue,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF7276F7),
                                    contentColor = fg,
                                    disabledContainerColor = Color(0xFF7276F7).copy(alpha = 0.35f),
                                    disabledContentColor = fg.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) { Text("Share") }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { consentModel.onEvent(IMdocInformationRequestPresenter.Event.OnDecline) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = fg.copy(alpha = 0.12f),
                                    contentColor = Color(0xFF7C40E8)
                                ),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) { Text("Decline") }
                        }

                        is IMdocEngagementQrPresenter.Model.Success -> {
                            LaunchedEffect(Unit) {
                                delay(1500)
                                model.onStateEvent(IMdocEngagementQrPresenter.UiStateEvent.SuccessComplete)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = true),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2F364C)),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .padding(32.dp)
                                            .fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Success",
                                            tint = Color(0xFF31C26E),
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            text = "Shared!",
                                            color = fg,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 24.sp
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = "Information sent to the reader",
                                            color = fg.copy(alpha = 0.9f),
                                            fontSize = 16.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        is IMdocEngagementQrPresenter.Model.Stopped -> {
                            Spacer(modifier = Modifier.height(1.dp))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun NfcOrQrContent(showQr: Boolean, qr: androidx.compose.ui.graphics.ImageBitmap?) {
        if (showQr && qr != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(1f)
                    .background(Color.White)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = qr,
                    contentDescription = "QR Code",
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Icon(
                imageVector = Icons.Outlined.Nfc,
                contentDescription = "NFC",
                modifier = Modifier.size(96.dp),
                tint = Color(0xFF7C40E8)
            )
        }
    }

    @Composable
    private fun ButtonsRow(model: IMdocEngagementQrPresenter.Model, fg: Color) {
        when (model) {
            is IMdocEngagementQrPresenter.Model.Engagement -> {
                if (model.showQr) {
                    // Show Stop button when QR is displayed
                    Button(
                        onClick = { model.onStateEvent(IMdocEngagementQrPresenter.UiStateEvent.SuccessComplete) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = fg.copy(alpha = 0.12f),
                            contentColor = fg
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Stop") }
                } else {
                    // Show QR code button when QR is not displayed
                    Button(
                        onClick = { model.onStateEvent(IMdocEngagementQrPresenter.UiStateEvent.ShowQr) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7276F7), contentColor = fg),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Show QR code") }
                }
            }

            else -> {
                // Default behavior for other states (like Initial)
                Button(
                    onClick = { model.onStateEvent(IMdocEngagementQrPresenter.UiStateEvent.ShowQr) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7276F7), contentColor = fg),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Show QR code") }
            }
        }
    }
}

@Composable
private fun MiniCardsRowInline(
    docRequestSection: IMdocInformationRequestPresenter.DocRequestSection,
    onSelect: (Int) -> Unit,
    accent: Color,
    fg: Color,
    credentialCardRenderer: CredentialCardRenderer
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp)
    ) {
        itemsIndexed(docRequestSection.cards) { index: Int, cardModel: com.sphereon.ui.card.ICredentialCardPresenter.Model ->
            val selected = docRequestSection.selectedIndex == index
            val shape = RoundedCornerShape(6.dp)
            Box(
                modifier = Modifier
                    .size(width = 83.dp, height = 58.dp)
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) accent else Color(0xFF5D6990).copy(alpha = 0.5f),
                        shape = shape
                    )
                    .clip(shape)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                // Render mini card
                credentialCardRenderer.renderCompose(cardModel)
                if (selected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(accent),
                        contentAlignment = Alignment.Center
                    ) { Text("✓", color = fg, fontWeight = FontWeight.Bold, fontSize = 9.sp) }
                }
            }
        }
    }
}

@Composable
private fun DetailsPanelInline(
    docRequestSection: IMdocInformationRequestPresenter.DocRequestSection,
    fg: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Box(modifier = Modifier.width(8.dp).fillMaxHeight().background(Color(0xFF00C249)))
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
private fun SelectionPromptInline(fg: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(horizontal = 0.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Select a credential to view details", color = fg)
    }
}
