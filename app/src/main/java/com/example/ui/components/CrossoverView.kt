package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChannelSelect
import com.example.data.model.CrossoverSettings
import com.example.data.model.CrossoverSlope
import com.example.data.model.formatFrequency
import com.example.ui.theme.AudioAmber
import com.example.ui.theme.AudioCyan
import com.example.ui.theme.AudioGreen
import com.example.ui.theme.AudioRed
import com.example.ui.theme.RackBorder
import com.example.ui.theme.RackCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun CrossoverView(
    crossoverChannel: ChannelSelect,
    crossoverL: CrossoverSettings,
    crossoverR: CrossoverSettings,
    onSelectChannel: (ChannelSelect) -> Unit,
    onHpfEnabledChange: (Boolean) -> Unit,
    onHpfFrequencyChange: (Float) -> Unit,
    onHpfSlopeChange: (CrossoverSlope) -> Unit,
    onLpfEnabledChange: (Boolean) -> Unit,
    onLpfFrequencyChange: (Float) -> Unit,
    onLpfSlopeChange: (CrossoverSlope) -> Unit,
    onCopyLtoR: () -> Unit = {},
    onCopyRtoL: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentCrossover = when (crossoverChannel) {
        ChannelSelect.LEFT -> crossoverL
        ChannelSelect.RIGHT -> crossoverR
        ChannelSelect.LINKED -> crossoverL
    }

    val activeColor = when (crossoverChannel) {
        ChannelSelect.LEFT -> AudioCyan
        ChannelSelect.RIGHT -> AudioAmber
        ChannelSelect.LINKED -> AudioGreen
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, RackBorder, RoundedCornerShape(24.dp))
            .testTag("crossover_view"),
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
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = "Crossover",
                        tint = AudioCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "CROSSOVER FILTERS (OUTPUT L & R)",
                        style = MaterialTheme.typography.labelSmall,
                        color = AudioCyan,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = if (currentCrossover.hpfEnabled || currentCrossover.lpfEnabled) "DSP ACTIVE" else "BYPASS",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (currentCrossover.hpfEnabled || currentCrossover.lpfEnabled) AudioGreen else TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Channel Selector Tabs: [OUTPUT L] [OUTPUT R] [LINKED (L+R)]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1C1B1F))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ChannelSelect.values().forEach { channel ->
                    val isSelected = crossoverChannel == channel
                    val tabColor = when (channel) {
                        ChannelSelect.LEFT -> AudioCyan
                        ChannelSelect.RIGHT -> AudioAmber
                        ChannelSelect.LINKED -> AudioGreen
                    }
                    val label = when (channel) {
                        ChannelSelect.LEFT -> "OUTPUT L"
                        ChannelSelect.RIGHT -> "OUTPUT R"
                        ChannelSelect.LINKED -> "LINKED (L+R)"
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) tabColor.copy(alpha = 0.25f) else Color.Transparent)
                            .border(
                                1.dp,
                                if (isSelected) tabColor else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectChannel(channel) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) tabColor else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dual Output Crossover Visualizer (L = Cyan, R = Amber)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1C1B1F))
                    .border(1.dp, RackBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
                    val w = size.width
                    val h = size.height

                    // Grid lines
                    drawLine(Color(0x22FFFFFF), Offset(0f, h * 0.2f), Offset(w, h * 0.2f), 1f) // 0 dB
                    drawLine(Color(0x15FFFFFF), Offset(0f, h * 0.5f), Offset(w, h * 0.5f), 1f) // -12 dB
                    drawLine(Color(0x15FFFFFF), Offset(0f, h * 0.8f), Offset(w, h * 0.8f), 1f) // -24 dB

                    val minFreq = 20f
                    val maxFreq = 20000f
                    val logMin = log10(minFreq)
                    val logMax = log10(maxFreq)
                    val steps = 80

                    fun drawCrossoverCurve(settings: CrossoverSettings, strokeColor: Color, strokeWidth: Float) {
                        val path = Path()
                        for (step in 0..steps) {
                            val fraction = step / steps.toFloat()
                            val currentLog = logMin + fraction * (logMax - logMin)
                            val freq = 10.0.pow(currentLog.toDouble()).toFloat()

                            var gainDb = 0f
                            if (settings.hpfEnabled && settings.hpfSlope != CrossoverSlope.BYPASS) {
                                if (freq < settings.hpfFrequency) {
                                    val octaves = (log10(settings.hpfFrequency / freq) / log10(2.0)).toFloat()
                                    gainDb -= octaves * settings.hpfSlope.rollOffDb
                                }
                            }
                            if (settings.lpfEnabled && settings.lpfSlope != CrossoverSlope.BYPASS) {
                                if (freq > settings.lpfFrequency) {
                                    val octaves = (log10(freq / settings.lpfFrequency) / log10(2.0)).toFloat()
                                    gainDb -= octaves * settings.lpfSlope.rollOffDb
                                }
                            }

                            val clampedDb = gainDb.coerceIn(-36f, 0f)
                            val y = (h * 0.2f) + ((0f - clampedDb) / 36f) * (h * 0.75f)
                            val x = fraction * w

                            if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }

                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    when (crossoverChannel) {
                        ChannelSelect.LEFT -> {
                            drawCrossoverCurve(crossoverR, AudioAmber.copy(alpha = 0.35f), 2f)
                            drawCrossoverCurve(crossoverL, AudioCyan, 3.5f)
                        }
                        ChannelSelect.RIGHT -> {
                            drawCrossoverCurve(crossoverL, AudioCyan.copy(alpha = 0.35f), 2f)
                            drawCrossoverCurve(crossoverR, AudioAmber, 3.5f)
                        }
                        ChannelSelect.LINKED -> {
                            drawCrossoverCurve(crossoverL, AudioGreen, 3.5f)
                        }
                    }

                    // Markers for active selection
                    if (currentCrossover.hpfEnabled && currentCrossover.hpfSlope != CrossoverSlope.BYPASS) {
                        val hpfFrac = ((log10(currentCrossover.hpfFrequency) - logMin) / (logMax - logMin)).coerceIn(0f, 1f)
                        drawCircle(
                            color = AudioGreen,
                            radius = 5f,
                            center = Offset(hpfFrac * w, h * 0.2f)
                        )
                    }
                    if (currentCrossover.lpfEnabled && currentCrossover.lpfSlope != CrossoverSlope.BYPASS) {
                        val lpfFrac = ((log10(currentCrossover.lpfFrequency) - logMin) / (logMax - logMin)).coerceIn(0f, 1f)
                        drawCircle(
                            color = AudioCyan,
                            radius = 5f,
                            center = Offset(lpfFrac * w, h * 0.2f)
                        )
                    }
                }

                // Legend inside visualizer
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AudioCyan))
                        Text(
                            text = "OUT L (${formatFrequency(crossoverL.hpfFrequency)}-${formatFrequency(crossoverL.lpfFrequency)}Hz)",
                            color = AudioCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AudioAmber))
                        Text(
                            text = "OUT R (${formatFrequency(crossoverR.hpfFrequency)}-${formatFrequency(crossoverR.lpfFrequency)}Hz)",
                            color = AudioAmber,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Copy Channels Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2B2930))
                        .border(1.dp, RackBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onCopyLtoR)
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "COPY OUT-L ➔ OUT-R",
                        color = AudioCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2B2930))
                        .border(1.dp, RackBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onCopyRtoL)
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "COPY OUT-R ➔ OUT-L",
                        color = AudioAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // HIGH-PASS FILTER (HPF) SECTION
            FilterControlCard(
                title = "HIGH-PASS FILTER (HPF / LOW-CUT) - $crossoverChannel",
                subtitle = "Memotong frekuensi sub woofer untuk proteksi driver speaker",
                accentColor = AudioGreen,
                isEnabled = currentCrossover.hpfEnabled,
                frequency = currentCrossover.hpfFrequency,
                selectedSlope = currentCrossover.hpfSlope,
                minFreq = 20f,
                maxFreq = 2000f,
                onEnabledChange = onHpfEnabledChange,
                onFrequencyChange = onHpfFrequencyChange,
                onSlopeChange = onHpfSlopeChange,
                quickPresets = listOf(
                    "30Hz Sub" to 30f,
                    "40Hz Box" to 40f,
                    "80Hz Sat" to 80f,
                    "120Hz Mid" to 120f
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // LOW-PASS FILTER (LPF) SECTION
            FilterControlCard(
                title = "LOW-PASS FILTER (LPF / HIGH-CUT) - $crossoverChannel",
                subtitle = "Memotong frekuensi atas untuk subwoofer atau speaker mid-bass",
                accentColor = activeColor,
                isEnabled = currentCrossover.lpfEnabled,
                frequency = currentCrossover.lpfFrequency,
                selectedSlope = currentCrossover.lpfSlope,
                minFreq = 60f,
                maxFreq = 20000f,
                onEnabledChange = onLpfEnabledChange,
                onFrequencyChange = onLpfFrequencyChange,
                onSlopeChange = onLpfSlopeChange,
                quickPresets = listOf(
                    "80Hz Sub" to 80f,
                    "120Hz Sub" to 120f,
                    "150Hz Kick" to 150f,
                    "16kHz Top" to 16000f
                )
            )
        }
    }
}

@Composable
fun FilterControlCard(
    title: String,
    subtitle: String,
    accentColor: Color,
    isEnabled: Boolean,
    frequency: Float,
    selectedSlope: CrossoverSlope,
    minFreq: Float,
    maxFreq: Float,
    onEnabledChange: (Boolean) -> Unit,
    onFrequencyChange: (Float) -> Unit,
    onSlopeChange: (CrossoverSlope) -> Unit,
    quickPresets: List<Pair<String, Float>>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1C1B1F))
            .border(1.dp, if (isEnabled) accentColor.copy(alpha = 0.5f) else RackBorder.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(12.dp)
    ) {
        Column {
            // Title & Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) accentColor else TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = accentColor,
                        checkedTrackColor = Color(0xFF381E72),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = RackBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cutoff Frequency Slider with Numeric Readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "FREQ:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )

                val logMin = log10(minFreq)
                val logMax = log10(maxFreq)
                val currentLog = log10(frequency.coerceIn(minFreq, maxFreq))
                val fraction = (currentLog - logMin) / (logMax - logMin)

                Slider(
                    value = fraction,
                    onValueChange = { frac ->
                        val newLog = logMin + frac * (logMax - logMin)
                        val newFreq = 10.0.pow(newLog.toDouble()).toFloat()
                        onFrequencyChange(newFreq)
                    },
                    enabled = isEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = RackBorder
                    ),
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = formatFrequency(frequency) + " Hz",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isEnabled) accentColor else TextMuted,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.width(68.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Filter Slope Type Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "SLOPE:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )

                CrossoverSlope.values().forEach { slope ->
                    val isSlopeSelected = selectedSlope == slope
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSlopeSelected && isEnabled) Color(0xFF381E72) else Color.Transparent)
                            .border(1.dp, if (isSlopeSelected && isEnabled) accentColor else RackBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable(enabled = isEnabled) { onSlopeChange(slope) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = slope.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSlopeSelected && isEnabled) accentColor else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Preset Frequencies
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "QUICK:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 9.sp
                )
                quickPresets.forEach { (label, presetFreq) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RackBorder.copy(alpha = 0.3f))
                            .border(1.dp, RackBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable(enabled = isEnabled) { onFrequencyChange(presetFreq) }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isEnabled) TextPrimary else TextMuted,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}
