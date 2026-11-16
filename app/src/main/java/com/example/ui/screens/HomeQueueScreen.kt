package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BatchReportEntity
import com.example.model.K2Settings
import com.example.model.QueuedDocumentItem
import com.example.service.DeviceThermalBatteryState
import com.example.util.LocaleFormatUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeQueueScreen(
    locContext: Context,
    localeTag: String,
    queue: List<QueuedDocumentItem>,
    batchReports: List<BatchReportEntity>,
    settings: K2Settings,
    activeProfileName: String,
    deviceHealth: DeviceThermalBatteryState,
    watchedFolderPath: String,
    stressTestStatus: String,
    onAddFilesClick: () -> Unit,
    onGenerateCorpusClick: () -> Unit,
    onToggleWatchFolder: () -> Unit,
    onOptimizePhoneClick: () -> Unit,
    onStartBatchClick: () -> Unit,
    onPauseJobClick: () -> Unit,
    onResumeJobClick: () -> Unit,
    onCancelJobClick: () -> Unit,
    onRun300PageTestClick: () -> Unit,
    onApplyRecommendedProfile: (QueuedDocumentItem) -> Unit,
    onOpenInEditor: (String) -> Unit,
    onOpenInReader: () -> Unit,
    onShareOutput: (String, String) -> Unit,
    onExportApkClick: () -> Unit = {}
) {
    val res = locContext.resources

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_queue_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Hero Banner Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_reflow),
                        contentDescription = locContext.getString(R.string.app_name),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(168.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(168.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x66090E1A),
                                        Color(0xEB090E1A)
                                    )
                                )
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column {
                            Text(
                                text = locContext.getString(R.string.app_name) + " v2.55",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = locContext.getString(R.string.app_tagline),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFE2E8F0),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = locContext.getString(
                                    R.string.home_active_profile_format,
                                    activeProfileName,
                                    settings.outputFormat.ext.uppercase()
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }
                }
            }
        }

        // 2. Primary Action Toolbar
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onAddFilesClick,
                            modifier = Modifier.testTag("btn_add_files")
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(locContext.getString(R.string.home_add_files))
                        }

                        FilledTonalButton(
                            onClick = onOptimizePhoneClick,
                            modifier = Modifier.testTag("btn_optimize_phone")
                        ) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(locContext.getString(R.string.home_optimize_this_phone))
                        }

                        OutlinedButton(
                            onClick = onGenerateCorpusClick,
                            modifier = Modifier.testTag("btn_generate_corpus")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(locContext.getString(R.string.home_generate_samples))
                        }

                        OutlinedButton(
                            onClick = onToggleWatchFolder,
                            modifier = Modifier.testTag("btn_watch_folder")
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (watchedFolderPath.isEmpty()) {
                                    locContext.getString(R.string.home_watch_folder)
                                } else {
                                    "Watching: samples/"
                                }
                            )
                        }

                        OutlinedButton(
                            onClick = onRun300PageTestClick,
                            modifier = Modifier.testTag("btn_300_page_test")
                        ) {
                            Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("300-Page Stability Test")
                        }

                        OutlinedButton(
                            onClick = onExportApkClick,
                            modifier = Modifier.testTag("btn_export_apk")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save / Export APK")
                        }
                    }

                    if (stressTestStatus.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stressTestStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Batch execution & Pause / Resume / Cancel controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onStartBatchClick,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_start_batch")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "${locContext.getString(R.string.home_start_batch)} (${
                                    res.getQuantityString(
                                        R.plurals.plural_files_queued,
                                        queue.size,
                                        queue.size
                                    )
                                })"
                            )
                        }
                        IconButton(
                            onClick = onPauseJobClick,
                            modifier = Modifier.testTag("btn_pause_batch")
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = locContext.getString(R.string.home_pause_job))
                        }
                        IconButton(
                            onClick = onResumeJobClick,
                            modifier = Modifier.testTag("btn_resume_batch")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = locContext.getString(R.string.home_resume_job))
                        }
                        IconButton(
                            onClick = onCancelJobClick,
                            modifier = Modifier.testTag("btn_cancel_batch")
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = locContext.getString(R.string.home_cancel_job))
                        }
                    }
                }
            }
        }

        // 3. Device Thermal & Battery Health Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = locContext.getString(R.string.home_thermal_status_label),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "Battery: ${LocaleFormatUtils.formatPercent(deviceHealth.batteryPercent, localeTag)} • Thermal: ${deviceHealth.thermalStatusLabel} • Active OCR Threads: ${LocaleFormatUtils.formatInteger(deviceHealth.recommendedOcrThreads.toLong(), localeTag)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // 4. Queue Items
        if (queue.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = locContext.getString(R.string.home_empty_queue_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = locContext.getString(R.string.home_empty_queue_subtitle),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            items(queue, key = { it.id }) { doc ->
                QueueDocumentCard(
                    locContext = locContext,
                    localeTag = localeTag,
                    doc = doc,
                    onApplyRecommendedProfile = { onApplyRecommendedProfile(doc) },
                    onOpenInEditor = { onOpenInEditor(doc.id) },
                    onOpenInReader = onOpenInReader,
                    onShareOutput = onShareOutput
                )
            }
        }

        // 5. Batch Conversion Reports
        if (batchReports.isNotEmpty()) {
            item {
                Text(
                    text = locContext.getString(R.string.home_batch_report_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(batchReports.take(6), key = { it.id }) { rep ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "${rep.fileName} → ${rep.outputFormat}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${
                                res.getQuantityString(
                                    R.plurals.plural_pages_processed,
                                    rep.pagesProcessed,
                                    rep.pagesProcessed
                                )
                            } • OCR: ${LocaleFormatUtils.formatPercent(rep.avgOcrConfidence, localeTag)} • Size: ${
                                LocaleFormatUtils.formatFileSize(rep.outputSizeBytes, localeTag)
                            } • Time: ${LocaleFormatUtils.formatInteger(rep.elapsedMs, localeTag)} ms",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = rep.warningsSummary,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QueueDocumentCard(
    locContext: Context,
    localeTag: String,
    doc: QueuedDocumentItem,
    onApplyRecommendedProfile: () -> Unit,
    onOpenInEditor: () -> Unit,
    onOpenInReader: () -> Unit,
    onShareOutput: (String, String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("queue_card_${doc.id}"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = doc.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${LocaleFormatUtils.formatInteger(doc.pageCount.toLong(), localeTag)} pages • ${
                            LocaleFormatUtils.formatFileSize(doc.fileSizeBytes, localeTag)
                        } • Status: ${doc.status}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = onOpenInEditor,
                    label = { Text(locContext.getString(R.string.nav_preview)) },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            // Auto-detected Document Type & Rationale Banner
            doc.analysis?.let { analysis ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Auto-Detected: ${analysis.detectedDocType.title}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "-ocrlang ${analysis.suggestedOcrLangs}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Text(
                            text = analysis.rationale,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = onApplyRecommendedProfile,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(locContext.getString(R.string.detect_accept_profile))
                            }
                        }
                    }
                }
            }

            if (doc.status == "PROCESSING" || doc.progressPercent in 1..99) {
                LinearProgressIndicator(
                    progress = { doc.progressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }

            // Quality scores & OCR output preview after conversion
            if (doc.pageQualityScores.isNotEmpty()) {
                val avgQuality = doc.pageQualityScores.map { it.overallScore }.average().toInt()
                val lowPages = doc.pageQualityScores.count { it.flaggedForReview }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = locContext.getString(
                            R.string.home_page_quality_format,
                            avgQuality,
                            doc.avgOcrConfidence
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    if (lowPages > 0) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Text(
                            text = locContext.getString(R.string.home_low_quality_warning, lowPages),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (doc.extractedTextPreview.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Extracted Multilingual OCR / Text Layer Preview:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = doc.extractedTextPreview,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (doc.outputFilePath != null) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(onClick = onOpenInReader) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(locContext.getString(R.string.nav_reader))
                    }
                    OutlinedButton(
                        onClick = { onShareOutput(doc.outputFilePath, doc.outputFormat.mimeType) }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "${locContext.getString(R.string.profiles_export_json)} (${
                                LocaleFormatUtils.formatFileSize(doc.outputSizeBytes, localeTag)
                            })"
                        )
                    }
                }
            }
        }
    }
}
