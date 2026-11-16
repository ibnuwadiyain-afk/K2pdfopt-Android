package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.K2Settings
import com.example.ocr.TessModelEntry
import com.example.util.LocaleFormatUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LanguageManagerScreen(
    locContext: Context,
    localeTag: String,
    catalog: List<TessModelEntry>,
    totalStorageBytes: Long,
    settings: K2Settings,
    onToggleQualityFastOrBest: (Boolean) -> Unit,
    onToggleLanguageInPriority: (String) -> Unit,
    onDownloadLanguage: (String, Boolean) -> Unit,
    onDeleteLanguage: (String) -> Unit,
    onImportCustomTraineddataClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val useBestQuality = !settings.ocrFastMode

    val filtered = remember(catalog, searchQuery) {
        if (searchQuery.isBlank()) {
            catalog
        } else {
            val q = searchQuery.trim().lowercase()
            catalog.filter {
                it.englishName.lowercase().contains(q) ||
                    it.nativeName.lowercase().contains(q) ||
                    it.code.lowercase().contains(q) ||
                    it.scriptFamily.lowercase().contains(q)
            }
        }
    }

    val installedCount = catalog.count { it.isInstalledFast || it.isInstalledBest }
    val activeCodes = settings.ocrLanguages.split("+").map { it.trim() }.filter { it.isNotEmpty() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("language_manager_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Storage Summary, Quality Mode (Fast vs Best), and Multi-Language Priority Order
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = locContext.getString(
                            R.string.ocr_storage_summary,
                            installedCount,
                            LocaleFormatUtils.formatFileSize(totalStorageBytes, localeTag)
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = locContext.getString(
                            R.string.ocr_selected_priority,
                            settings.ocrLanguages
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = settings.ocrFastMode,
                            onClick = { onToggleQualityFastOrBest(true) },
                            label = { Text(locContext.getString(R.string.ocr_quality_fast)) }
                        )
                        FilterChip(
                            selected = !settings.ocrFastMode,
                            onClick = { onToggleQualityFastOrBest(false) },
                            label = { Text(locContext.getString(R.string.ocr_quality_best)) }
                        )
                    }

                    OutlinedButton(
                        onClick = onImportCustomTraineddataClick,
                        modifier = Modifier.testTag("btn_import_traineddata")
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(locContext.getString(R.string.ocr_import_traineddata))
                    }
                }
            }
        }

        // 2. Search Bar (English, Native Script, or ISO Code)
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text(locContext.getString(R.string.ocr_search_languages)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ocr_search_input")
            )
        }

        // 3. Language Models List
        items(filtered, key = { it.code }) { model ->
            val isSelectedInPriority = model.code in activeCodes
            val isInstalled = if (useBestQuality) model.isInstalledBest else model.isInstalledFast
            val expectedSize = if (useBestQuality) model.bestSizeBytes else model.fastSizeBytes

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tess_item_${model.code}"),
                shape = RoundedCornerShape(14.dp)
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
                                text = "${model.englishName} • ${model.nativeName} (${model.code})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = buildString {
                                    append("Script: ${model.scriptFamily}")
                                    if (model.isRtl) append(" • RTL Bidirectional")
                                    if (model.isVerticalCapable) append(" • Vertical CJK Capable")
                                    append(" • Size: ")
                                    append(
                                        LocaleFormatUtils.formatFileSize(
                                            if (model.actualBytesOnDisk > 0) model.actualBytesOnDisk else expectedSize,
                                            localeTag
                                        )
                                    )
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        FilterChip(
                            selected = isSelectedInPriority,
                            onClick = { onToggleLanguageInPriority(model.code) },
                            label = {
                                Text(
                                    if (isSelectedInPriority) {
                                        "#${activeCodes.indexOf(model.code) + 1} Active"
                                    } else {
                                        "+ Select"
                                    }
                                )
                            }
                        )
                    }

                    if (model.isDownloading) {
                        LinearProgressIndicator(
                            progress = { model.downloadProgressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (model.sha256Hex.isNotEmpty()) {
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text("${locContext.getString(R.string.ocr_verified_checksum)}: ${model.sha256Hex}")
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        } else {
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isInstalled) {
                                FilledTonalButton(
                                    onClick = { onDownloadLanguage(model.code, useBestQuality) }
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Installed")
                                }
                                IconButton(
                                    onClick = { onDeleteLanguage(model.code) },
                                    modifier = Modifier.testTag("btn_delete_${model.code}")
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = locContext.getString(R.string.ocr_delete),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { onDownloadLanguage(model.code, useBestQuality) },
                                    modifier = Modifier.testTag("btn_download_${model.code}")
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(locContext.getString(R.string.ocr_download))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
