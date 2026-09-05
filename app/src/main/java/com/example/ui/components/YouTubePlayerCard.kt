package com.example.ui.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
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
import java.net.URLEncoder

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

data class SoundcheckTrack(
    val id: String,
    val title: String,
    val category: String
)

val CURATED_SOUNDCHECK_TRACKS = listOf(
    SoundcheckTrack("kffacxfA7G4", "DJ Soundcheck Bass Glerr Horeg", "Sub/Bass"),
    SoundcheckTrack("b65L0h2g2aQ", "Subwoofer 20Hz - 120Hz Sweep Test", "Low-End"),
    SoundcheckTrack("pWJeH_4571g", "Pink Noise Calibrated Flat Ref", "Reference"),
    SoundcheckTrack("9bZkp7q19f0", "Audio Clarity Vocal & High Test", "Clarity"),
    SoundcheckTrack("fJ9rUzIMcZQ", "Queen - Bohemian Rhapsody (Dynamic)", "Dynamics"),
    SoundcheckTrack("kJQP7kiw5Fk", "Luis Fonsi - Despacito (Latin Percussion)", "Percussion")
)

val QUICK_SEARCH_CHIPS = listOf(
    "Soundcheck Bass Horeg",
    "Test Subwoofer 20Hz-80Hz",
    "DJ Remix Bass Glerr",
    "Dangdut Koplo Horeg",
    "Acoustic Lossless Audio",
    "Drum & Percussion Dynamic"
)

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
    var showVideoInMiniMode by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isSearchActiveInWebView by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Helper to execute search or load video directly
    val executeSearchOrLoad: (String) -> Unit = { query ->
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            focusManager.clearFocus()
            val extractedId = extractYouTubeId(trimmed)
            if (extractedId != null) {
                isSearchActiveInWebView = false
                onTrackSelected(extractedId, "YouTube ($extractedId)")
                val html = buildYouTubeHtml(extractedId)
                webViewRef?.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
            } else {
                // In-App Search: Load YouTube mobile search results directly into the player
                isSearchActiveInWebView = true
                val encoded = URLEncoder.encode(trimmed, "UTF-8")
                val searchUrl = "https://m.youtube.com/results?search_query=$encoded"
                webViewRef?.loadUrl(searchUrl)
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, RackBorder, RoundedCornerShape(20.dp))
            .testTag("youtube_player_card"),
        color = RackCard
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // --- HEADER BAR ---
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
                        text = if (isMiniMode) "AUDIO ENGINE (BACKGROUND ACTIVE)" else "YOUTUBE AUDIO ENGINE",
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
                        // Toggle video window while on EQ or Crossover tab
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (showVideoInMiniMode) Color(0xFF4F378B) else Color(0xFF381E72),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AudioCyan.copy(alpha = 0.6f)),
                            modifier = Modifier.clickable { showVideoInMiniMode = !showVideoInMiniMode }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (showVideoInMiniMode) Icons.Default.VideocamOff else Icons.Default.Videocam,
                                    contentDescription = "Video Mode",
                                    tint = AudioCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (showVideoInMiniMode) "TUTUP VIDEO" else "LIHAT VIDEO",
                                    color = AudioCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        // Play/Pause direct button in full card header
                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    webViewRef?.evaluateJavascript(
                                        "window.userExplicitlyPaused = true; if (player && player.pauseVideo) player.pauseVideo();",
                                        null
                                    )
                                    onTogglePlay(false)
                                } else {
                                    webViewRef?.evaluateJavascript(
                                        "window.userExplicitlyPaused = false; if (player && player.playVideo) player.playVideo();",
                                        null
                                    )
                                    onTogglePlay(true)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause Audio" else "Play Audio",
                                tint = if (isPlaying) AudioGreen else AudioCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                isSearchActiveInWebView = false
                                val html = buildYouTubeHtml(currentVideoId)
                                webViewRef?.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                            },
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

            // --- NOW PLAYING TITLE BAR ---
            if (!isMiniMode) {
                Text(
                    text = if (isSearchActiveInWebView) "Mode Pencarian YouTube Aktif - Pilih video di bawah untuk memutar" else currentTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSearchActiveInWebView) AudioAmber else AudioCyan,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // --- PERSISTENT WEBVIEW CONTAINER ---
            // CRITICAL FIX: The WebView is permanently mounted and NEVER resized to 0dp or 1dp.
            // In mini-mode without video expanded, it is measured at 16:9 full size off-screen with 0 layout height.
            // This prevents YouTube iframe player and Chromium from detecting viewport collapse or throttling background playback!
            val isFullDisplay = (!isMiniMode && isPlayerVisible) || (isMiniMode && showVideoInMiniMode)

            Box(
                modifier = if (isFullDisplay) {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black)
                        .border(1.dp, RackBorder.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                } else {
                    // Background active state: layout measurement keeps full 16:9 dimensions internally
                    // but consumes 0 height in the Compose layout flow.
                    Modifier
                        .fillMaxWidth()
                        .layout { measurable, constraints ->
                            val targetHeight = (constraints.maxWidth * 9 / 16).coerceAtLeast(360)
                            val placeable = measurable.measure(
                                Constraints.fixed(constraints.maxWidth, targetHeight)
                            )
                            layout(placeable.width, 0) {
                                placeable.placeRelative(0, -9999)
                            }
                        }
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
                            settings.databaseEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    val detectedId = extractYouTubeId(url)
                                    if (detectedId != null) {
                                        isSearchActiveInWebView = false
                                        onTrackSelected(detectedId, "YouTube Audio ($detectedId)")
                                        val html = buildYouTubeHtml(detectedId)
                                        view?.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                                        return true
                                    }
                                    return false
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    // Inject anti-pause overrides into any loaded page (including mobile search results)
                                    view?.evaluateJavascript(
                                        """
                                        Object.defineProperty(document, 'hidden', { get: function() { return false; }, configurable: true });
                                        Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; }, configurable: true });
                                        document.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);
                                        """.trimIndent(),
                                        null
                                    )
                                }
                            }

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
                        if (!isSearchActiveInWebView && webView.tag != currentVideoId) {
                            webView.tag = currentVideoId
                            val html = buildYouTubeHtml(currentVideoId)
                            webView.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // --- MINI MODE COMPACT AUDIO STATUS BAR ---
            if (isMiniMode) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2B2930))
                        .border(1.dp, RackBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
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
                                .size(26.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isPlaying) AudioGreen else AudioRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Playing",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (isPlaying) "AUDIO DLMS AKTIF (TIDAK BERHENTI)" else "AUDIO DIJEDA",
                                color = if (isPlaying) AudioGreen else TextMuted,
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

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    webViewRef?.evaluateJavascript(
                                        "window.userExplicitlyPaused = true; if (player && player.pauseVideo) player.pauseVideo();",
                                        null
                                    )
                                    onTogglePlay(false)
                                } else {
                                    webViewRef?.evaluateJavascript(
                                        "window.userExplicitlyPaused = false; if (player && player.playVideo) player.playVideo();",
                                        null
                                    )
                                    onTogglePlay(true)
                                }
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = if (isPlaying) AudioGreen else AudioCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF381E72),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AudioCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable(onClick = onExpandVideo)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "PLAYER",
                                    color = AudioCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Go to Player",
                                    tint = AudioCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // --- FULL PLAYER CONTROLS & SEARCH (Tab 0) ---
                Spacer(modifier = Modifier.height(10.dp))

                // In-App YouTube Search Bar (No copy-paste needed!)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Ketik judul lagu, DJ, atau artis...",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                executeSearchOrLoad(searchQuery)
                            }
                        ),
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
                            executeSearchOrLoad(searchQuery)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AudioCyan),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("youtube_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Track",
                            tint = Color(0xFF381E72),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cari", color = Color(0xFF381E72), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Search Suggestion Chips
                Text(
                    text = "PENCARIAN CEPAT SOUND SYSTEM / HOREG:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QUICK_SEARCH_CHIPS.forEach { chipText ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF2B2930),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RackBorder),
                            modifier = Modifier.clickable {
                                searchQuery = chipText
                                executeSearchOrLoad(chipText)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = AudioCyan,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = chipText,
                                    fontSize = 10.sp,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Curated 1-Tap Soundcheck Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PRESET TRACK SOUNDCHECK RESMI:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isSearchActiveInWebView) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AudioCyan.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AudioCyan),
                            modifier = Modifier.clickable {
                                isSearchActiveInWebView = false
                                val html = buildYouTubeHtml(currentVideoId)
                                webViewRef?.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                            }
                        ) {
                            Text(
                                text = "KEMBALI KE PLAYER",
                                fontSize = 9.sp,
                                color = AudioCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CURATED_SOUNDCHECK_TRACKS.forEach { track ->
                        val isCurrent = (currentVideoId == track.id && !isSearchActiveInWebView)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) AudioCyan.copy(alpha = 0.2f) else Color(0xFF2B2930),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCurrent) AudioCyan else RackBorder
                            ),
                            modifier = Modifier.clickable {
                                isSearchActiveInWebView = false
                                onTrackSelected(track.id, track.title)
                                val html = buildYouTubeHtml(track.id)
                                webViewRef?.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCurrent && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isCurrent) AudioCyan else TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Column {
                                    Text(
                                        text = track.title,
                                        fontSize = 11.sp,
                                        color = if (isCurrent) AudioCyan else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = track.category,
                                        fontSize = 9.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Hardware DSP Signal Generator (Sine/Pink Noise/Sweep)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF2B2930))
                        .border(1.dp, RackBorder.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
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
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
          <style>
            * { box-sizing: border-box; }
            html, body { margin: 0; padding: 0; width: 100%; height: 100%; background: #000; overflow: hidden; }
            #player { width: 100%; height: 100%; position: absolute; top: 0; left: 0; }
          </style>
          <script>
            // 1. Anti-pause overrides: prevent background visibility change and intersection observer from pausing audio
            Object.defineProperty(document, 'hidden', { get: function() { return false; }, configurable: true });
            Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; }, configurable: true });
            document.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);

            window.IntersectionObserver = function(cb) {
              return {
                observe: function(elem) { cb([{ isIntersecting: true, intersectionRatio: 1.0, target: elem }]); },
                unobserve: function() {},
                disconnect: function() {}
              };
            };
          </script>
          <script src="https://www.youtube.com/iframe_api"></script>
        </head>
        <body>
          <div id="player"></div>
          <script>
            var player;
            window.userExplicitlyPaused = false;

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
                  'origin': 'https://www.youtube.com',
                  'widget_referrer': 'https://www.youtube.com',
                  'rel': 0,
                  'fs': 1,
                  'iv_load_policy': 3
                },
                events: {
                  'onReady': function(e) {
                    try {
                      if (!window.userExplicitlyPaused) {
                        e.target.playVideo();
                      }
                      if (window.DlmsBridge) window.DlmsBridge.onPlayerState(1);
                    } catch(err){}
                  },
                  'onStateChange': function(e) {
                    if (window.DlmsBridge) window.DlmsBridge.onPlayerState(e.data);
                    // If YouTube attempts to pause involuntarily (e.data === 2) when user has not requested pause:
                    if (e.data === 2 && !window.userExplicitlyPaused) {
                      setTimeout(function() {
                        if (!window.userExplicitlyPaused && player && player.playVideo) {
                          player.playVideo();
                        }
                      }, 60);
                    }
                  },
                  'onError': function(e) {
                    console.log("YT Player Error:", e.data);
                    if (window.DlmsBridge) window.DlmsBridge.onPlayerState(2);
                  }
                }
              });
            }

            setInterval(function() {
              try {
                if (player && player.getPlayerState && player.getPlayerState() === 1) {
                  var t = player.getCurrentTime() || 0;
                  if (window.DlmsBridge) window.DlmsBridge.onTimeTick(t);
                }
              } catch(err){}
            }, 35);
          </script>
        </body>
        </html>
    """.trimIndent()
}

fun extractYouTubeId(urlOrId: String): String? {
    val trimmed = urlOrId.trim()
    if (trimmed.length == 11 && trimmed.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
        return trimmed
    }
    if (trimmed.contains("youtu.be/")) {
        val candidate = trimmed.substringAfter("youtu.be/").substringBefore("?").substringBefore("&").substringBefore("/")
        if (candidate.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) return candidate
    }
    if (trimmed.contains("v=")) {
        val candidate = trimmed.substringAfter("v=").substringBefore("&").substringBefore("?").substringBefore("/")
        if (candidate.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) return candidate
    }
    if (trimmed.contains("embed/")) {
        val candidate = trimmed.substringAfter("embed/").substringBefore("?").substringBefore("&").substringBefore("/")
        if (candidate.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) return candidate
    }
    if (trimmed.contains("shorts/")) {
        val candidate = trimmed.substringAfter("shorts/").substringBefore("?").substringBefore("&").substringBefore("/")
        if (candidate.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) return candidate
    }
    return null
}
