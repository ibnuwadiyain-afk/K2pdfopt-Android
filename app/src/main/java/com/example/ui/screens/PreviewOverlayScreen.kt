package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DetectedBlock
import com.example.model.QueuedDocumentItem
import com.example.util.LocaleFormatUtils
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreviewOverlayScreen(
    locContext: Context,
    localeTag: String,
    queue: List<QueuedDocumentItem>,
    selectedDocId: String?,
    previewPageNumber: Int,
    beforeBitmap: Bitmap?,
    afterBitmap: Bitmap?,
    overlayBlocks: List<DetectedBlock>,
    pageOcrLangOverride: String,
    pageRangeValue: String,
    onSelectDocAndPage: (String, Int) -> Unit,
    onUpdateBlockBounds: (String, Float, Float, Float, Float) -> Unit,
    onCycleProtectPolicy: (String) -> Unit,
    onAddCustomBlock: () -> Unit,
    onChangePageOcrOverride: (String) -> Unit,
    onSavePageOverrides: () -> Unit,
    onResetPageOverrides: () -> Unit,
    onUpdatePageRange: (String) -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0=Split Before/After, 1=Overlay Editor, 2=Page Grid
    var splitFraction by remember { mutableFloatStateOf(0.50f) }
    var selectedBlockId by remember { mutableStateOf<String?>(overlayBlocks.firstOrNull()?.id) }

    val activeDoc = queue.find { it.id == selectedDocId } ?: queue.firstOrNull()
    val totalPages = activeDoc?.pageCount?.coerceAtLeast(1) ?: 1
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("preview_overlay_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Sub-mode Selector (Before/After Split, Overlay Editor, Thumbnail Grid)
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    label = { Text(locContext.getString(R.string.preview_before_after)) },
                    leadingIcon = { Icon(Icons.Default.Compare, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_before_after")
                )
                FilterChip(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    label = { Text(locContext.getString(R.string.preview_overlay_editor)) },
                    leadingIcon = { Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_overlay_editor")
                )
                FilterChip(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    label = { Text(locContext.getString(R.string.preview_thumbnails)) },
                    leadingIcon = { Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_page_grid")
                )
            }
        }

        // 2. Document & Page Navigation Bar (RTL-mirrored arrows)
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            if (activeDoc != null && previewPageNumber > 1) {
                                onSelectDocAndPage(activeDoc.id, previewPageNumber - 1)
                            }
                        },
                        enabled = previewPageNumber > 1,
                        modifier = Modifier.testTag("btn_prev_page")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Page"
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = activeDoc?.fileName ?: "Sample Document",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Page ${LocaleFormatUtils.formatInteger(previewPageNumber.toLong(), localeTag)} / ${
                                LocaleFormatUtils.formatInteger(totalPages.toLong(), localeTag)
                            }",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    IconButton(
                        onClick = {
                            if (activeDoc != null && previewPageNumber < totalPages) {
                                onSelectDocAndPage(activeDoc.id, previewPageNumber + 1)
                            }
                        },
                        enabled = previewPageNumber < totalPages,
                        modifier = Modifier.testTag("btn_next_page")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Page"
                        )
                    }
                }
            }
        }

        // 3. Mode 0: Live Before / After Interactive Split Slider
        if (activeTab == 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${locContext.getString(R.string.preview_before_after)} (${
                                LocaleFormatUtils.formatPercent((splitFraction * 100).roundToInt(), localeTag)
                            })",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.75f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .clipToBounds()
                        ) {
                            val srcImg = beforeBitmap?.asImageBitmap()
                            val dstImg = afterBitmap?.asImageBitmap()

                            if (srcImg != null && dstImg != null) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    val splitX = if (isRtl) w * (1f - splitFraction) else w * splitFraction

                                    // Draw After (Reflowed) across full canvas
                                    drawImage(
                                        image = dstImg,
                                        srcOffset = IntOffset.Zero,
                                        srcSize = IntSize(dstImg.width, dstImg.height),
                                        dstOffset = IntOffset.Zero,
                                        dstSize = IntSize(w.roundToInt(), h.roundToInt())
                                    )

                                    // Clip Before (Original Source) on the leading side of the slider
                                    clipRect(
                                        left = if (isRtl) splitX else 0f,
                                        top = 0f,
                                        right = if (isRtl) w else splitX,
                                        bottom = h
                                    ) {
                                        drawImage(
                                            image = srcImg,
                                            srcOffset = IntOffset.Zero,
                                            srcSize = IntSize(srcImg.width, srcImg.height),
                                            dstOffset = IntOffset.Zero,
                                            dstSize = IntSize(w.roundToInt(), h.roundToInt())
                                        )
                                    }

                                    // Draw cyan divider line
                                    drawLine(
                                        color = Color(0xFF0EA5E9),
                                        start = Offset(splitX, 0f),
                                        end = Offset(splitX, h),
                                        strokeWidth = 6f
                                    )
                                }
                            }
                        }

                        Slider(
                            value = splitFraction,
                            onValueChange = { splitFraction = it.coerceIn(0.05f, 0.95f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("slider_before_after")
                        )
                    }
                }
            }
        }

        // 4. Mode 1: Interactive Overlay Editor (Drag to adjust blocks, crop boxes, and page OCR overrides)
        if (activeTab == 1) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = locContext.getString(R.string.preview_overlay_editor),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Drag any detected region on the page canvas to adjust its bounding box, or tap a block below to toggle its protection policy (Reflow / Scale-to-Fit / Keep-as-Image).",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.75f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .pointerInput(overlayBlocks, selectedBlockId, isRtl) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val w = size.width.toFloat().coerceAtLeast(1f)
                                        val h = size.height.toFloat().coerceAtLeast(1f)
                                        val targetId = selectedBlockId ?: overlayBlocks.firstOrNull()?.id ?: return@detectDragGestures
                                        val blk = overlayBlocks.find { it.id == targetId } ?: return@detectDragGestures
                                        val dx = (if (isRtl) -dragAmount.x else dragAmount.x) / w
                                        val dy = dragAmount.y / h
                                        onUpdateBlockBounds(
                                            blk.id,
                                            blk.leftNorm + dx,
                                            blk.topNorm + dy,
                                            blk.rightNorm + dx,
                                            blk.bottomNorm + dy
                                        )
                                    }
                                }
                        ) {
                            beforeBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Source Page",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.FillBounds
                                )
                            }

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val cw = size.width
                                val ch = size.height
                                overlayBlocks.forEach { blk ->
                                    val leftPx = blk.leftNorm * cw
                                    val topPx = blk.topNorm * ch
                                    val bw = (blk.rightNorm - blk.leftNorm) * cw
                                    val bh = (blk.bottomNorm - blk.topNorm) * ch
                                    val color = Color(blk.type.colorHex)
                                    val isSelected = blk.id == selectedBlockId
                                    drawRect(
                                        color = color.copy(alpha = if (isSelected) 0.28f else 0.14f),
                                        topLeft = Offset(leftPx, topPx),
                                        size = Size(bw, bh)
                                    )
                                    drawRect(
                                        color = color,
                                        topLeft = Offset(leftPx, topPx),
                                        size = Size(bw, bh),
                                        style = Stroke(width = if (isSelected) 6f else 3f)
                                    )
                                }
                            }
                        }

                        // Per-page OCR Language Override field
                        OutlinedTextField(
                            value = pageOcrLangOverride,
                            onValueChange = onChangePageOcrOverride,
                            label = { Text(locContext.getString(R.string.preview_page_override_lang)) },
                            placeholder = { Text("e.g. ara+eng or chi_sim_vert") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onAddCustomBlock,
                                modifier = Modifier.testTag("btn_add_overlay_block")
                            ) {
                                Icon(Icons.Default.AddBox, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(locContext.getString(R.string.preview_add_region))
                            }
                            OutlinedButton(onClick = onResetPageOverrides) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(locContext.getString(R.string.preview_reset_overrides))
                            }
                            Button(
                                onClick = onSavePageOverrides,
                                modifier = Modifier.testTag("btn_save_overlay_overrides")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(locContext.getString(R.string.preview_save_overrides, previewPageNumber))
                            }
                        }
                    }
                }
            }

            // List of Detected Blocks on this Page
            items(overlayBlocks, key = { it.id }) { blk ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (blk.id == selectedBlockId) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedBlockId = blk.id
                            onCycleProtectPolicy(blk.id)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "#${blk.readingOrder} • ${blk.type.label} (${blk.scriptDirection.name})",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Policy: ${blk.protectPolicy.label} • Bounds: [${
                                    String.format("%.2f, %.2f - %.2f, %.2f", blk.leftNorm, blk.topNorm, blk.rightNorm, blk.bottomNorm)
                                }]",
                                style = MaterialTheme.typography.labelSmall
                            )
                            if (blk.extractedText.isNotBlank()) {
                                Text(
                                    text = blk.extractedText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Mode 2: Page Thumbnail Grid & Visual Page-Range Selector
        if (activeTab == 2) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = locContext.getString(R.string.preview_thumbnails),
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedTextField(
                            value = pageRangeValue,
                            onValueChange = onUpdatePageRange,
                            label = { Text("Active Page Range (-p)") },
                            placeholder = { Text("1-5, 8, 10-") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (p in 1..totalPages.coerceAtMost(24)) {
                                val isCur = p == previewPageNumber
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isCur) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .size(width = 92.dp, height = 116.dp)
                                        .clickable {
                                            if (activeDoc != null) {
                                                onSelectDocAndPage(activeDoc.id, p)
                                                activeTab = 0
                                            }
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp),
                                        verticalArrangement = Arrangement.SpaceBetween,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "P.${LocaleFormatUtils.formatInteger(p.toLong(), localeTag)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (isCur) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (isCur) "Selected" else "Tap to Preview",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isCur) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
