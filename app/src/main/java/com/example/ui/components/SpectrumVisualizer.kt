package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CrossoverSettings
import com.example.data.model.CrossoverSlope
import com.example.data.model.ISO_31_FREQUENCIES
import com.example.ui.theme.HighDensityBorder
import com.example.ui.theme.HighDensityCard
import com.example.ui.theme.HighDensityLavender
import com.example.ui.theme.HighDensityTextSecondary
import com.example.ui.theme.RackBorder
import com.example.ui.theme.TextMuted
import kotlin.math.log10

/** RTA presentation. Input levels are normalized against -72..0 dBFS. */
@Composable
fun RealTimeSpectrumVisualizer(
    levels: List<Float>,
    peakLevels: List<Float>,
    eqGains: List<Float>,
    crossover: CrossoverSettings,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(HighDensityCard)
            .border(1.dp, HighDensityBorder, RoundedCornerShape(24.dp))
            .padding(12.dp)
            .testTag("real_time_spectrum_visualizer")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.GraphicEq, "RTA Spectrum", tint = HighDensityLavender, modifier = Modifier.size(16.dp))
                    Text("REAL-TIME SPECTRUM ANALYZER", color = HighDensityLavender, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("dBFS", color = HighDensityTextSecondary, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                    IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(24.dp)) {
                        Icon(if (expanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, "Toggle Spectrum Size", tint = HighDensityTextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(5.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("20 Hz", color = HighDensityLavender.copy(alpha = .7f), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                Text("1 kHz", color = HighDensityLavender.copy(alpha = .7f), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                Text("20 kHz", color = HighDensityLavender.copy(alpha = .7f), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            }
            Spacer(Modifier.height(4.dp))

            Box(
                Modifier.fillMaxWidth().height(if (expanded) 200.dp else 135.dp)
                    .clip(RoundedCornerShape(16.dp)).background(Color(0xFF151419))
                    .border(1.dp, HighDensityBorder.copy(alpha = .5f), RoundedCornerShape(16.dp))
            ) {
                Canvas(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
                    val w = size.width
                    val h = size.height
                    val dbGrid = listOf(0f, -12f, -24f, -36f, -48f, -60f, -72f)
                    dbGrid.forEach { db ->
                        val y = h * (1f - ((db + 72f) / 72f))
                        drawLine(Color.White.copy(alpha = if (db == 0f) .18f else .08f), Offset(0f, y), Offset(w, y), 1f)
                    }

                    val count = 31
                    val gap = 2.2f
                    val barWidth = ((w - gap * (count - 1)) / count).coerceAtLeast(1f)
                    for (i in 0 until count) {
                        val value = levels.getOrElse(i) { 0f }.coerceIn(0f, 1f)
                        val peak = peakLevels.getOrElse(i) { value }.coerceIn(value, 1f)
                        val x = i * (barWidth + gap)
                        val barH = (h * value).coerceIn(1f, h)
                        drawRoundRect(
                            color = HighDensityLavender.copy(alpha = .82f),
                            topLeft = Offset(x, h - barH),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                        val peakY = h - h * peak
                        drawLine(HighDensityLavender, Offset(x, peakY), Offset(x + barWidth, peakY), 1.5f, cap = StrokeCap.Round)
                    }

                    // EQ/crossover transfer curve is shown around the true 0 dB line.
                    val curve = Path()
                    for (i in 0 until count) {
                        val f = ISO_31_FREQUENCIES[i]
                        var gain = eqGains.getOrElse(i) { 0f }
                        if (crossover.hpfEnabled && crossover.hpfSlope != CrossoverSlope.BYPASS && f < crossover.hpfFrequency) {
                            gain -= (log10(crossover.hpfFrequency / f) / log10(2.0)).toFloat() * crossover.hpfSlope.rollOffDb
                        }
                        if (crossover.lpfEnabled && crossover.lpfSlope != CrossoverSlope.BYPASS && f > crossover.lpfFrequency) {
                            gain -= (log10(f / crossover.lpfFrequency) / log10(2.0)).toFloat() * crossover.lpfSlope.rollOffDb
                        }
                        val db = gain.coerceIn(-24f, 24f)
                        val x = i * (barWidth + gap) + barWidth / 2f
                        val y = h * (1f - ((db + 24f) / 48f)).coerceIn(.06f, .94f)
                        if (i == 0) curve.moveTo(x, y) else curve.lineTo(x, y)
                    }
                    drawPath(curve, Color.White.copy(alpha = .82f), style = Stroke(1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("20", "63", "250", "1k", "4k", "10k", "20k").forEach {
                    Text(it, color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                }
            }
        }
    }
}
