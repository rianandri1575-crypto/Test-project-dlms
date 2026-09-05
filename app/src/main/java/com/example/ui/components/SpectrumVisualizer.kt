package com.example.ui.components

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CrossoverSettings
import com.example.data.model.CrossoverSlope
import com.example.data.model.ISO_31_FREQUENCIES
import com.example.data.model.formatFrequency
import com.example.ui.theme.AudioAmber
import com.example.ui.theme.AudioCyan
import com.example.ui.theme.AudioGreen
import com.example.ui.theme.AudioRed
import com.example.ui.theme.HighDensityBorder
import com.example.ui.theme.HighDensityCard
import com.example.ui.theme.HighDensityLavender
import com.example.ui.theme.HighDensityTextSecondary
import com.example.ui.theme.MeterRed
import com.example.ui.theme.MeterYellow
import com.example.ui.theme.RackBorder
import com.example.ui.theme.RackCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlin.math.log10

@Composable
fun RealTimeSpectrumVisualizer(
    levels: List<Float>,
    peakLevels: List<Float>,
    eqGains: List<Float>,
    crossover: CrossoverSettings,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(RoundedCornerShape(24.dp))
            .background(HighDensityCard)
            .border(1.dp, HighDensityBorder, RoundedCornerShape(24.dp))
            .padding(12.dp)
            .testTag("real_time_spectrum_visualizer")
    ) {
        Column {
            // Header with High Density tracking and colors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "RTA Spectrum",
                        tint = HighDensityLavender,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "REAL-TIME SPECTRUM ANALYZER",
                        style = MaterialTheme.typography.labelSmall,
                        color = HighDensityLavender,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 10.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Legend
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp, 2.dp)
                                .background(HighDensityLavender)
                        )
                        Text(
                            text = "Curve",
                            style = MaterialTheme.typography.labelSmall,
                            color = HighDensityTextSecondary,
                            fontSize = 9.sp
                        )
                    }

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "Toggle Spectrum Size",
                            tint = HighDensityTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Top Frequency Markers from Design HTML: 20Hz, 1kHz, 20kHz
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "20Hz",
                    style = MaterialTheme.typography.labelSmall,
                    color = HighDensityLavender.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
                Text(
                    text = "1kHz",
                    style = MaterialTheme.typography.labelSmall,
                    color = HighDensityLavender.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
                Text(
                    text = "20kHz",
                    style = MaterialTheme.typography.labelSmall,
                    color = HighDensityLavender.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isExpanded) 190.dp else 125.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1C1B1F))
                    .border(1.dp, HighDensityBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 4.dp)) {
                    val w = size.width
                    val h = size.height

                    // 1. Draw Grid Lines (dB markings)
                    val dbLevels = listOf(
                        0.15f to "-36dB",
                        0.40f to "-18dB",
                        0.65f to "0dB",
                        0.90f to "+6dB"
                    )

                    dbLevels.forEach { (fraction, _) ->
                        val y = h * (1f - fraction)
                        drawLine(
                            color = Color(0x18CAC4D0),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                    }

                    // 2. Draw 31 Frequency Bars with High Density Lavender Glow
                    val numBands = 31
                    val spacing = 2f
                    val totalSpacing = spacing * (numBands - 1)
                    val barWidth = (w - totalSpacing) / numBands

                    val barBrush = Brush.verticalGradient(
                        colors = listOf(
                            HighDensityLavender,
                            HighDensityLavender.copy(alpha = 0.7f),
                            HighDensityLavender.copy(alpha = 0.35f),
                            HighDensityLavender.copy(alpha = 0.15f)
                        ),
                        startY = 0f,
                        endY = h
                    )

                    for (i in 0 until numBands) {
                        val level = levels.getOrElse(i) { 0.05f }.coerceIn(0.02f, 1.0f)
                        val peak = peakLevels.getOrElse(i) { 0.05f }.coerceIn(0.02f, 1.0f)

                        val barHeight = h * level
                        val x = i * (barWidth + spacing)
                        val y = h - barHeight

                        // Bar
                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(2f, 2f)
                        )

                        // Peak hold line
                        val peakY = h - (h * peak)
                        drawLine(
                            color = HighDensityLavender,
                            start = Offset(x, peakY),
                            end = Offset(x + barWidth, peakY),
                            strokeWidth = 2f,
                            cap = StrokeCap.Round
                        )
                    }

                    // 3. Draw Filter Curve Overlay (Combined 31-band EQ + Crossover HPF/LPF transfer function)
                    val curvePath = Path()
                    var firstPoint = true

                    for (i in 0 until numBands) {
                        val freq = ISO_31_FREQUENCIES[i]
                        val eqGain = eqGains.getOrElse(i) { 0f } // -12 to +12 dB

                        var xOverDb = 0f
                        if (crossover.hpfEnabled && crossover.hpfSlope != CrossoverSlope.BYPASS) {
                            if (freq < crossover.hpfFrequency) {
                                val octaves = (log10(crossover.hpfFrequency / freq) / log10(2.0)).toFloat()
                                xOverDb -= octaves * crossover.hpfSlope.rollOffDb
                            }
                        }
                        if (crossover.lpfEnabled && crossover.lpfSlope != CrossoverSlope.BYPASS) {
                            if (freq > crossover.lpfFrequency) {
                                val octaves = (log10(freq / crossover.lpfFrequency) / log10(2.0)).toFloat()
                                xOverDb -= octaves * crossover.lpfSlope.rollOffDb
                            }
                        }

                        val totalGainDb = (eqGain + xOverDb).coerceIn(-36f, 18f)
                        val normalizedY = 1f - ((totalGainDb + 36f) / 54f).coerceIn(0.05f, 0.95f)
                        val pointX = i * (barWidth + spacing) + (barWidth / 2f)
                        val pointY = h * normalizedY

                        if (firstPoint) {
                            curvePath.moveTo(pointX, pointY)
                            firstPoint = false
                        } else {
                            curvePath.lineTo(pointX, pointY)
                        }
                    }

                    drawPath(
                        path = curvePath,
                        color = Color(0xFFFFFFFF),
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Frequency Labels on bottom (Key markers: 20, 63, 250, 1k, 4k, 16k, 20k)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("20", "63", "250", "1k", "4k", "10k", "20k").forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}
