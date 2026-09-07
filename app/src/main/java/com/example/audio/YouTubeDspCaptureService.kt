package com.example.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/** Opt-in playback capture. The UI mutes the original WebView while this processed copy is playing. */
class YouTubeDspCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var recorder: AudioRecord? = null
    private var track: AudioTrack? = null
    private var worker: Thread? = null
    @Volatile private var running = false
    private val dsp = DlmsDspEngine(48_000)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopPipeline()
            sendBroadcast(Intent(ACTION_STOPPED).setPackage(packageName))
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
            return START_NOT_STICKY
        }
        if (Build.VERSION.SDK_INT < 29 || intent == null) return START_NOT_STICKY
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
        val resultData = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        if (resultCode != RESULT_OK || resultData == null) return START_NOT_STICKY

        startForeground(NOTIFICATION_ID, notification())
        stopPipeline()
        try {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, resultData)
            if (projection == null) throw IllegalStateException("MediaProjection unavailable")
            startPipeline(projection!!)
        } catch (_: SecurityException) {
            sendBroadcast(Intent(ACTION_STOPPED).setPackage(packageName))
            stopSelf(startId)
        } catch (_: IllegalStateException) {
            sendBroadcast(Intent(ACTION_STOPPED).setPackage(packageName))
            stopSelf(startId)
        } catch (_: Exception) {
            sendBroadcast(Intent(ACTION_STOPPED).setPackage(packageName))
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    private fun startPipeline(p: MediaProjection) {
        val sampleRate = 48_000
        val channelMask = AudioFormat.CHANNEL_IN_STEREO
        val min = AudioRecord.getMinBufferSize(sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) throw IllegalStateException("AudioRecord buffer unavailable")
        val bufferSize = (min * 4).coerceAtLeast(sampleRate / 10)
        val config = AudioPlaybackCaptureConfiguration.Builder(p)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .build()
        val newRecorder = AudioRecord.Builder()
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(channelMask).build())
            .setBufferSizeInBytes(bufferSize)
            .setAudioPlaybackCaptureConfig(config)
            .build()
        val newTrack = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        if (newRecorder.state != AudioRecord.STATE_INITIALIZED || newTrack.state != AudioTrack.STATE_INITIALIZED) {
            newRecorder.release(); newTrack.release()
            throw IllegalStateException("Audio pipeline could not be initialized")
        }
        recorder = newRecorder
        track = newTrack
        try {
            newRecorder.startRecording()
            newTrack.play()
        } catch (e: Exception) {
            stopPipeline()
            throw e
        }
        running = true
        worker = Thread {
            val pcm = ShortArray(bufferSize / 2)
            while (running) {
                val r = recorder ?: break
                val n = try { r.read(pcm, 0, pcm.size, AudioRecord.READ_BLOCKING) } catch (_: Exception) { -1 }
                if (n <= 0) continue
                val data = if (n == pcm.size) pcm else pcm.copyOf(n)
                dsp.processPcm16Stereo(data, DspSettingsStore.read(this))
                try { track?.write(data, 0, data.size, AudioTrack.WRITE_BLOCKING) } catch (_: Exception) { break }
            }
        }.also { it.name = "YouTube-DLMS-DSP"; it.start() }
    }

    private fun stopPipeline() {
        running = false
        try { recorder?.stop() } catch (_: Exception) {}
        try { track?.pause() } catch (_: Exception) {}
        try { worker?.join(250) } catch (_: Exception) {}
        worker = null
        try { recorder?.release() } catch (_: Exception) {}
        try { track?.release() } catch (_: Exception) {}
        recorder = null; track = null
        try { projection?.stop() } catch (_: Exception) {}
        projection = null
    }

    private fun notification(): Notification {
        val channelId = "youtube_dsp"
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel(channelId, "YouTube DLMS DSP", NotificationManager.IMPORTANCE_LOW))
        val stopIntent = Intent(this, YouTubeDspCaptureService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = android.app.PendingIntent.getService(
            this, 7402, stopIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= 23) android.app.PendingIntent.FLAG_IMMUTABLE else 0
        )
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("YouTube DLMS DSP")
            .setContentText("Playback capture + DSP aktif")
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop DSP", stopPendingIntent)
            .build()
    }

    override fun onDestroy() {
        stopPipeline()
        sendBroadcast(Intent(ACTION_STOPPED).setPackage(packageName))
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val ACTION_STOP = "com.example.audio.ACTION_STOP_YOUTUBE_DSP"
        const val ACTION_STOPPED = "com.example.audio.ACTION_YOUTUBE_DSP_STOPPED"
        private const val NOTIFICATION_ID = 7401
        private const val RESULT_OK = -1
    }
}
