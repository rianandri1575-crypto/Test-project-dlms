package com.example.ui.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.SignalType
import com.example.ui.theme.AudioAmber
import com.example.ui.theme.AudioCyan
import com.example.ui.theme.AudioCyanDark
import com.example.ui.theme.AudioGreen
import com.example.ui.theme.AudioRed
import com.example.ui.theme.RackBorder
import com.example.ui.theme.RackCard
import com.example.ui.theme.RackCardHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class YouTubeTrackItem(
    val title: String,
    val category: String,
    val videoId: String
)

val CURATED_TEST_TRACKS = listOf(
    YouTubeTrackItem("Sine Sweep 20Hz-20kHz", "Frequency Test", "qNf9nzvnd1k"),
    YouTubeTrackItem("Subwoofer Bass Test 40Hz", "Sub Bass Check", "P8c_v-XqK6k"),
    YouTubeTrackItem("Pink Noise Reference", "RTA Calibration", "WJ9Go1PnAVA"),
    YouTubeTrackItem("Lofi Hip Hop Chill Beats", "Music Stream", "jfKfPfyJRdk"),
    YouTubeTrackItem("Drum & Bass Soundcheck", "Live PA Test", "kJQP7kiw5Fk")
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayerCard(
    currentVideoId: String,
    currentTitle: String,
    isPlaying: Boolean,
    isSignalPlaying: Boolean,
    signalType: SignalType,
    onTrackSelected: (String, String) -> Unit,
    onTogglePlay: (Boolean) -> Unit,
    onToggleSignal: (SignalType) -> Unit,
    onStopSignal: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isPlayerVisible by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, RackBorder, RoundedCornerShape(24.dp))
            .testTag("youtube_player_card"),
        color = RackCard
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) AudioGreen else AudioRed)
                    )
                    Text(
                        text = "YOUTUBE AUDIO SOURCE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AudioCyan,
                        letterSpacing = 0.8.sp,
                        fontSize = 10.sp
                    )
                    Text(
                        text = if (isPlaying) "STREAMING ACTIVE" else "READY",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPlaying) AudioGreen else TextMuted,
                        fontSize = 9.sp
                    )
                }

                Row {
                    IconButton(
                        onClick = { webViewRef?.reload() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload Player",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { isPlayerVisible = !isPlayerVisible },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlayerVisible) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Player Visibility",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = currentTitle,
                style = MaterialTheme.typography.bodySmall,
                color = AudioCyan,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Embedded Player WebView
            AnimatedVisibility(visible = isPlayerVisible) {
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .border(1.dp, RackBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    ) {
                        AndroidView(
                            factory = { context ->
                                WebView(context).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.mediaPlaybackRequiresUserGesture = false
                                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                                    webViewClient = WebViewClient()
                                    webChromeClient = WebChromeClient()

                                    val html = """
                                        <!DOCTYPE html>
                                        <html>
                                        <head>
                                          <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                          <style>
                                            body { margin: 0; padding: 0; background: #000; overflow: hidden; }
                                            iframe { width: 100vw; height: 100vh; border: none; }
                                          </style>
                                        </head>
                                        <body>
                                          <iframe 
                                            id="ytplayer"
                                            src="https://www.youtube.com/embed/$currentVideoId?autoplay=1&enablejsapi=1&playsinline=1&controls=1" 
                                            allow="autoplay; encrypted-media" 
                                            allowfullscreen>
                                          </iframe>
                                        </body>
                                        </html>
                                    """.trimIndent()

                                    loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                                    webViewRef = this
                                }
                            },
                            update = { webView ->
                                if (webView.tag != currentVideoId) {
                                    webView.tag = currentVideoId
                                    val html = """
                                        <!DOCTYPE html>
                                        <html>
                                        <head>
                                          <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                          <style>
                                            body { margin: 0; padding: 0; background: #000; overflow: hidden; }
                                            iframe { width: 100vw; height: 100vh; border: none; }
                                          </style>
                                        </head>
                                        <body>
                                          <iframe 
                                            id="ytplayer"
                                            src="https://www.youtube.com/embed/$currentVideoId?autoplay=1&enablejsapi=1&playsinline=1&controls=1" 
                                            allow="autoplay; encrypted-media" 
                                            allowfullscreen>
                                          </iframe>
                                        </body>
                                        </html>
                                    """.trimIndent()
                                    webView.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Search / URL input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Paste YouTube URL atau ID video...", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AudioCyan,
                        unfocusedBorderColor = RackBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextSecondary,
                        focusedContainerColor = Color(0xFF1C1B1F),
                        unfocusedContainerColor = Color(0xFF1C1B1F)
                    ),
                    textStyle = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("youtube_search_input")
                )

                Button(
                    onClick = {
                        val parsedId = extractYouTubeId(searchQuery)
                        if (parsedId.isNotBlank()) {
                            onTrackSelected(parsedId, "Custom Track ($parsedId)")
                            searchQuery = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AudioCyan),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("youtube_load_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Play Track",
                        tint = Color(0xFF381E72),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play", color = Color(0xFF381E72), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Curated Test Tracks
            Text(
                text = "TRACK UJI AUDIO & KALIBRASI SOUND SYSTEM:",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(CURATED_TEST_TRACKS) { track ->
                    val isSelected = currentVideoId == track.videoId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Color(0xFF381E72) else Color(0xFF1C1B1F))
                            .border(
                                1.dp,
                                if (isSelected) AudioCyan else RackBorder.copy(alpha = 0.5f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                onTrackSelected(track.videoId, track.title)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) AudioCyan else TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                            Text(
                                text = track.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hardware DSP Signal Generator (Sine/Pink Noise/Sweep)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF2B2930))
                    .border(1.dp, RackBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = "Generator",
                        tint = if (isSignalPlaying) AudioAmber else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Column {
                        Text(
                            text = "DSP TEST TONE GENERATOR",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 10.sp
                        )
                        Text(
                            text = if (isSignalPlaying) "Active: ${signalType.label}" else "AudioTrack Synthesis Standby",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSignalPlaying) AudioAmber else TextMuted,
                            fontSize = 9.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SignalType.values().take(3).forEach { type ->
                        val isCurrent = isSignalPlaying && signalType == type
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCurrent) AudioCyan else RackBorder.copy(alpha = 0.4f))
                                .border(1.dp, if (isCurrent) AudioCyan else RackBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .clickable { onToggleSignal(type) }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = when (type) {
                                    SignalType.SINE_1KHZ -> "1kHz"
                                    SignalType.SINE_40HZ -> "40Hz"
                                    SignalType.SINE_100HZ -> "100Hz"
                                    SignalType.PINK_NOISE -> "Pink"
                                    SignalType.SWEEP_20_20K -> "Sweep"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCurrent) Color(0xFF381E72) else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    if (isSignalPlaying) {
                        IconButton(
                            onClick = onStopSignal,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop Generator",
                                tint = AudioRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun extractYouTubeId(urlOrId: String): String {
    val trimmed = urlOrId.trim()
    if (trimmed.length == 11 && !trimmed.contains("/") && !trimmed.contains("?")) {
        return trimmed
    }
    // Handle youtu.be/XXXX
    if (trimmed.contains("youtu.be/")) {
        return trimmed.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
    }
    // Handle youtube.com/watch?v=XXXX
    if (trimmed.contains("v=")) {
        return trimmed.substringAfter("v=").substringBefore("&").substringBefore("?")
    }
    // Handle youtube.com/embed/XXXX
    if (trimmed.contains("embed/")) {
        return trimmed.substringAfter("embed/").substringBefore("?").substringBefore("&")
    }
    return trimmed
}
