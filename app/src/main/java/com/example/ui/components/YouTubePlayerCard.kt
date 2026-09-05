package com.example.ui.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JavascriptInterface
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
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.SignalType
import com.example.ui.theme.AudioAmber
import com.example.ui.theme.AudioCyan
import com.example.ui.theme.AudioGreen
import com.example.ui.theme.AudioRed
import com.example.ui.theme.RackBorder
import com.example.ui.theme.RackCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class DlmsWebBridge(
    private val onPlaybackState: (Int) -> Unit,
    private val onTick: (Double) -> Unit
) {
    @JavascriptInterface
    fun onPlayerState(state: Int) {
        onPlaybackState(state)
    }

    @JavascriptInterface
    fun onTimeTick(timeSec: Double) {
        onTick(timeSec)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayerCard(
    currentVideoId: String,
    currentTitle: String,
    isPlaying: Boolean,
    isSignalPlaying: Boolean,
    signalType: SignalType,
    isMiniMode: Boolean = false,
    onTrackSelected: (String, String) -> Unit,
    onPlaybackStateChanged: (Int) -> Unit = {},
    onTimeTick: (Double) -> Unit = {},
    onTogglePlay: (Boolean) -> Unit,
    onToggleSignal: (SignalType) -> Unit,
    onStopSignal: () -> Unit,
    onExpandVideo: () -> Unit = {},
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) AudioGreen else AudioRed)
                    )
                    Text(
                        text = "YOUTUBE AUDIO ENGINE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AudioCyan,
                        letterSpacing = 0.8.sp,
                        fontSize = 10.sp
                    )
                    Text(
                        text = if (isPlaying) "PLAYING" else "PAUSED",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPlaying) AudioGreen else TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isMiniMode) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF381E72),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AudioCyan.copy(alpha = 0.6f)),
                            modifier = Modifier.clickable(onClick = onExpandVideo)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Video Mode",
                                    tint = AudioCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "LIHAT VIDEO",
                                    color = AudioCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
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
            }

            Text(
                text = currentTitle,
                style = MaterialTheme.typography.bodySmall,
                color = AudioCyan,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Embedded Player WebView Container:
            // CRITICAL: Stays in the composition tree at all times so background audio never stops!
            Box(
                modifier = if (!isMiniMode && isPlayerVisible) {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(1.dp, RackBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                } else {
                    // Minimized size when on EQ/Crossover tabs or collapsed:
                    // Still attached to window surface so WebKit media player audio thread is uninterrupted!
                    Modifier
                        .size(1.dp)
                        .alpha(0.001f)
                }
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

                            addJavascriptInterface(
                                DlmsWebBridge(
                                    onPlaybackState = { state ->
                                        onPlaybackStateChanged(state)
                                    },
                                    onTick = { time ->
                                        onTimeTick(time)
                                    }
                                ),
                                "DlmsBridge"
                            )

                            val html = buildYouTubeHtml(currentVideoId)
                            loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                            webViewRef = this
                        }
                    },
                    update = { webView ->
                        if (webView.tag != currentVideoId) {
                            webView.tag = currentVideoId
                            val html = buildYouTubeHtml(currentVideoId)
                            webView.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // If in Mini Mode, show a compact audio status bar
            if (isMiniMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2B2930))
                        .clickable(onClick = onExpandVideo)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(AudioRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Playing",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "AUDIO BACKGROUND BERJALAN",
                                color = AudioGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentTitle,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        text = "TAB PLAYER ➔",
                        color = AudioCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(10.dp))

                // Search / URL input (Play any YouTube song)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Paste YouTube URL atau ID lagu...", fontSize = 12.sp) },
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
                                onTrackSelected(parsedId, "Audio Track ($parsedId)")
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
                        Text("Putar", color = Color(0xFF381E72), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                                text = if (isSignalPlaying) "Active: ${signalType.label}" else "AudioTrack Standby",
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
}

private fun buildYouTubeHtml(videoId: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <style>
            body { margin: 0; padding: 0; background: #000; overflow: hidden; }
            #player { width: 100vw; height: 100vh; }
          </style>
          <script src="https://www.youtube.com/iframe_api"></script>
        </head>
        <body>
          <div id="player"></div>
          <script>
            var player;
            function onYouTubeIframeAPIReady() {
              player = new YT.Player('player', {
                height: '100%',
                width: '100%',
                videoId: '$videoId',
                playerVars: {
                  'autoplay': 1,
                  'playsinline': 1,
                  'controls': 1,
                  'enablejsapi': 1,
                  'rel': 0,
                  'fs': 1
                },
                events: {
                  'onReady': function(e) {
                    try {
                      e.target.playVideo();
                      if (window.DlmsBridge) window.DlmsBridge.onPlayerState(1);
                    } catch(err){}
                  },
                  'onStateChange': function(e) {
                    if (window.DlmsBridge) window.DlmsBridge.onPlayerState(e.data);
                  }
                }
              });
            }

            setInterval(function() {
              if (player && player.getPlayerState && player.getPlayerState() === 1) {
                var t = player.getCurrentTime() || 0;
                if (window.DlmsBridge) window.DlmsBridge.onTimeTick(t);
              }
            }, 35);
          </script>
        </body>
        </html>
    """.trimIndent()
}

fun extractYouTubeId(urlOrId: String): String {
    val trimmed = urlOrId.trim()
    if (trimmed.length == 11 && !trimmed.contains("/") && !trimmed.contains("?")) {
        return trimmed
    }
    if (trimmed.contains("youtu.be/")) {
        return trimmed.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
    }
    if (trimmed.contains("v=")) {
        return trimmed.substringAfter("v=").substringBefore("&").substringBefore("?")
    }
    if (trimmed.contains("embed/")) {
        return trimmed.substringAfter("embed/").substringBefore("?").substringBefore("&")
    }
    return trimmed
}
