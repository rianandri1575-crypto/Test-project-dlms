package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AudioCyan
import com.example.ui.theme.AudioGreen
import com.example.ui.theme.HighDensityCard
import com.example.ui.theme.HighDensityLavender
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

/**
 * SATU tombol DSP untuk seluruh aplikasi (pengganti DSP ganda/dummy).
 *
 * ON  = semua audio yang berbunyi lewat SATU [com.example.audio.DspEngineHolder.engine].
 * OFF = bypass murni (metering/VU/spectrum tetap hidup dari PCM aktual).
 * Tombol bereaksi saat musik sedang diputar — tidak perlu stop manual.
 * Visual modern (gradient + glow) dengan cost render minimal (tanpa blur/shadow
 * mahal) agar ringan di Android rendah.
 */
@Composable
fun DspMasterSwitch(
    isDspEnabled: Boolean,
    isAudioPlaying: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = if (isDspEnabled) AudioGreen else TextMuted
    val trackBrush = if (isDspEnabled) {
        Brush.horizontalGradient(listOf(Color(0xFF0B3B3A), Color(0xFF123A5C)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF232227), Color(0xFF1B1A1E)))
    }
    Box(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(trackBrush)
            .border(1.dp, if (isDspEnabled) AudioCyan.copy(alpha = .55f) else Color(0xFF3A3840), RoundedCornerShape(20.dp))
            .clickable { onToggle(!isDspEnabled) }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("dsp_master_switch")
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(34.dp).clip(CircleShape)
                        .background(
                            if (isDspEnabled) Brush.radialGradient(listOf(AudioCyan, Color(0xFF0E5A5E)))
                            else Brush.radialGradient(listOf(Color(0xFF3A3840), Color(0xFF26252B)))
                        )
                        .border(1.dp, if (isDspEnabled) Color.White.copy(alpha = .5f) else Color.Transparent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.GraphicEq, null, tint = if (isDspEnabled) Color.White else TextMuted, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(
                        "DSP ENGINE",
                        color = if (isDspEnabled) AudioCyan else TextMuted,
                        fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp
                    )
                    Text(
                        if (isDspEnabled) {
                            if (isAudioPlaying) "● PROCESSING LIVE AUDIO" else "● AKTIF — SIAP MEMPROSES"
                        } else "○ BYPASS — AUDIO MENTAH",
                        color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Box(
                Modifier.clip(RoundedCornerShape(12.dp))
                    .background(if (isDspEnabled) AudioCyan else Color(0xFF2B2930))
                    .border(1.dp, if (isDspEnabled) Color.White.copy(alpha = .4f) else HighDensityLavender.copy(alpha = .25f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    if (isDspEnabled) "ON" else "OFF",
                    color = if (isDspEnabled) Color(0xFF10231F) else TextPrimary,
                    fontWeight = FontWeight.Bold, fontSize = 12.sp
                )
            }
        }
    }
}
