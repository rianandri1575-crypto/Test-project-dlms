package com.example.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val latestVersionName: String = "",
    val latestVersionCode: Int = 0,
    val releaseNotes: String = "",
    val downloadUrl: String = "",
    val isUpdateAvailable: Boolean = false,
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val errorMessage: String? = null
)

object AppUpdateManager {

    private val _updateState = MutableStateFlow(AppUpdateInfo())
    val updateState: StateFlow<AppUpdateInfo> = _updateState.asStateFlow()

    private var downloadId: Long = -1L
    private var downloadReceiverRegistered = false

    fun getCurrentVersionName(): String = BuildConfig.VERSION_NAME
    fun getCurrentVersionCode(): Int = BuildConfig.VERSION_CODE

    /**
     * Cek pembaruan rilis.
     * Mendukung URL rilis publik atau manual check endpoint.
     */
    suspend fun checkForUpdates(
        context: Context,
        customCheckUrl: String? = null
    ) {
        _updateState.value = _updateState.value.copy(
            isChecking = true,
            errorMessage = null
        )

        withContext(Dispatchers.IO) {
            try {
                // Endpoint URL: bisa custom, atau fallback ke manifest update info
                val checkUrl = customCheckUrl ?: "https://api.github.com/repos/rianandri1575/dlms-sound-management/releases/latest"
                val url = URL(checkUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    setRequestProperty("User-Agent", "DLMS-App-${BuildConfig.VERSION_NAME}")
                }

                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)

                    val tagName = json.optString("tag_name", "").replace("v", "")
                    val releaseBody = json.optString("body", "Peningkatan performa dan perbaikan bug.")
                    var apkDownloadUrl = ""

                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk", ignoreCase = true)) {
                                apkDownloadUrl = asset.optString("browser_download_url", "")
                                break
                            }
                        }
                    }

                    if (apkDownloadUrl.isBlank()) {
                        apkDownloadUrl = json.optString("html_url", "")
                    }

                    val serverVersionCode = try {
                        val numbers = tagName.split(".").mapNotNull { it.toIntOrNull() }
                        if (numbers.size >= 2) numbers[0] * 10 + numbers[1] else tagName.toIntOrNull() ?: 2
                    } catch (e: Exception) {
                        2
                    }

                    val isAvailable = serverVersionCode > BuildConfig.VERSION_CODE

                    _updateState.value = AppUpdateInfo(
                        latestVersionName = if (tagName.isNotBlank()) tagName else "1.2",
                        latestVersionCode = serverVersionCode,
                        releaseNotes = releaseBody,
                        downloadUrl = apkDownloadUrl,
                        isUpdateAvailable = isAvailable,
                        isChecking = false
                    )
                } else {
                    // Status 404 atau repo belum rilis public release:
                    // Tetap berikan informasi bahwa versi lokal adalah yang termutakhir (Up-to-Date)
                    _updateState.value = _updateState.value.copy(
                        isChecking = false,
                        isUpdateAvailable = false,
                        latestVersionName = BuildConfig.VERSION_NAME,
                        latestVersionCode = BuildConfig.VERSION_CODE,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                // Offline atau timeout
                _updateState.value = _updateState.value.copy(
                    isChecking = false,
                    errorMessage = "Tidak dapat memeriksa pembaruan: Pastikan terhubung ke internet."
                )
            }
        }
    }

    /**
     * Download dan jalankan installer pembaruan APK langsung di HP
     */
    fun startDownloadAndInstall(context: Context, downloadUrl: String) {
        if (downloadUrl.isBlank() || !downloadUrl.startsWith("http")) {
            Toast.makeText(context, "URL unduhan tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        // Jika link rilis web/github bukan direct APK link, buka browser
        if (!downloadUrl.endsWith(".apk", ignoreCase = true)) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
            return
        }

        try {
            val fileName = "DLMS_Update_${System.currentTimeMillis()}.apk"
            val destinationFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle("Mengunduh Pembaruan DLMS")
                .setDescription("Versi baru sedang diunduh...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(destinationFile))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = downloadManager.enqueue(request)

            _updateState.value = _updateState.value.copy(
                isDownloading = true,
                errorMessage = null
            )

            Toast.makeText(context, "Mengunduh pembaruan di latar belakang...", Toast.LENGTH_SHORT).show()

            val onCompleteReceiver = object : BroadcastReceiver() {
                override fun onReceive(recvContext: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                    if (id == downloadId) {
                        _updateState.value = _updateState.value.copy(isDownloading = false)
                        installApk(context, destinationFile)
                        try {
                            if (downloadReceiverRegistered) {
                                context.unregisterReceiver(this)
                                downloadReceiverRegistered = false
                            }
                        } catch (e: Exception) {}
                    }
                }
            }

            val intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(onCompleteReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(onCompleteReceiver, intentFilter)
            }
            downloadReceiverRegistered = true

        } catch (e: Exception) {
            _updateState.value = _updateState.value.copy(
                isDownloading = false,
                errorMessage = "Gagal memulai unduhan: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Membuka installer bawaan Android untuk menimpa/memperbarui APK tanpa uninstall
     */
    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) {
            Toast.makeText(context, "File pembaruan tidak ditemukan.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            // Periksa izin Unknown Sources di Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    Toast.makeText(context, "Izinkan penginstalan dari sumber ini, lalu ulangi.", Toast.LENGTH_LONG).show()
                    return
                }
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal meluncurkan installer: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun dismissDialog() {
        _updateState.value = _updateState.value.copy(
            isChecking = false,
            errorMessage = null
        )
    }
}
