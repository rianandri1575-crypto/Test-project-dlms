package com.example

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
            val intent = Intent(this, YouTubeDspCaptureService::class.java).apply {
                putExtra(YouTubeDspCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(YouTubeDspCaptureService.EXTRA_RESULT_DATA, result.data)
            }
            startForegroundService(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                val dlmsViewModel: DlmsViewModel = viewModel()
                DlmsApp(viewModel = dlmsViewModel)
            }
        }
        // Android requires explicit user consent before playback capture can begin.
        requestYouTubeDspCapture()
    }

    private fun requestYouTubeDspCapture() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        captureLauncher.launch(manager.createScreenCaptureIntent())
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
