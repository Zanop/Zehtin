package com.zehtin.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object UpdateManager {
    private const val TAG = "UpdateManager"
    private const val VERSION_URL = "https://torbalan.ddns.net/zehtin/update/version.json"
    private val client = OkHttpClient()

    data class UpdateInfo(val apkUrl: String, val description: String?)

    suspend fun checkForUpdates(context: Context, onUpdateAvailable: (UpdateInfo) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(VERSION_URL).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val remoteVersionCode = json.getInt("versionCode")
                    val apkUrl = json.getString("apkUrl")
                    val description = json.optString("description", "")

                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        pInfo.longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        pInfo.versionCode
                    }

                    if (remoteVersionCode > currentVersionCode) {
                        withContext(Dispatchers.Main) {
                            onUpdateAvailable(UpdateInfo(apkUrl, description.ifEmpty { null }))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update check failed", e)
            }
        }
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        apkUrl: String,
        onProgress: (Float) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(apkUrl).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext

                val body = response.body ?: return@withContext
                val totalBytes = body.contentLength()
                val apkFile = File(context.cacheDir, "update.apk")
                
                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var totalRead: Long = 0
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (totalBytes > 0) {
                                val progress = totalRead.toFloat() / totalBytes
                                withContext(Dispatchers.Main) {
                                    onProgress(progress)
                                }
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    installApk(context, apkFile)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
            }
        }
    }

    private fun installApk(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}