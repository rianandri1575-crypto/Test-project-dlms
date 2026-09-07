package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.consume
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
import com.example.ui.theme.AudioCyan
import com.example.ui.theme.RackBorder
import com.example.ui.theme.RackCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private const val EQ_MIN_DB = -12f
private const val EQ_MAX_DB = 12f

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
    val current = when (activeChannel) {
        ChannelSelect.LEFT -> channelL
        ChannelSelect.RIGHT -> channelR
        ChannelSelect.LINKED -> channelL
    }

    Surface(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .border(1.dp, RackBorder, RoundedCornerShape(24.dp))
            .testTag("equalizer_31_band_view"),
        color = RackCard
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1C1B1F))
                    .border(1.dp, RackBorder.copy(alpha = .5f), RoundedCornerShape(16.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ChannelSelectButton("OUTPUT L", activeChannel == ChannelSelect.LEFT, Modifier.weight(1f)) { onSelectChannel(ChannelSelect.LEFT) }
                ChannelSelectButton("LINK L/R", activeChannel == ChannelSelect.LINKED, Modifier.weight(1f), if (activeChannel == ChannelSelect.LINKED) Icons.Default.Link else Icons.Default.LinkOff) { onSelectChannel(ChannelSelect.LINKED) }
                ChannelSelectButton("OUTPUT R", activeChannel == ChannelSelect.RIGHT, Modifier.weight(1f)) { onSelectChannel(ChannelSelect.RIGHT) }
            }

            Spacer(Modifier.height(10.dp))
            val muted = when (activeChannel) {
                ChannelSelect.LEFT -> channelL.isMuted
                ChannelSelect.RIGHT -> channelR.isMuted
                ChannelSelect.LINKED -> channelL.isMuted && channelR.isMuted
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF1C1B1F))
                    .border(1.dp, RackBorder.copy(alpha = .5f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onToggleMute(activeChannel == ChannelSelect.LEFT) },
                    colors = ButtonDefaults.buttonColors(containerColor = RackBorder),
                    shape = CircleShape,
                    modifier = Modifier.testTag("channel_mute_button")
                ) {
                    Icon(if (muted) Icons.Default.VolumeMute else Icons.Default.VolumeUp, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp)); Text(if (muted) "MUTED" else "MUTE", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                val gain = current.gainDb
                Row(Modifier.weight(1f).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("GAIN", color = AudioCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Slider(
                        value = gain,
                        onValueChange = { onChannelGainChange(activeChannel == ChannelSelect.LEFT, it) },
                        valueRange = -30f..12f,
                        colors = SliderDefaults.colors(thumbColor = AudioCyan, activeTrackColor = AudioCyan, inactiveTrackColor = RackBorder),
                        modifier = Modifier.weight(1f).testTag("channel_gain_slider")
                    )
                    Text("${if (gain >= 0) "+" else ""}${"%.1f".format(gain)} dB", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 13.sp, modifier = Modifier.width(58.dp), textAlign = TextAlign.End)
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("CURVE:", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                QuickCurveChip("Bass Boost") { val c = MutableList(31) { 0f }; c[1]=4f; c[2]=5.5f; c[3]=6f; c[4]=4.5f; c[5]=2.5f; onApplyCurve(c) }
                QuickCurveChip("Vocal Clarity") { val c = MutableList(31) { 0f }; c[0]=-4f; c[1]=-3f; c[2]=-2f; c[15]=1.5f; c[16]=2.5f; c[17]=3f; c[18]=2f; c[19]=1.5f; onApplyCurve(c) }
                QuickCurveChip("Loudness") { val c = MutableList(31) { 0f }; c[1]=3.5f; c[2]=4f; c[3]=3.5f; c[4]=2f; c[24]=2f; c[25]=3f; c[26]=3.5f; c[27]=3f; onApplyCurve(c) }
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("31-BAND GRAPHIC EQUALIZER", color = AudioCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
                Text("RANGE: ±12 dB • FULL TRAVEL", color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            }
            Spacer(Modifier.height(8.dp))

            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF1C1B1F))
                    .border(1.dp, RackBorder.copy(alpha = .5f), RoundedCornerShape(18.dp))
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.width(24.dp).height(158.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Text("+12", color = TextSecondary, fontSize = 8.sp)
                        Text("0", color = TextSecondary, fontSize = 8.sp)
                        Text("-12", color = TextSecondary, fontSize = 8.sp)
                    }
                    LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        itemsIndexed(ISO_31_FREQUENCIES) { index, freq ->
                            VerticalBandFader(freq, current.eqGains.getOrElse(index) { 0f }, { onBandGainChange(index, it) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelSelectButton(label: String, isSelected: Boolean, modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit) {
    Box(modifier.fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(if (isSelected) RackBorder else Color.Transparent).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (icon != null) Icon(icon, null, tint = if (isSelected) AudioCyan else TextSecondary, modifier = Modifier.size(14.dp))
            Text(label, color = if (isSelected) AudioCyan else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

@Composable
fun QuickCurveChip(label: String, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF1C1B1F)).border(1.dp, RackBorder.copy(alpha=.6f), RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(horizontal=10.dp, vertical=5.dp)) {
        Text(label, color = AudioCyan, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun VerticalBandFader(frequency: Float, gainDb: Float, onGainChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val trackHeight = 142f
    val clamped = gainDb.coerceIn(EQ_MIN_DB, EQ_MAX_DB)
    val thumbY = ((EQ_MAX_DB - clamped) / (EQ_MAX_DB - EQ_MIN_DB) * trackHeight).coerceIn(0f, trackHeight)

    fun yToGain(y: Float): Float {
        val normalized = 1f - (y.coerceIn(0f, trackHeight) / trackHeight)
        return (EQ_MIN_DB + normalized * (EQ_MAX_DB - EQ_MIN_DB)).coerceIn(EQ_MIN_DB, EQ_MAX_DB)
    }

    Column(modifier.width(42.dp).padding(horizontal=2.dp), horizontalAlignment=Alignment.CenterHorizontally) {
        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(if (clamped != 0f) AudioCyan.copy(alpha=.25f) else RackBorder.copy(alpha=.5f)).clickable { onGainChange(0f) }.padding(horizontal=4.dp, vertical=2.dp)) {
            Text("${if (clamped > 0) "+" else ""}${"%.1f".format(clamped)}", color = if (clamped != 0f) AudioCyan else TextSecondary, fontFamily=FontFamily.Monospace, fontSize=9.sp, fontWeight=FontWeight.Bold)
        }
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier.height(150.dp).width(36.dp).pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { p -> onGainChange(yToGain(p.y)) },
                    onDrag = { change, _ -> change.consume(); onGainChange(yToGain(change.position.y)) }
                )
            },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val x = size.width / 2f
                val top = 4f
                val bottom = size.height - 4f
                val y = (thumbY + 4f).coerceIn(top, bottom)
                val center = size.height / 2f
                drawLine(RackBorder.copy(alpha=.9f), Offset(x, top), Offset(x, bottom), strokeWidth=4f)
                drawLine(AudioCyan, Offset(x, center), Offset(x, y), strokeWidth=5f)
                drawLine(RackBorder.copy(alpha=.9f), Offset(x-12f, center), Offset(x+12f, center), strokeWidth=1.5f)
                drawRoundRect(if (clamped == 0f) TextSecondary else AudioCyan, Offset(x-7f, y-15f), Size(14f,30f), CornerRadius(5f,5f))
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(formatFrequency(frequency), color = if (frequency in listOf(100f,1000f,10000f)) AudioCyan else TextSecondary, fontFamily=FontFamily.Monospace, fontSize=9.sp, fontWeight=if (frequency in listOf(100f,1000f,10000f)) FontWeight.Bold else FontWeight.Normal, textAlign=TextAlign.Center)
    }
}
