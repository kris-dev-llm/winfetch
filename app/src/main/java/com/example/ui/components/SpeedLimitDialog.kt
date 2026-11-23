package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.SpeedUnit
import com.example.data.prefs.WinFetchSettings
import com.example.engine.FormatUtils
import com.example.ui.theme.LocalWin10Tokens
import com.example.ui.theme.WinMonoFamily

@Composable
fun SpeedLimitDialog(
    settings: WinFetchSettings,
    onDismiss: () -> Unit,
    onSave: (enabled: Boolean, value: Long, unit: SpeedUnit) -> Unit
) {
    val tokens = LocalWin10Tokens.current
    var enabled by remember(settings) { mutableStateOf(settings.speedLimitEnabled) }
    var valueStr by remember(settings) { mutableStateOf(settings.speedLimitValue.toString()) }
    var unit by remember(settings) { mutableStateOf(settings.speedLimitUnit) }

    val parsedValue = valueStr.toLongOrNull()?.coerceAtLeast(1L) ?: 0L
    val bytesPerSec = unit.toBytesPerSecond(parsedValue)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RectangleShape,
            color = tokens.cardBackground,
            border = BorderStroke(1.5.dp, tokens.accent),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(tokens.accent)
                        .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Speed,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Download Speed Limiter",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Win10CheckboxRow(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        label = "Enable Global Speed Cap",
                        subtitle = "Throttles active HTTP, Site, Torrent, and Git transfers",
                        testTag = "speed_limit_enable_checkbox"
                    )

                    OutlinedTextField(
                        value = valueStr,
                        onValueChange = { input ->
                            valueStr = input.filter { it.isDigit() }.take(7)
                        },
                        enabled = enabled,
                        label = { Text("Speed Limit Value") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RectangleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = tokens.accent,
                            unfocusedBorderColor = tokens.border
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = WinMonoFamily),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("speed_limit_value_input")
                    )

                    Text(
                        text = "UNIT (BITS VS BYTES VS BINARY)",
                        style = MaterialTheme.typography.labelMedium,
                        color = tokens.textSecondary
                    )

                    // Row 1: Kb (bit), KB (bytes), KiB
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SpeedUnit.primaryUnits.forEach { u ->
                            UnitOptionTile(
                                unit = u,
                                selected = unit == u,
                                enabled = enabled,
                                onClick = { unit = u },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Row 2: Mb, MB, MiB
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(SpeedUnit.MB_BIT, SpeedUnit.MB_BYTE, SpeedUnit.MIB_BYTE).forEach { u ->
                            UnitOptionTile(
                                unit = u,
                                selected = unit == u,
                                enabled = enabled,
                                onClick = { unit = u },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Quick Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("128", "512", "1024", "4096").forEach { preset ->
                            Surface(
                                shape = RectangleShape,
                                color = tokens.chromeBackground,
                                border = BorderStroke(1.dp, tokens.border),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 40.dp)
                                    .clickable(enabled = enabled) { valueStr = preset }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = preset,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (enabled) tokens.textPrimary else tokens.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(tokens.chromeBackground, RectangleShape)
                            .border(1.dp, tokens.border, RectangleShape)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = if (enabled && parsedValue > 0L) {
                                "Effective Cap: $parsedValue ${unit.shortLabel}/s = ${FormatUtils.formatSpeed(bytesPerSec)} (${unit.description})"
                            } else {
                                "Effective Cap: Unlimited (Full Network Bandwidth)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (enabled) tokens.accent else tokens.textSecondary
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(tokens.chromeBackground)
                        .border(1.dp, tokens.border, RectangleShape)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Win10ActionButton(text = "Cancel", onClick = onDismiss)
                    Spacer(modifier = Modifier.width(8.dp))
                    Win10ActionButton(
                        text = "Apply Limit",
                        primary = true,
                        onClick = {
                            onSave(enabled, parsedValue.coerceAtLeast(1L), unit)
                            onDismiss()
                        },
                        testTag = "apply_speed_limit_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun UnitOptionTile(
    unit: SpeedUnit,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalWin10Tokens.current
    Surface(
        shape = RectangleShape,
        color = if (selected) tokens.accent else tokens.chromeBackground,
        border = BorderStroke(1.dp, if (selected) tokens.accentPressed else tokens.border),
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .testTag("speed_unit_${unit.name.lowercase()}")
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
        ) {
            Text(
                text = "${unit.shortLabel}/s",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = when {
                    !enabled -> tokens.textSecondary.copy(alpha = 0.5f)
                    selected -> Color.White
                    else -> tokens.textPrimary
                }
            )
            Text(
                text = when (unit) {
                    SpeedUnit.KB_BIT, SpeedUnit.MB_BIT -> "bits"
                    SpeedUnit.KB_BYTE, SpeedUnit.MB_BYTE -> "1000 B"
                    SpeedUnit.KIB_BYTE, SpeedUnit.MIB_BYTE -> "1024 B"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) Color.White.copy(alpha = 0.85f) else tokens.textSecondary
            )
        }
    }
}
