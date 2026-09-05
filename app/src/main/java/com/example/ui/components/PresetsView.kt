package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.AudioPresetEntity
import com.example.data.model.formatFrequency
import com.example.util.AppUpdateManager
import kotlinx.coroutines.launch
import com.example.ui.theme.AudioAmber
import com.example.ui.theme.AudioCyan
import com.example.ui.theme.AudioCyanDark
import com.example.ui.theme.AudioGreen
import com.example.ui.theme.AudioRed
import com.example.ui.theme.RackBorder
import com.example.ui.theme.RackCard
import com.example.ui.theme.RackCardHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PresetsView(
    presets: List<AudioPresetEntity>,
    currentPresetName: String,
    onLoadPreset: (AudioPresetEntity) -> Unit,
    onSavePreset: (name: String, description: String) -> Unit,
    onDeletePreset: (AudioPresetEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }
    var newPresetDesc by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateState by AppUpdateManager.updateState.collectAsState()
    var showUpdateDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, RackBorder, RoundedCornerShape(24.dp))
            .testTag("presets_view"),
        color = RackCard
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Presets",
                        tint = AudioCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "AUDIO PRESETS & MEMORY",
                        style = MaterialTheme.typography.labelSmall,
                        color = AudioCyan,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                Button(
                    onClick = {
                        newPresetName = "Setup Venue ${SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date())}"
                        newPresetDesc = "Custom 31-band EQ, crossover, and speaker delay tuning"
                        showSaveDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AudioCyan),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("save_preset_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Save",
                        tint = Color(0xFF381E72),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SIMPAN PRESET",
                        color = Color(0xFF381E72),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // FITUR UPDATE APLIKASI TERBARU (IN-APP UPDATER)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1C24),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4A4458)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_update_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (updateState.isUpdateAvailable) AudioGreen else Color(0xFF381E72)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (updateState.isUpdateAvailable) Icons.Default.Download else Icons.Default.SystemUpdate,
                                contentDescription = "App Update",
                                tint = if (updateState.isUpdateAvailable) Color.Black else AudioCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "VERSI APLIKASI",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    letterSpacing = 0.5.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF2B2930)
                                ) {
                                    Text(
                                        text = "v${AppUpdateManager.getCurrentVersionName()}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AudioCyan,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (updateState.isDownloading) {
                                    "Mengunduh APK baru..."
                                } else if (updateState.isChecking) {
                                    "Memeriksa pembaruan..."
                                } else if (updateState.isUpdateAvailable) {
                                    "Versi Baru v${updateState.latestVersionName} Tersedia!"
                                } else {
                                    "Aplikasi Sudah Versi Terbaru"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (updateState.isUpdateAvailable) AudioGreen else TextPrimary
                            )
                        }
                    }

                    // Tombol Aksi Update / Cek Update
                    if (updateState.isChecking || updateState.isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AudioCyan,
                            strokeWidth = 2.dp
                        )
                    } else if (updateState.isUpdateAvailable) {
                        Button(
                            onClick = {
                                AppUpdateManager.startDownloadAndInstall(context, updateState.downloadUrl)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AudioGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "UPDATE",
                                color = Color.Black,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    AppUpdateManager.checkForUpdates(context)
                                    showUpdateDialog = true
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AudioCyan.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AudioCyan)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Check",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "CEK UPDATE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Presets List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presets, key = { it.id }) { preset ->
                    val isActive = currentPresetName == preset.name
                    PresetCardItem(
                        preset = preset,
                        isActive = isActive,
                        onLoad = { onLoadPreset(preset) },
                        onDelete = { onDeletePreset(preset) }
                    )
                }
            }
        }
    }

    // Save Preset Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text(
                    text = "Simpan Preset Audio",
                    style = MaterialTheme.typography.titleMedium,
                    color = AudioCyan,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Konfigurasi saat ini (31-band EQ L/R, crossover HPF/LPF, gain, dan delay) akan disimpan ke database lokal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = newPresetName,
                        onValueChange = { newPresetName = it },
                        label = { Text("Nama Preset") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AudioCyan,
                            unfocusedBorderColor = RackBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextSecondary,
                            focusedContainerColor = Color(0xFF1C1B1F),
                            unfocusedContainerColor = Color(0xFF1C1B1F)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPresetDesc,
                        onValueChange = { newPresetDesc = it },
                        label = { Text("Keterangan / Catatan Venue") },
                        maxLines = 2,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AudioCyan,
                            unfocusedBorderColor = RackBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextSecondary,
                            focusedContainerColor = Color(0xFF1C1B1F),
                            unfocusedContainerColor = Color(0xFF1C1B1F)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPresetName.isNotBlank()) {
                            onSavePreset(newPresetName, newPresetDesc)
                            showSaveDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AudioCyan),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Simpan", color = Color(0xFF381E72), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF2B2930),
            shape = RoundedCornerShape(24.dp)
        )
    }

    // App Update Status / Download Dialog
    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            icon = {
                Icon(
                    imageVector = if (updateState.isUpdateAvailable) Icons.Default.Download else Icons.Default.SystemUpdate,
                    contentDescription = "Update Status",
                    tint = if (updateState.isUpdateAvailable) AudioGreen else AudioCyan,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (updateState.isChecking) {
                        "Memeriksa Pembaruan..."
                    } else if (updateState.isUpdateAvailable) {
                        "Pembaruan Tersedia (v${updateState.latestVersionName})"
                    } else {
                        "Aplikasi Sudah Terbaru (v${AppUpdateManager.getCurrentVersionName()})"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (updateState.isChecking) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = AudioCyan,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Menghubungi server rilis...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    } else if (updateState.isUpdateAvailable) {
                        Text(
                            text = "Versi baru siap dipasang langsung tanpa menghapus data atau settingan yang sudah ada.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        if (updateState.releaseNotes.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1C1B1F),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Text(
                                    text = updateState.releaseNotes,
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Anda sedang menjalankan versi v${AppUpdateManager.getCurrentVersionName()} (Build ${AppUpdateManager.getCurrentVersionCode()}). Tidak ada pembaruan baru saat ini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    if (updateState.errorMessage != null) {
                        Text(
                            text = updateState.errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = AudioAmber
                        )
                    }
                }
            },
            confirmButton = {
                if (updateState.isUpdateAvailable) {
                    Button(
                        onClick = {
                            AppUpdateManager.startDownloadAndInstall(context, updateState.downloadUrl)
                            showUpdateDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AudioGreen),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("UPDATE SEKARANG", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                    }
                } else {
                    Button(
                        onClick = { showUpdateDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = AudioCyan),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("TUTUP", color = Color(0xFF381E72), fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (updateState.isUpdateAvailable) {
                    TextButton(onClick = { showUpdateDialog = false }) {
                        Text("Nanti Saja", color = TextSecondary)
                    }
                }
            },
            containerColor = Color(0xFF2B2930),
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun PresetCardItem(
    preset: AudioPresetEntity,
    isActive: Boolean,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isActive) Color(0xFF381E72) else Color(0xFF1C1B1F))
            .border(
                1.dp,
                if (isActive) AudioCyan else RackBorder.copy(alpha = 0.5f),
                RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (preset.isFactory) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AudioAmber.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "FACTORY",
                                style = MaterialTheme.typography.labelSmall,
                                color = AudioAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AudioGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "USER",
                                style = MaterialTheme.typography.labelSmall,
                                color = AudioGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                        }
                    }

                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) AudioCyan else TextPrimary,
                        maxLines = 1
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(AudioCyan)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF381E72),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "LOADED",
                                    color = Color(0xFF381E72),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = onLoad,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AudioCyan,
                                contentColor = Color(0xFF381E72)
                            ),
                            shape = CircleShape,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("LOAD", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (!preset.isFactory) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Preset",
                                tint = AudioRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (preset.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = preset.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Preset Specs Summary Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SpecChip(
                    label = if (preset.hpfEnabled) "HPF: ${formatFrequency(preset.hpfFrequency)}Hz" else "HPF: Off",
                    color = if (preset.hpfEnabled) AudioGreen else TextMuted
                )
                SpecChip(
                    label = if (preset.lpfEnabled) "LPF: ${formatFrequency(preset.lpfFrequency)}Hz" else "LPF: Off",
                    color = if (preset.lpfEnabled) AudioCyan else TextMuted
                )
                SpecChip(
                    label = "Delay: ${"%.1f".format(preset.delayL)}ms",
                    color = AudioAmber
                )
            }
        }
    }
}

@Composable
fun SpecChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF2B2930))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontSize = 9.sp
        )
    }
}
