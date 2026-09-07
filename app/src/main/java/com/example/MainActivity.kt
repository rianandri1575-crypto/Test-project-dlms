package com.example

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.YouTubeDspCaptureService
import com.example.ui.DlmsApp
import com.example.ui.DlmsViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private var dspActive by mutableStateOf(false)
    private val dspHandler = Handler(Looper.getMainLooper())

    private val webViewVolumeKeeper = object : Runnable {
        override fun run() {
            if (!dspActive) return
            setYouTubeWebViewVolume(1)
            dspHandler.postDelayed(this, 250)
        }
    }

    private val dspStoppedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == YouTubeDspCaptureService.ACTION_STOPPED) {
                setDspActive(false)
            }
        }
    }

    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            runCatching {
                val serviceIntent = Intent(this, YouTubeDspCaptureService::class.java).apply {
                    putExtra(YouTubeDspCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                    putExtra(YouTubeDspCaptureService.EXTRA_RESULT_DATA, result.data)
                }
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(serviceIntent) else startService(serviceIntent)
                setDspActive(true)
            }.onFailure {
                setDspActive(false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filter = IntentFilter(YouTubeDspCaptureService.ACTION_STOPPED)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(dspStoppedReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION") registerReceiver(dspStoppedReceiver, filter)
        }

        setContent {
            MyApplicationTheme {
                val vm: DlmsViewModel = viewModel()
                Box(Modifier.fillMaxSize()) {
                    DlmsApp(viewModel = vm)
                    Button(
                        onClick = {
                            if (dspActive) stopYouTubeDsp() else requestYouTubeDspCapture()
                        },
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 54.dp, end = 12.dp),
                        content = { Text(if (dspActive) "Stop DSP" else "DSP YouTube") }
                    )
                }
            }
        }
    }

    private fun requestYouTubeDspCapture() {
        if (Build.VERSION.SDK_INT < 29) return
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        captureLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun stopYouTubeDsp() {
        val intent = Intent(this, YouTubeDspCaptureService::class.java).apply {
            action = YouTubeDspCaptureService.ACTION_STOP
        }
        startService(intent)
        setDspActive(false)
    }

    private fun setDspActive(active: Boolean) {
        dspActive = active
        dspHandler.removeCallbacks(webViewVolumeKeeper)
        if (active) {
            setYouTubeWebViewVolume(1)
            dspHandler.post(webViewVolumeKeeper)
        } else {
            setYouTubeWebViewVolume(100)
        }
    }

    /**
     * AudioPlaybackCapture copies the existing playback stream rather than replacing it.
     * Keep the original WebView at 1% while the processed return path is active, then restore it.
     */
    private fun setYouTubeWebViewVolume(percent: Int) {
        fun visit(view: View) {
            if (view is WebView) {
                val safe = percent.coerceIn(0, 100)
                view.evaluateJavascript(
                    "try { if (window.player && player.setVolume) player.setVolume($safe); } catch(e) {}",
                    null
                )
            }
            if (view is android.view.ViewGroup) {
                for (i in 0 until view.childCount) visit(view.getChildAt(i))
            }
        }
        visit(window.decorView)
    }

    override fun onDestroy() {
        dspHandler.removeCallbacks(webViewVolumeKeeper)
        setYouTubeWebViewVolume(100)
        runCatching { unregisterReceiver(dspStoppedReceiver) }
        super.onDestroy()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name", modifier = modifier)
}
