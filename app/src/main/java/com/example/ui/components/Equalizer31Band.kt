package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChannelAudioSettings
import com.example.data.model.ChannelSelect
import com.example.data.model.ISO_31_FREQUENCIES
import com.example.data.model.formatFrequency
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
fun Equalizer31BandView(
    activeChannel: ChannelSelect,
    channelL: ChannelAudioSettings,
    channelR: ChannelAudioSettings,
    onSelectChannel: (ChannelSelect) -> Unit,
    onBandGainChange: (Int, Float) -> Unit,
    onResetFlat: () -> Unit,
    onChannelGainChange: (Boolean, Float) -> Unit,
    onToggleMute: (Boolean) -> Unit,
    onApplyCurve: (List<Float>) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentChannelSettings = when (activeChannel) {
        ChannelSelect.LEFT -> channelL
        ChannelSelect.RIGHT -> channelR
        ChannelSelect.LINKED -> channelL
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, RackBorder, RoundedCornerShape(24.dp))
            .testTag("equalizer_31_band_view"),
        color = RackCard
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // Channel Selector & Master Strip (High Density Pill)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Channel Buttons: L / LINK / R
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1C1B1F))
                        .border(1.dp, RackBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ChannelSelectButton(
                        label = "OUTPUT L",
                        isSelected = activeChannel == ChannelSelect.LEFT,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectChannel(ChannelSelect.LEFT) }
                    )
                    ChannelSelectButton(
                        label = "LINK L/R",
                        isSelected = activeChannel == ChannelSelect.LINKED,
                        icon = if (activeChannel == ChannelSelect.LINKED) Icons.Default.Link else Icons.Default.LinkOff,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectChannel(ChannelSelect.LINKED) }
                    )
                    ChannelSelectButton(
                        label = "OUTPUT R",
                        isSelected = activeChannel == ChannelSelect.RIGHT,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectChannel(ChannelSelect.RIGHT) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Channel Output Header Bar (Gain fader, Mute toggle, readout)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF1C1B1F))
                    .border(1.dp, RackBorder.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Mute button: High Density pill
                val isCurrentMuted = when (activeChannel) {
                    ChannelSelect.LEFT -> channelL.isMuted
                    ChannelSelect.RIGHT -> channelR.isMuted
                    ChannelSelect.LINKED -> channelL.isMuted && channelR.isMuted
                }

                Button(
                    onClick = {
                        val isLeft = activeChannel == ChannelSelect.LEFT
                        onToggleMute(isLeft)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCurrentMuted) Color(0xFFFFB4AB) else RackBorder,
                        contentColor = if (isCurrentMuted) Color(0xFF690005) else TextPrimary
                    ),
                    shape = CircleShape,
                    modifier = Modifier.testTag("channel_mute_button")
                ) {
                    Icon(
                        imageVector = if (isCurrentMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                        contentDescription = "Mute",
                        tint = if (isCurrentMuted) Color(0xFF690005) else TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCurrentMuted) "MUTED" else "MUTE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                // Channel Output Gain Slider
                val currentGain = currentChannelSettings.gainDb
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
                ) {
                    Text(
                        text = "GAIN",
                        style = MaterialTheme.typography.labelSmall,
                        color = AudioCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Slider(
                        value = currentGain,
                        onValueChange = { newGain ->
                            val isLeft = activeChannel == ChannelSelect.LEFT
                            onChannelGainChange(isLeft, newGain)
                        },
                        valueRange = -30f..12f,
                        colors = SliderDefaults.colors(
                            thumbColor = AudioCyan,
                            activeTrackColor = AudioCyan,
                            inactiveTrackColor = RackBorder
                        ),
                        modifier = Modifier.weight(1f).testTag("channel_gain_slider")
                    )
                    Text(
                        text = "${if (currentGain >= 0) "+" else ""}${"%.1f".format(currentGain)} dB",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Light,
                        fontSize = 13.sp,
                        modifier = Modifier.width(58.dp),
                        textAlign = TextAlign.End
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Curves Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "CURVE:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                QuickCurveChip(label = "Bass Boost") {
                    // Boost 31.5Hz to 80Hz
                    val curve = MutableList(31) { 0f }
                    curve[1] = 4.0f; curve[2] = 5.5f; curve[3] = 6.0f; curve[4] = 4.5f; curve[5] = 2.5f
                    onApplyCurve(curve)
                }

                QuickCurveChip(label = "Vocal Clarity") {
                    val curve = MutableList(31) { 0f }
                    curve[0] = -4f; curve[1] = -3f; curve[2] = -2f
                    curve[15] = 1.5f; curve[16] = 2.5f; curve[17] = 3.0f; curve[18] = 2.0f; curve[19] = 1.5f
                    onApplyCurve(curve)
                }

                QuickCurveChip(label = "Loudness") {
                    val curve = MutableList(31) { 0f }
                    curve[1] = 3.5f; curve[2] = 4.0f; curve[3] = 3.5f; curve[4] = 2.0f
                    curve[24] = 2.0f; curve[25] = 3.0f; curve[26] = 3.5f; curve[27] = 3.0f
                    onApplyCurve(curve)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 31 BAND VERTICAL FADERS RACK
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "31-BAND GRAPHIC EQUALIZER",
                    style = MaterialTheme.typography.labelSmall,
                    color = AudioCyan,
                    letterSpacing = 0.8.sp,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "RANGE: ±12 dB",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF1C1B1F))
                    .border(1.dp, RackBorder.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    .padding(vertical = 10.dp, horizontal = 4.dp)
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(ISO_31_FREQUENCIES) { index, freq ->
                        val gain = currentChannelSettings.eqGains.getOrElse(index) { 0f }
                        VerticalBandFader(
                            frequency = freq,
                            gainDb = gain,
                            onGainChange = { onBandGainChange(index, it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelSelectButton(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) RackBorder else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) AudioCyan else TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) AudioCyan else TextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun QuickCurveChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1C1B1F))
            .border(1.dp, RackBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AudioCyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun VerticalBandFader(
    frequency: Float,
    gainDb: Float,
    onGainChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(42.dp)
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Gain dB readout tag (tap to zero)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (gainDb != 0f) AudioCyan.copy(alpha = 0.25f) else RackBorder.copy(alpha = 0.5f)
                )
                .clickable { onGainChange(0f) }
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = "${if (gainDb > 0) "+" else ""}${"%.1f".format(gainDb)}",
                style = MaterialTheme.typography.labelSmall,
                color = if (gainDb != 0f) AudioCyan else TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Vertical Slider container
        Box(
            modifier = Modifier
                .height(130.dp)
                .width(36.dp),
            contentAlignment = Alignment.Center
        ) {
            // Center 0 dB guide line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(RackBorder.copy(alpha = 0.8f))
            )

            // Rotated slider for vertical behavior
            Slider(
                value = gainDb,
                onValueChange = onGainChange,
                valueRange = -12f..12f,
                colors = SliderDefaults.colors(
                    thumbColor = if (gainDb == 0f) TextSecondary else AudioCyan,
                    activeTrackColor = AudioCyan,
                    inactiveTrackColor = RackBorder
                ),
                modifier = Modifier
                    .size(120.dp, 32.dp)
                    .rotate(-90f)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Frequency Label
        Text(
            text = formatFrequency(frequency),
            style = MaterialTheme.typography.labelSmall,
            color = if (frequency in listOf(100f, 1000f, 10000f)) AudioCyan else TextSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = if (frequency in listOf(100f, 1000f, 10000f)) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}
