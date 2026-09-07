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
import android.media.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/** Experimental Android playback-capture route. It is intentionally opt-in because Android requires user consent. */
class YouTubeDspCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var recorder: AudioRecord? = null
    private var track: AudioTrack? = null
    private var worker: Thread? = null
    private var running = false
    private val dsp = DlmsDspEngine(48_000)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
        val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
        if (resultCode < 0 || resultData == null) return START_NOT_STICKY
        startForeground(NOTIFICATION_ID, notification())
        stopPipeline()
        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mgr.getMediaProjection(resultCode, resultData)
        startPipeline()
        return START_NOT_STICKY
    }

    private fun startPipeline() {
        val p = projection ?: return
        val channelMask = AudioFormat.CHANNEL_IN_STEREO
        val min = AudioRecord.getMinBufferSize(48_000, channelMask, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) return
        val bufferSize = (min * 4).coerceAtLeast(48_000)
        val config = AudioPlaybackCaptureConfiguration.Builder(p)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .build()
        recorder = AudioRecord.Builder()
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(48_000)
                .setChannelMask(channelMask)
                .build())
            .setBufferSizeInBytes(bufferSize)
            .setAudioPlaybackCaptureConfig(config)
            .build()
        track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(48_000)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build())
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        recorder?.startRecording()
        track?.play()
        running = true
        worker = Thread {
            val pcm = ShortArray(bufferSize / 2)
            while (running) {
                val n = recorder?.read(pcm, 0, pcm.size, AudioRecord.READ_BLOCKING) ?: -1
                if (n > 0) {
                    val settings = DspSettingsStore.read(this)
                    if (n < pcm.size) {
                        val chunk = pcm.copyOf(n)
                        dsp.processPcm16Stereo(chunk, settings)
                        track?.write(chunk, 0, chunk.size)
                    } else {
                        dsp.processPcm16Stereo(pcm, settings)
                        track?.write(pcm, 0, pcm.size)
                    }
                }
            }
        }.also { it.name = "YouTube-DLMS-DSP"; it.start() }
    }

    private fun stopPipeline() {
        running = false
        try { recorder?.stop() } catch (_: Exception) {}
        try { track?.pause() } catch (_: Exception) {}
        worker?.join(250)
        recorder?.release(); recorder = null
        track?.release(); track = null
        projection?.stop(); projection = null
    }

    private fun notification(): Notification {
        val channelId = "youtube_dsp"
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel(channelId, "YouTube DLMS DSP", NotificationManager.IMPORTANCE_LOW))
        }
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("YouTube DLMS DSP")
            .setContentText("Audio capture + DSP aktif")
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopPipeline()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val NOTIFICATION_ID = 7401
    }
}
