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

package com.sphereon.kiwa.sample.ui.elicense.engagement.qr

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sphereon.kiwa.sample.ui.card.CredentialCardRenderer
import com.sphereon.kiwa.sample.ui.card.CredentialCardPresenter
import com.sphereon.kiwa.sample.ui.elicense.engagement.consent.MdocInformationRequestPresenter

import kotlinx.coroutines.delay
import me.tatarka.inject.annotations.Inject
import org.publicvalue.multiplatform.qrcode.CodeType
import org.publicvalue.multiplatform.qrcode.ScannerWithPermissions
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.renderer.ComposeRenderer

@Inject
@ContributesRenderer(modelType = MdocEngagementPresenter.Model::class)
@Suppress("TooManyFunctions") // UI renderer class with multiple render functions is appropriate
class MdocEngagementRenderer(
    private val credentialCardRenderer: CredentialCardRenderer
) : ComposeRenderer<MdocEngagementPresenter.Model>() {
    @Composable
    override fun Compose(model: MdocEngagementPresenter.Model) {
        val bg = Color(COLOR_BG)
        val fg = Color(COLOR_FG)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
        ) {
            ComposeContent(model, fg)
        }
    }

    @Composable
    private fun ComposeContent(model: MdocEngagementPresenter.Model, fg: Color) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = PADDING_HORIZONTAL.dp, vertical = PADDING_VERTICAL.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (model) {
                    is MdocEngagementPresenter.Model.Initial -> RenderInitial(model, fg)
                    is MdocEngagementPresenter.Model.Engagement -> RenderEngagement(model, fg)
                    is MdocEngagementPresenter.Model.Connecting -> RenderConnecting(model, fg)
                    is MdocEngagementPresenter.Model.Selecting -> RenderSelecting(model, fg)
                    is MdocEngagementPresenter.Model.Sharing -> RenderSharing(model, fg)
                    is MdocEngagementPresenter.Model.Success -> RenderSuccess(model)
                    is MdocEngagementPresenter.Model.Stopped -> Spacer(modifier = Modifier.height(1.dp))
                }
            }
        }
    }

    @Composable
    private fun RenderInitial(model: MdocEngagementPresenter.Model.Initial, fg: Color) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                contentAlignment = Alignment.Center
            ) {
                NfcOrQrContent(
                    showQr = model.showQr,
                    showQrScanner = model.showQrScanner,
                    qr = null,
                    fg = fg,
                    onQrScanned = model.onQrScanned
                )
            }
            Spacer(modifier = Modifier.height(SPACING_STANDARD.dp))
            ModeToggleRow(
                showQr = model.showQr,
                showQrScanner = model.showQrScanner,
                onShowQr = { model.onStateEvent(MdocEngagementPresenter.UiStateEvent.ShowQr) },
                onShowScanner = { model.onStateEvent(MdocEngagementPresenter.UiStateEvent.ShowQrScanner) },
                onStop = { model.onStateEvent(MdocEngagementPresenter.UiStateEvent.Stopped) },
                fg = fg
            )
        }
    }

    @Composable
    private fun RenderEngagement(model: MdocEngagementPresenter.Model.Engagement, fg: Color) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                contentAlignment = Alignment.Center
            ) {
                NfcOrQrContent(
                    showQr = model.showQr,
                    showQrScanner = model.showQrScanner,
                    qr = model.qrImage,
                    fg = fg,
                    onQrScanned = model.onQrScanned
                )
            }

            if (!model.showQr && !model.showQrScanner) {
                Spacer(Modifier.height(SPACING_SMALL.dp))
                Text(
                    text = "Please hold your phone to the Reader or use the QR code.",
                    color = fg,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(SPACING_STANDARD.dp))
            ModeToggleRow(
                showQr = model.showQr,
                showQrScanner = model.showQrScanner,
                onShowQr = { model.onStateEvent(MdocEngagementPresenter.UiStateEvent.ShowQr) },
                onShowScanner = { model.onStateEvent(MdocEngagementPresenter.UiStateEvent.ShowQrScanner) },
                onStop = { model.onStateEvent(MdocEngagementPresenter.UiStateEvent.Stopped) },
                fg = fg
            )
        }
    }

    @Composable
    private fun RenderConnecting(model: MdocEngagementPresenter.Model.Connecting, fg: Color) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(COLOR_PURPLE))
                    Spacer(Modifier.height(SPACING_SMALL.dp))
                    Text(
                        text = model.engagementEvent?.state?.name?.let { "Status: $it" } ?: "Connecting...",
                        color = fg,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(SPACING_STANDARD.dp))
            Button(
                onClick = { model.onStateEvent(MdocEngagementPresenter.UiStateEvent.Stopped) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = fg.copy(alpha = BUTTON_ALPHA_LOW),
                    contentColor = fg
                ),
                modifier = Modifier.fillMaxWidth().height(BUTTON_HEIGHT.dp)
            ) { Text("Stop") }
        }
    }

    @Composable
    private fun RenderSelecting(model: MdocEngagementPresenter.Model.Selecting, fg: Color) {
        val consentModel = model.consentModel
        if (consentModel !is MdocInformationRequestPresenter.Model.Content) {
            return
        }
        val scrollState = rememberScrollState()
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(scrollState)) {
                Text(
                    text = "The following information will be shared",
                    color = fg,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(SPACING_SMALL.dp))
                RenderConsentSectionContent(consentModel, fg)
            }
            RenderConsentActionButtons(consentModel, fg)
        }
    }

    @Composable
    private fun RenderConsentSectionContent(
        consentModel: MdocInformationRequestPresenter.Model.Content,
        fg: Color
    ) {
        consentModel.docRequestSections.forEachIndexed { sectionIndex, section ->
            section.description?.let { desc ->
                Text(text = desc, color = fg.copy(alpha = ALPHA_HIGH))
            }
            Spacer(Modifier.height(SPACING_TINY.dp))
            MiniCardsRowInline(
                docRequestSection = section,
                onSelect = { idx ->
                    consentModel.onEvent(
                        MdocInformationRequestPresenter.Event.OnCardSelected(sectionIndex, idx)
                    )
                },
                accent = Color(COLOR_ACCENT_BLUE),
                fg = fg,
                credentialCardRenderer = credentialCardRenderer
            )
            Spacer(Modifier.height(SPACING_TINY.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = SPACING_TINY.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(section.title, color = fg, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (section.selectedIndex != null) {
                        "1 Selected"
                    } else {
                        "0 Selected"
                    },
                    color = Color(COLOR_ACCENT_BLUE)
                )
            }
            if (section.cards.size > 1 && section.selectedIndex == null) {
                SelectionPromptInline(fg = fg)
            } else if (section.selectedIndex != null) {
                DetailsPanelInline(docRequestSection = section, fg = fg)
            }
            Spacer(Modifier.height(SPACING_STANDARD.dp))
        }
    }

    @Composable
    private fun RenderConsentActionButtons(
        consentModel: MdocInformationRequestPresenter.Model.Content,
        fg: Color
    ) {
        Button(
            onClick = { consentModel.onEvent(MdocInformationRequestPresenter.Event.OnShare) },
            enabled = consentModel.canContinue,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(COLOR_PURPLE_LIGHT),
                contentColor = fg,
                disabledContainerColor = Color(COLOR_PURPLE_LIGHT).copy(
                    alpha = BUTTON_ALPHA_DISABLED
                ),
                disabledContentColor = fg.copy(
                    alpha = TEXT_ALPHA_DISABLED
                )
            ),
            modifier = Modifier.fillMaxWidth().height(BUTTON_HEIGHT.dp)
        ) { Text("Share") }
        Spacer(Modifier.height(SPACING_TINY.dp))
        Button(
            onClick = { consentModel.onEvent(MdocInformationRequestPresenter.Event.OnDecline) },
            colors = ButtonDefaults.buttonColors(
                containerColor = fg.copy(alpha = BUTTON_ALPHA_LOW),
                contentColor = Color(COLOR_PURPLE)
            ),
            modifier = Modifier.fillMaxWidth().height(BUTTON_HEIGHT.dp)
        ) { Text("Decline") }
    }

    @Composable
    private fun RenderSuccess(model: MdocEngagementPresenter.Model.Success) {
        Column(modifier = Modifier.fillMaxSize()) {
            LaunchedEffect(Unit) {
                delay(DELAY_SUCCESS_MS)
                model.onStateEvent(MdocEngagementPresenter.UiStateEvent.SuccessComplete)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                contentAlignment = Alignment.Center
            ) {
                val fg = Color(COLOR_FG)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(COLOR_CARD_BG)),
                    modifier = Modifier.padding(SPACING_STANDARD.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(PADDING_CARD.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(COLOR_SUCCESS),
                            modifier = Modifier.size(ICON_SIZE_SUCCESS.dp)
                        )
                        Spacer(Modifier.height(SPACING_STANDARD.dp))
                        Text(
                            text = "Shared!",
                            color = fg,
                            fontWeight = FontWeight.Bold,
                            fontSize = FONT_SIZE_TITLE.sp
                        )
                        Spacer(Modifier.height(SPACING_TINY.dp))
                        Text(
                            text = "Information sent to the reader",
                            color = fg.copy(alpha = ALPHA_HIGH),
                            fontSize = FONT_SIZE_BODY.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    @Composable
    @Suppress("UnusedParameter") // model parameter required by interface but not used in this implementation
    private fun RenderSharing(model: MdocEngagementPresenter.Model.Sharing, fg: Color) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(COLOR_PURPLE))
                    Spacer(Modifier.height(SPACING_SMALL.dp))
                    Text(
                        text = "Sharing credentials...",
                        color = fg,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(SPACING_STANDARD.dp))
            Button(
                onClick = { model.onStateEvent(MdocEngagementPresenter.UiStateEvent.Stopped) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = fg.copy(alpha = BUTTON_ALPHA_LOW),
                    contentColor = fg
                ),
                modifier = Modifier.fillMaxWidth().height(BUTTON_HEIGHT.dp)
            ) { Text("Stop") }
        }
    }

    @Composable
    private fun QrScannerContent(onQrScanned: (String) -> Unit, fg: Color) {
        var hasScanned by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxSize()) {
            // Use ScannerWithPermissions and debounce scan events
            ScannerWithPermissions(
                onScanned = { result: String ->
                    println("🔍 Scanner detected QR: '${result.take(50)}...' (length: ${result.length})")
                    if (!hasScanned && result.isNotBlank()) {
                        println("✓ Passing to callback...")
                        hasScanned = true
                        onQrScanned(result)
                    } else {
                        println("✗ Rejected (hasScanned=$hasScanned, isBlank=${result.isBlank()})")
                    }
                    true // Return true to stop scanning
                },
                types = listOf(CodeType.QR)
            )
            // Overlay instructions (always visible)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
                    .background(
                        color = Color(COLOR_BG).copy(alpha = 0.8f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Scan Reader QR Code",
                    color = fg,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Position the QR code within the frame",
                    color = fg.copy(alpha = ALPHA_HIGH),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Must start with mdoc://",
                    color = Color(COLOR_ACCENT_BLUE),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    @Composable
    private fun NfcOrQrContent(
        showQr: Boolean,
        showQrScanner: Boolean,
        qr: androidx.compose.ui.graphics.ImageBitmap?,
        fg: Color,
        onQrScanned: (String) -> Unit
    ) {
        when {
            showQr && qr != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(FILL_FRACTION)
                        .aspectRatio(ASPECT_RATIO_SQUARE)
                        .background(Color.White)
                        .padding(QR_PADDING.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = qr,
                        contentDescription = "QR Code",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            showQrScanner -> {
                QrScannerContent(onQrScanned = onQrScanned, fg = fg)
            }

            else -> {
                Icon(
                    imageVector = Icons.Outlined.Nfc,
                    contentDescription = "NFC",
                    modifier = Modifier.size(NFC_ICON_SIZE.dp),
                    tint = Color(COLOR_PURPLE)
                )
            }
        }
    }

    @Composable
    private fun ModeToggleRow(
        showQr: Boolean,
        showQrScanner: Boolean,
        onShowQr: () -> Unit,
        onShowScanner: () -> Unit,
        onStop: () -> Unit,
        fg: Color
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Mode selector with rounded tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(
                        color = fg.copy(alpha = BUTTON_ALPHA_LOW),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // QR Display Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            color = if (showQr) Color(COLOR_PURPLE_LIGHT) else Color.Transparent,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable {
                            if (!showQr) {
                                onShowQr()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Display QR",
                        color = fg,
                        fontWeight = if (showQr) FontWeight.Bold else FontWeight.Normal
                    )
                }

                // QR Scanner Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            color = if (showQrScanner) Color(COLOR_PURPLE_LIGHT) else Color.Transparent,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable {
                            if (!showQrScanner) {
                                onShowScanner()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Scan QR",
                        color = fg,
                        fontWeight = if (showQrScanner) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            // Stop button below the tabs (always visible for consistent layout)
            Spacer(modifier = Modifier.height(SPACING_SMALL.dp))
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = fg.copy(alpha = BUTTON_ALPHA_LOW),
                    contentColor = fg
                ),
                modifier = Modifier.fillMaxWidth().height(BUTTON_HEIGHT.dp)
            ) {
                Text("Stop")
            }
        }
    }

    @Composable
    private fun MiniCardsRowInline(
        docRequestSection: MdocInformationRequestPresenter.DocRequestSection,
        onSelect: (Int) -> Unit,
        accent: Color,
        fg: Color,
        credentialCardRenderer: CredentialCardRenderer
    ) {
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp, Alignment.Start),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = PADDING_ZERO.dp)
        ) {
            itemsIndexed(docRequestSection.cards) { index: Int, cardModel: CredentialCardPresenter.Model ->
                val selected = docRequestSection.selectedIndex == index
                val shape = RoundedCornerShape(CORNER_RADIUS.dp)
                Box(
                    modifier = Modifier
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
                                Color(COLOR_BORDER_GRAY).copy(alpha = ALPHA_MEDIUM)
                            },
                            shape = shape
                        )
                        .clip(shape)
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    credentialCardRenderer.renderCompose(cardModel)
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(SELECTION_INDICATOR_PADDING.dp)
                                .size(SELECTION_INDICATOR_SIZE.dp)
                                .clip(CircleShape)
                                .background(accent),
                            contentAlignment = Alignment.Center
                        ) { Text("✓", color = fg, fontWeight = FontWeight.Bold, fontSize = CHECKMARK_FONT_SIZE.sp) }
                    }
                }
            }
        }
    }

    @Composable
    private fun DetailsPanelInline(
        docRequestSection: MdocInformationRequestPresenter.DocRequestSection,
        fg: Color
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(modifier = Modifier.width(DETAILS_BORDER_WIDTH.dp).fillMaxHeight().background(Color(COLOR_SUCCESS)))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SPACING_SMALL.dp)
            ) {
                docRequestSection.details.forEach { row ->
                    Column(modifier = Modifier.padding(vertical = PADDING_DETAIL_ROW.dp)) {
                        Text(row.label, color = fg.copy(alpha = ALPHA_HIGH))
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
                .height(SELECTION_PROMPT_HEIGHT.dp)
                .padding(horizontal = PADDING_ZERO.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Select a credential to view details", color = fg)
        }
    }
}

private const val COLOR_BG = 0xFF202537
private const val COLOR_FG = 0xFFFBFBFB
private const val COLOR_PURPLE = 0xFF7C40E8
private const val COLOR_PURPLE_LIGHT = 0xFF7276F7
private const val COLOR_ACCENT_BLUE = 0xFF0B81FF
private const val COLOR_SUCCESS = 0xFF31C26E
private const val COLOR_CARD_BG = 0xFF2F364C
private const val COLOR_BORDER_GRAY = 0xFF5D6990
private const val PADDING_HORIZONTAL = 16
private const val PADDING_VERTICAL = 12
private const val PADDING_CARD = 32
private const val PADDING_ZERO = 0
private const val PADDING_DETAIL_ROW = 4
private const val SPACING_TINY = 8
private const val SPACING_SMALL = 12
private const val SPACING_STANDARD = 16
private const val BUTTON_HEIGHT = 48
private const val BUTTON_ALPHA_LOW = 0.12f
private const val BUTTON_ALPHA_DISABLED = 0.35f
private const val TEXT_ALPHA_DISABLED = 0.6f
private const val ALPHA_HIGH = 0.9f
private const val ALPHA_MEDIUM = 0.5f
private const val FILL_FRACTION = 0.8f
private const val ASPECT_RATIO_SQUARE = 1f
private const val QR_PADDING = 12
private const val NFC_ICON_SIZE = 96
private const val ICON_SIZE_SUCCESS = 64
private const val FONT_SIZE_TITLE = 24
private const val FONT_SIZE_BODY = 16
private const val DELAY_SUCCESS_MS = 1500L
private const val SELECTION_PROMPT_HEIGHT = 80
private const val CORNER_RADIUS = 6
private const val MINI_CARD_WIDTH = 83
private const val MINI_CARD_HEIGHT = 58
private const val BORDER_WIDTH_SELECTED = 2
private const val BORDER_WIDTH_NORMAL = 1
private const val SELECTION_INDICATOR_PADDING = 4
private const val SELECTION_INDICATOR_SIZE = 14
private const val CHECKMARK_FONT_SIZE = 9
private const val DETAILS_BORDER_WIDTH = 8
