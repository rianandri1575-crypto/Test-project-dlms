package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.LiveAudioMetrics
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
    val liveActive by LiveAudioMetrics.active.collectAsState()
    val liveL by LiveAudioMetrics.leftDb.collectAsState()
    val liveR by LiveAudioMetrics.rightDb.collectAsState()
    val shownL = if (liveActive) liveL else levelDbL
    val shownR = if (liveActive) liveR else levelDbR

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
    val segments = 24
    val stepDb = 2.5f
    val activeCount = if (isMuted) 0 else (((levelDb.coerceIn(-60f, 0f) + 60f) / stepDb).coerceIn(0f, segments.toFloat())).toInt()

    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(channelLabel, color = if (isMuted) AudioRed else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(14.dp))
        Row(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF151419)), horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
            repeat(segments) { index ->
                val threshold = -60f + (index + 1) * stepDb
                val active = !isMuted && index < activeCount
                val segmentColor = when {
                    threshold > -3f -> MeterRed
                    threshold > -12f -> MeterOrange
                    threshold > -24f -> MeterYellow
                    else -> AudioCyan
                }
                Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(1.dp)).background(if (active) segmentColor else segmentColor.copy(alpha = .10f)))
            }
        }
        Box(Modifier.width(13.dp).height(10.dp).clip(RoundedCornerShape(3.dp)).background(if (!isMuted && levelDb >= 0f) MeterRed else MeterRed.copy(alpha = .12f)))
    }
}
