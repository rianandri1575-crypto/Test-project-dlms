package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AudioCyan
import com.example.ui.theme.AudioRed
import com.example.ui.theme.MeterGreen
import com.example.ui.theme.MeterOrange
import com.example.ui.theme.MeterRed
import com.example.ui.theme.MeterYellow
import com.example.ui.theme.RackBorder
import com.example.ui.theme.RackCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun StereoVuMeter(
    levelDbL: Float,
    levelDbR: Float,
    isMutedL: Boolean,
    isMutedR: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(RackCard)
            .border(1.dp, RackBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("stereo_vu_meter")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STEREO OUTPUT METERS",
                    style = MaterialTheme.typography.labelSmall,
                    color = AudioCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    fontSize = 10.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isMutedL) "L: MUTE" else "L: ${if (levelDbL <= -55f) "-INF" else "%.1f".format(levelDbL)} dB",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMutedL) AudioRed else AudioCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isMutedR) "R: MUTE" else "R: ${if (levelDbR <= -55f) "-INF" else "%.1f".format(levelDbR)} dB",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMutedR) AudioRed else AudioCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Channel L bar
            VuMeterBar(
                channelLabel = "L",
                levelDb = levelDbL,
                isMuted = isMutedL
            )

            // Channel R bar
            VuMeterBar(
                channelLabel = "R",
                levelDb = levelDbR,
                isMuted = isMutedR
            )
        }
    }
}

@Composable
fun VuMeterBar(
    channelLabel: String,
    levelDb: Float,
    isMuted: Boolean,
    modifier: Modifier = Modifier
) {
    // Segments: -48, -36, -24, -18, -12, -9, -6, -3, 0, +3, +6 dB (11 segments)
    val thresholds = listOf(-48f, -36f, -24f, -18f, -12f, -9f, -6f, -3f, 0f, 3f, 6f)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = channelLabel,
            style = MaterialTheme.typography.labelSmall,
            color = if (isMuted) AudioRed else TextSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.width(14.dp)
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1C1B1F)),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            thresholds.forEach { threshold ->
                val isActive = !isMuted && (levelDb >= threshold)
                val segmentColor = when {
                    threshold >= 3f -> MeterRed
                    threshold >= 0f -> MeterOrange
                    threshold >= -9f -> MeterYellow
                    else -> AudioCyan
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(if (isActive) segmentColor else segmentColor.copy(alpha = 0.12f))
                )
            }
        }

        // Clip LED indicator
        val isClipping = !isMuted && levelDb >= 0f
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (isClipping) MeterRed else MeterRed.copy(alpha = 0.15f))
        )
    }
}
