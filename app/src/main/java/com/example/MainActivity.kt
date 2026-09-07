package com.example

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
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
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                val vm: DlmsViewModel = viewModel()
                Box(Modifier.fillMaxSize()) {
                    DlmsApp(viewModel = vm)
                    Button(
                        onClick = { requestYouTubeDspCapture() },
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 54.dp, end = 12.dp),
                        content = { Text("DSP YouTube") }
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
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name", modifier = modifier)
}
