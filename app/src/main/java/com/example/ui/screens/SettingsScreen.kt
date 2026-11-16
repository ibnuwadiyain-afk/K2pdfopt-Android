package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.example.model.ConversionMode
import com.example.model.DevicePreset
import com.example.model.FootnotePlacement
import com.example.model.K2Settings
import com.example.model.OutputFormat
import com.example.model.ProtectPolicy
import com.example.util.LocaleFormatUtils
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    locContext: Context,
    localeTag: String,
    settings: K2Settings,
    estimatedSizeKb: Int,
    darkTheme: Boolean,
    einkTheme: Boolean,
    onUpdateSettings: (K2Settings) -> Unit,
    onApplyRawCli: (String) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onToggleEinkTheme: (Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var rawCliInput by remember(settings.rawCliFlags) { mutableStateOf(settings.rawCliFlags) }

    fun matchesSearch(vararg texts: String): Boolean {
        if (searchQuery.isBlank()) return true
        val q = searchQuery.trim().lowercase()
        return texts.any { it.lowercase().contains(q) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Search Bar & Live CLI Equivalent Banner
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text(locContext.getString(R.string.settings_search_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_search_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Active k2pdfopt Command-Line Flags:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = settings.toCliString(),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        // GROUP 1: Device Preset & Dimensions (-dev, -w, -h, -dpi)
        if (matchesSearch(
                locContext.getString(R.string.settings_group_device),
                locContext.getString(R.string.setting_device_preset),
                "-dev", "-w", "-h", "-dpi"
            )
        ) {
            item {
                SettingsGroupCard(title = locContext.getString(R.string.settings_group_device)) {
                    SettingHeaderWithTooltip(
                        title = locContext.getString(R.string.setting_device_preset),
                        tooltip = locContext.getString(R.string.tooltip_device_preset)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DevicePreset.BUILTIN_PRESETS.forEach { preset ->
                            FilterChip(
                                selected = settings.devicePresetId == preset.id,
                                onClick = {
                                    onUpdateSettings(
                                        settings.copy(
                                            devicePresetId = preset.id,
                                            dstWidthPx = preset.widthPx,
                                            dstHeightPx = preset.heightPx,
                                            dstDpi = preset.dpi
                                        )
                                    )
                                },
                                label = { Text("${preset.name} (${preset.dpi}dpi)") }
                            )
                        }
                    }
                    Text(
                        text = "Resolution: ${LocaleFormatUtils.formatInteger(settings.dstWidthPx.toLong(), localeTag)} × ${
                            LocaleFormatUtils.formatInteger(settings.dstHeightPx.toLong(), localeTag)
                        } px @ ${LocaleFormatUtils.formatInteger(settings.dstDpi.toLong(), localeTag)} DPI",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = settings.dstDpi.toFloat(),
                        onValueChange = { onUpdateSettings(settings.copy(dstDpi = it.roundToInt())) },
                        valueRange = 120f..450f
                    )
                }
            }
        }

        // GROUP 2: Conversion Mode, Columns, Justification & RTL/CJK (-mode, -col, -wrap, -hy, -j, -r2l)
        if (matchesSearch(
                locContext.getString(R.string.settings_group_mode),
                "-mode", "-col", "-wrap", "-hy", "-j", "-r2l", "cjk"
            )
        ) {
            item {
                SettingsGroupCard(title = locContext.getString(R.string.settings_group_mode)) {
                    SettingHeaderWithTooltip(
                        title = locContext.getString(R.string.setting_conversion_mode),
                        tooltip = locContext.getString(R.string.tooltip_conversion_mode)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ConversionMode.entries.forEach { m ->
                            FilterChip(
                                selected = settings.mode == m,
                                onClick = { onUpdateSettings(settings.copy(mode = m)) },
                                label = { Text(m.label) }
                            )
                        }
                    }

                    SettingHeaderWithTooltip(
                        title = "${locContext.getString(R.string.setting_max_columns)}: ${settings.maxColumns}",
                        tooltip = locContext.getString(R.string.tooltip_max_columns)
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3, 4).forEach { col ->
                            FilterChip(
                                selected = settings.maxColumns == col,
                                onClick = { onUpdateSettings(settings.copy(maxColumns = col)) },
                                label = { Text("-col $col") }
                            )
                        }
                    }

                    SettingSwitchRow(
                        title = locContext.getString(R.string.setting_wrap_text),
                        tooltip = locContext.getString(R.string.tooltip_wrap_text),
                        checked = settings.wrapText,
                        onCheckedChange = { onUpdateSettings(settings.copy(wrapText = it)) }
                    )

                    SettingSwitchRow(
                        title = locContext.getString(R.string.setting_hyphenation),
                        tooltip = locContext.getString(R.string.tooltip_hyphenation),
                        checked = settings.keepHyphens,
                        onCheckedChange = { onUpdateSettings(settings.copy(keepHyphens = it)) }
                    )

                    SettingHeaderWithTooltip(
                        title = locContext.getString(R.string.setting_justification),
                        tooltip = locContext.getString(R.string.tooltip_justification)
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(-1 to "Auto (-j -1)", 0 to "Left (-j 0)", 1 to "Center (-j 1)", 2 to "Right (-j 2)", 3 to "Full (-j 3)").forEach { (jVal, label) ->
                            FilterChip(
                                selected = settings.justification == jVal,
                                onClick = { onUpdateSettings(settings.copy(justification = jVal)) },
                                label = { Text(label) }
                            )
                        }
                    }

                    SettingSwitchRow(
                        title = locContext.getString(R.string.setting_rtl_mode),
                        tooltip = locContext.getString(R.string.tooltip_rtl_mode),
                        checked = settings.rtlMode,
                        onCheckedChange = { onUpdateSettings(settings.copy(rtlMode = it)) }
                    )

                    SettingSwitchRow(
                        title = locContext.getString(R.string.setting_vertical_cjk),
                        tooltip = locContext.getString(R.string.tooltip_vertical_cjk),
                        checked = settings.verticalCjk,
                        onCheckedChange = { onUpdateSettings(settings.copy(verticalCjk = it)) }
                    )
                }
            }
        }

        // GROUP 3: Cropping & Odd/Even Margins (-ac, -ml, -mt, -mr, -mb)
        if (matchesSearch(
                locContext.getString(R.string.settings_group_margins),
                "-ac", "-ml", "-mr", "-mt", "-mb"
            )
        ) {
            item {
                SettingsGroupCard(title = locContext.getString(R.string.settings_group_margins)) {
                    SettingHeaderWithTooltip(
                        title = "${locContext.getString(R.string.setting_auto_crop)}: ${String.format("%.2f", settings.autoCropThreshold)}",
                        tooltip = locContext.getString(R.string.tooltip_auto_crop)
                    )
                    Slider(
                        value = settings.autoCropThreshold,
                        onValueChange = { onUpdateSettings(settings.copy(autoCropThreshold = it)) },
                        valueRange = 0.0f..0.50f
                    )

                    SettingHeaderWithTooltip(
                        title = "${locContext.getString(R.string.setting_odd_even_margins)} (L:${String.format("%.2f", settings.marginLeftIn)}\" R:${String.format("%.2f", settings.marginRightIn)}\")",
                        tooltip = locContext.getString(R.string.tooltip_odd_even_margins)
                    )
                    Slider(
                        value = settings.marginLeftIn,
                        onValueChange = {
                            onUpdateSettings(
                                settings.copy(
                                    marginLeftIn = it,
                                    marginRightIn = it,
                                    oddMarginLeftIn = it * 1.2f,
                                    evenMarginRightIn = it * 1.2f
                                )
                            )
                        },
                        valueRange = 0.0f..1.20f
                    )
                }
            }
        }

        // GROUP 4: Contrast, Gamma, Dither, Dark/E-Ink & Leptonica Scan Cleanup (-c, -g, -sh, -d, -bpc, -neg, -as)
        if (matchesSearch(
                locContext.getString(R.string.settings_group_image),
                "-c", "-g", "-sh", "-d", "-bpc", "-neg", "-as", "leptonica", "e-ink"
            )
        ) {
            item {
                SettingsGroupCard(title = locContext.getString(R.string.settings_group_image)) {
                    SettingHeaderWithTooltip(
                        title = "${locContext.getString(R.string.setting_contrast)}: ${String.format("%.2f", settings.contrast)}",
                        tooltip = locContext.getString(R.string.tooltip_contrast)
                    )
                    Slider(
                        value = settings.contrast,
                        onValueChange = { onUpdateSettings(settings.copy(contrast = it)) },
                        valueRange = 0.50f..2.50f
                    )

                    SettingHeaderWithTooltip(
                        title = "${locContext.getString(R.string.setting_gamma)}: ${String.format("%.2f", settings.gamma)}",
                        tooltip = locContext.getString(R.string.tooltip_gamma)
                    )
                    Slider(
                        value = settings.gamma,
                        onValueChange = { onUpdateSettings(settings.copy(gamma = it)) },
                        valueRange = 0.20f..1.50f
                    )

                    SettingSwitchRow(
                        title = locContext.getString(R.string.setting_sharpen),
                        tooltip = locContext.getString(R.string.tooltip_sharpen),
                        checked = settings.sharpen,
                        onCheckedChange = { onUpdateSettings(settings.copy(sharpen = it)) }
                    )

                    SettingSwitchRow(
                        title = locContext.getString(R.string.setting_dither),
                        tooltip = locContext.getString(R.string.tooltip_dither),
                        checked = settings.dither,
                        onCheckedChange = { onUpdateSettings(settings.copy(dither = it)) }
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 4, 8).forEach { bpc ->
                            FilterChip(
                                selected = settings.bitsPerPixel == bpc,
                                onClick = { onUpdateSettings(settings.copy(bitsPerPixel = bpc)) },
                                label = { Text("-bpc $bpc (${1 shl bpc} shades)") }
                            )
                        }
                    }

                    SettingSwitchRow(
                        title = locContext.getString(R.string.setting_scan_cleanup),
                        tooltip = locContext.getString(R.string.tooltip_scan_cleanup),
                        checked = settings.autoStraightenDeskew && settings.despeckle,
                        onCheckedChange = {
                            onUpdateSettings(
                                settings.copy(
                                    autoStraightenDeskew = it,
                                    despeckle = it,
                                    whitenBackground = it,
                                    removeShadows = it
                                )
                            )
                        }
                    )

                    SettingHeaderWithTooltip(
                        title = locContext.getString(R.string.setting_dark_eink),
                        tooltip = locContext.getString(R.string.tooltip_dark_eink)
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.negateColors == 0,
                            onClick = { onUpdateSettings(settings.copy(negateColors = 0)) },
                            label = { Text("Paper Light (-neg-)") }
                        )
                        FilterChip(
                            selected = settings.negateColors == 2,
                            onClick = { onUpdateSettings(settings.copy(negateColors = 2)) },
                            label = { Text("Smart Dark (-neg+)") }
                        )
                        FilterChip(
                            selected = einkTheme,
                            onClick = { onToggleEinkTheme(!einkTheme) },
                            label = { Text("E-Ink UI Mode") }
                        )
                        FilterChip(
                            selected = darkTheme,
                            onClick = { onToggleDarkTheme(!darkTheme) },
                            label = { Text("Dark App Theme") }
                        )
                    }
                }
            }
        }

        // GROUP 5: Neural Layout Model, Protected Blocks & Smart Footnotes
        if (matchesSearch(
                locContext.getString(R.string.settings_group_layout),
                "table", "footnote", "equation", "neural"
            )
        ) {
            item {
                SettingsGroupCard(title = locContext.getString(R.string.settings_group_layout)) {
                    SettingSwitchRow(
                        title = locContext.getString(R.string.setting_layout_engine),
                        tooltip = locContext.getString(R.string.tooltip_layout_engine),
                        checked = settings.useNeuralLayoutDetector,
                        onCheckedChange = { onUpdateSettings(settings.copy(useNeuralLayoutDetector = it)) }
                    )

                    SettingHeaderWithTooltip(
                        title = locContext.getString(R.string.setting_protected_blocks),
                        tooltip = locContext.getString(R.string.tooltip_protected_blocks)
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProtectPolicy.entries.forEach { pol ->
                            FilterChip(
                                selected = settings.tableProtection == pol,
                                onClick = { onUpdateSettings(settings.copy(tableProtection = pol)) },
                                label = { Text(pol.label) }
                            )
                        }
                    }

                    SettingHeaderWithTooltip(
                        title = locContext.getString(R.string.setting_smart_footnotes),
                        tooltip = locContext.getString(R.string.tooltip_smart_footnotes)
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FootnotePlacement.entries.forEach { fn ->
                            FilterChip(
                                selected = settings.footnotePlacement == fn,
                                onClick = { onUpdateSettings(settings.copy(footnotePlacement = fn)) },
                                label = { Text(fn.label) }
                            )
                        }
                    }
                }
            }
        }

        // GROUP 6: Output Format, Target File Size & Raw CLI Flags
        if (matchesSearch(
                locContext.getString(R.string.settings_group_output),
                "epub", "html", "pdf", "markdown", "cli", "size"
            )
        ) {
            item {
                SettingsGroupCard(title = locContext.getString(R.string.settings_group_output)) {
                    SettingHeaderWithTooltip(
                        title = locContext.getString(R.string.setting_output_format),
                        tooltip = locContext.getString(R.string.tooltip_output_format)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutputFormat.entries.forEach { fmt ->
                            FilterChip(
                                selected = settings.outputFormat == fmt,
                                onClick = { onUpdateSettings(settings.copy(outputFormat = fmt)) },
                                label = { Text(fmt.label) }
                            )
                        }
                    }

                    SettingHeaderWithTooltip(
                        title = "${locContext.getString(R.string.setting_target_size)} — Estimated: ~${
                            LocaleFormatUtils.formatFileSize(estimatedSizeKb * 1024L, localeTag)
                        }",
                        tooltip = locContext.getString(R.string.tooltip_target_size)
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "Unlimited", 250 to "250 KB", 500 to "500 KB", 1024 to "1 MB", 2048 to "2 MB").forEach { (kb, label) ->
                            FilterChip(
                                selected = settings.targetSizeKb == kb,
                                onClick = { onUpdateSettings(settings.copy(targetSizeKb = kb)) },
                                label = { Text(label) }
                            )
                        }
                    }

                    SettingHeaderWithTooltip(
                        title = locContext.getString(R.string.setting_raw_cli),
                        tooltip = locContext.getString(R.string.tooltip_raw_cli)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = rawCliInput,
                            onValueChange = { rawCliInput = it },
                            placeholder = { Text("-mode 2col -dpi 300 -col 2 -ocrlang eng+ara") },
                            leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("raw_cli_input")
                        )
                        Button(
                            onClick = { onApplyRawCli(rawCliInput) },
                            modifier = Modifier.testTag("btn_apply_raw_cli")
                        ) {
                            Text("Apply")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun SettingHeaderWithTooltip(
    title: String,
    tooltip: String
) {
    var showTooltip by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showTooltip = !showTooltip }) {
                Icon(
                    Icons.Default.HelpOutline,
                    contentDescription = "Explain setting",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
        AnimatedVisibility(visible = showTooltip) {
            Card(
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Text(
                    text = tooltip,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    tooltip: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    var showTooltip by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f, fill = false)
                )
                IconButton(onClick = { showTooltip = !showTooltip }) {
                    Icon(
                        Icons.Default.HelpOutline,
                        contentDescription = "Explain setting",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
        AnimatedVisibility(visible = showTooltip) {
            Card(
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Text(
                    text = tooltip,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}
