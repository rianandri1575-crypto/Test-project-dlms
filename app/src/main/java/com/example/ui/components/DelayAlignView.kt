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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.ChangeCircle
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChannelAudioSettings
import com.example.ui.theme.AudioAmber
import com.example.ui.theme.AudioCyan
import com.example.ui.theme.AudioGreen
import com.example.ui.theme.AudioRed
import com.example.ui.theme.FaderTrack
import com.example.ui.theme.RackBorder
import com.example.ui.theme.RackCard
import com.example.ui.theme.RackCardHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DelayAlignView(
    channelL: ChannelAudioSettings,
    channelR: ChannelAudioSettings,
    onDelayChange: (isLeft: Boolean, Float) -> Unit,
    onPhaseToggle: (isLeft: Boolean) -> Unit,
    onMuteToggle: (isLeft: Boolean) -> Unit,
    onGainChange: (isLeft: Boolean, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, RackBorder, RoundedCornerShape(24.dp))
            .testTag("delay_align_view"),
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
                        imageVector = Icons.Default.AvTimer,
                        contentDescription = "Delay Alignment",
                        tint = AudioCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "SPEAKER DELAY & PHASE ALIGNMENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = AudioCyan,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = "v = 343 m/s (20°C)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 9.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Channel L Card
            ChannelDelayCard(
                channelName = "OUTPUT L (LEFT / SUB)",
                channelSettings = channelL,
                accentColor = AudioCyan,
                onDelayChange = { onDelayChange(true, it) },
                onPhaseToggle = { onPhaseToggle(true) },
                onMuteToggle = { onMuteToggle(true) },
                onGainChange = { onGainChange(true, it) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Channel R Card
            ChannelDelayCard(
                channelName = "OUTPUT R (RIGHT / TOPS)",
                channelSettings = channelR,
                accentColor = AudioCyan,
                onDelayChange = { onDelayChange(false, it) },
                onPhaseToggle = { onPhaseToggle(false) },
                onMuteToggle = { onMuteToggle(false) },
                onGainChange = { onGainChange(false, it) }
            )
        }
    }
}

@Composable
fun ChannelDelayCard(
    channelName: String,
    channelSettings: ChannelAudioSettings,
    accentColor: Color,
    onDelayChange: (Float) -> Unit,
    onPhaseToggle: () -> Unit,
    onMuteToggle: () -> Unit,
    onGainChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1C1B1F))
            .border(1.dp, RackBorder.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(12.dp)
    ) {
        Column {
            // Header Row (Name, Mute, Phase)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = channelName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontSize = 11.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Phase Invert Button (0° vs 180°)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (channelSettings.isPhaseInverted) Color(0xFF381E72) else Color.Transparent)
                            .border(1.dp, if (channelSettings.isPhaseInverted) accentColor else RackBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable(onClick = onPhaseToggle)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChangeCircle,
                                contentDescription = "Phase",
                                tint = if (channelSettings.isPhaseInverted) accentColor else TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (channelSettings.isPhaseInverted) "PHASE: 180°" else "PHASE: 0°",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (channelSettings.isPhaseInverted) accentColor else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }

                    // Mute Button
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (channelSettings.isMuted) Color(0xFFFFB4AB) else RackBorder)
                            .clickable(onClick = onMuteToggle)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (channelSettings.isMuted) "MUTED" else "MUTE",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (channelSettings.isMuted) Color(0xFF690005) else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Delay Numerical Readout & Distance (High Density)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF2B2930))
                    .border(1.dp, RackBorder.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TIME DELAY", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                    Text(
                        text = "${"%.2f".format(channelSettings.delayMs)} ms",
                        style = MaterialTheme.typography.titleMedium,
                        color = accentColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Light,
                        fontSize = 15.sp
                    )
                }

                Box(modifier = Modifier.width(1.dp).height(24.dp).background(RackBorder.copy(alpha = 0.5f)))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DISTANCE (M)", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                    Text(
                        text = "${"%.2f".format(channelSettings.delayDistanceMeters)} m",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Light,
                        fontSize = 15.sp
                    )
                }

                Box(modifier = Modifier.width(1.dp).height(24.dp).background(RackBorder.copy(alpha = 0.5f)))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DISTANCE (FT)", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                    Text(
                        text = "${"%.1f".format(channelSettings.delayDistanceFeet)} ft",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Light,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Delay Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "0ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 9.sp
                )
                Slider(
                    value = channelSettings.delayMs,
                    onValueChange = onDelayChange,
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = RackBorder
                    ),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                Text(
                    text = "100ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 9.sp
                )
            }

            // Fine Nudge Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FINE NUDGE:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(-5.0f, -1.0f, -0.1f, 0.1f, 1.0f, 5.0f).forEach { step ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(RackBorder.copy(alpha = 0.3f))
                                .border(1.dp, RackBorder.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable {
                                    val newDelay = (channelSettings.delayMs + step).coerceIn(0f, 100f)
                                    onDelayChange(newDelay)
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (step > 0) "+$step" else "$step",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Zero reset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF381E72))
                            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { onDelayChange(0f) }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "0.0",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
