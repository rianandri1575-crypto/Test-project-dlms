package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AudioCyan
import com.example.ui.theme.AudioRed
import com.example.ui.theme.MeterOrange
import com.example.ui.theme.MeterRed
import com.example.ui.theme.MeterYellow
import com.example.ui.theme.RackBorder
import com.example.ui.theme.RackCard
import com.example.ui.theme.TextSecondary

@Composable
fun StereoVuMeter(
    levelDbL: Float,
    levelDbR: Float,
    isMutedL: Boolean,
    isMutedR: Boolean,
    modifier: Modifier = Modifier
) {
    // ViewModel sudah memprioritaskan PCM aktual pasca-DSP (LiveAudioMetrics)
    // dengan ballistik attack/release; composable TIDAK menimpa dengan data
    // mentah agar peak/decay tetap natural dan tidak bergetar.
    val shownL = levelDbL
    val shownR = levelDbR
    Box(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(RackCard)
            .border(1.dp, RackBorder, RoundedCornerShape(20.dp)).padding(horizontal = 14.dp, vertical = 9.dp)
            .testTag("stereo_vu_meter")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("STEREO OUTPUT METERS", color = AudioCyan, fontWeight = FontWeight.Bold, letterSpacing = .8.sp, fontSize = 10.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MeterReadout("L", shownL, isMutedL)
                    MeterReadout("R", shownR, isMutedR)
                }
            }
            VuMeterBar("L", shownL, isMutedL)
            VuMeterBar("R", shownR, isMutedR)
            // Signal meter: level gabungan stereo (data PCM aktual yang sama).
            SignalPresenceMeter(levelDbL = shownL, levelDbR = shownR, isMuted = isMutedL && isMutedR)
            Row(Modifier.fillMaxWidth().padding(start = 21.dp, end = 18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("-60", "-48", "-36", "-24", "-18", "-12", "-6", "0").forEach {
                    Text(it, color = TextSecondary.copy(alpha = .55f), fontFamily = FontFamily.Monospace, fontSize = 7.sp)
                }
            }
        }
    }
}

@Composable
private fun MeterReadout(label: String, levelDb: Float, muted: Boolean) {
    Text(
        text = if (muted) "$label: MUTE" else "$label: ${if (levelDb <= -59f) "-INF" else "%.1f".format(levelDb)} dB",
        color = if (muted) AudioRed else AudioCyan,
        fontFamily = FontFamily.Monospace,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun VuMeterBar(
    channelLabel: String,
    levelDb: Float,
    isMuted: Boolean,
    modifier: Modifier = Modifier
) {
    // Skala dB logaritmik yang benar: -60..0 dB dipetakan non-linear agar
    // gerakan jarum realistis (peka di area -30..0 dB seperti VU analog).
    val fraction = if (isMuted) 0f else dbToMeterFraction(levelDb)
    val segments = 24
    val activeCount = (fraction * segments).toInt().coerceIn(0, segments)

    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(channelLabel, color = if (isMuted) AudioRed else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(14.dp))
        Row(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF151419)), horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
            repeat(segments) { index ->
                val threshold = -60f + (index + 1) * (60f / segments)
                val active = !isMuted && index < activeCount
                val segmentColor = when {
                    threshold > -3f -> MeterRed
                    threshold > -12f -> MeterOrange
                    threshold > -24f -> MeterYellow
                    else -> AudioCyan
                }
                // Segmen aktif digambar solid; non-aktif redup 10% (murah di GPU).
                Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(1.dp)).background(if (active) segmentColor else segmentColor.copy(alpha = .10f)))
            }
        }
        // LED CLIP modern dengan glow (murah: 1 Box + border, tanpa shadow).
        Box(
            Modifier.width(13.dp).height(10.dp).clip(RoundedCornerShape(3.dp))
                .background(if (!isMuted && levelDb >= 0f) MeterRed else MeterRed.copy(alpha = .12f))
                .border(
                    1.dp,
                    if (!isMuted && levelDb >= -1f) Color.White.copy(alpha = .7f) else Color.Transparent,
                    RoundedCornerShape(3.dp)
                )
        )
    }
}

/**
 * Signal meter gabungan stereo dari PCM aktual yang sama dengan VU/spectrum.
 * Menampilkan status NO SIGNAL / WEAK / GOOD / HOT dengan gradasi modern.
 * Ringan: 1 Canvas kecil, tanpa animasi tambahan (nilai sudah di-smoothing).
 */
@Composable
fun SignalPresenceMeter(
    levelDbL: Float,
    levelDbR: Float,
    isMuted: Boolean,
    modifier: Modifier = Modifier
) {
    val peak = maxOf(levelDbL, levelDbR)
    val fraction = if (isMuted) 0f else dbToMeterFraction(peak)
    val status = when {
        isMuted || peak <= -59f -> "NO SIGNAL" to TextSecondary
        peak < -30f -> "WEAK" to MeterYellow
        peak < -3f -> "GOOD SIGNAL" to AudioCyan
        else -> "HOT" to MeterRed
    }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("SIG", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.width(14.dp))
        Box(
            Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF151419)).border(.5.dp, status.second.copy(alpha = .35f), RoundedCornerShape(4.dp))
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width * fraction
                if (w > 1f) {
                    // Gradasi 3 zona (hijau→kuning→merah) digambar sebagai 3
                    // segmen solid — terlihat modern tanpa shader mahal.
                    val greenEnd = size.width * .55f
                    val yellowEnd = size.width * .85f
                    drawRoundRect(
                        Color(0xFF2FD08C), Offset.Zero,
                        Size(minOf(w, greenEnd), size.height), CornerRadius(size.height / 2f, size.height / 2f)
                    )
                    if (w > greenEnd) drawRoundRect(
                        Color(0xFFFFC94D), Offset(greenEnd, 0f),
                        Size(minOf(w, yellowEnd) - greenEnd, size.height), CornerRadius(2f, 2f)
                    )
                    if (w > yellowEnd) drawRoundRect(
                        Color(0xFFFF5A5A), Offset(yellowEnd, 0f),
                        Size(w - yellowEnd, size.height), CornerRadius(2f, 2f)
                    )
                    // Ujung terang (hot tip) untuk kesan kaca modern.
                    drawLine(
                        Color.White.copy(alpha = .55f), Offset(1f, 1f), Offset(minOf(w - 1f, size.width - 1f), 1f),
                        strokeWidth = 1.5f, cap = StrokeCap.Round
                    )
                }
            }
        }
        Spacer(Modifier.width(13.dp))
    }
    Row(Modifier.fillMaxWidth().padding(start = 22.dp), horizontalArrangement = Arrangement.Start) {
        Text(
            if (isMuted) "SIG: MUTED" else "SIG: ${status.first} ${if (peak <= -59f) "" else "(${\"%.0f\".format(peak)} dB)"}",
            color = status.second, fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold
        )
    }
}

/** Pemetaan dB logaritmik ke fraksi meter 0..1 (peka di -30..0 dB). */
private fun dbToMeterFraction(db: Float): Float {
    if (db <= -60f) return 0f
    if (db >= 0f) return 1f
    // Kurva: (dB+60)/60 dipangkat 1.6 → resolusi tinggi di dekat 0 dB.
    val linear = (db + 60f) / 60f
    return linear.pow(1.6f).coerceIn(0f, 1f)
}
