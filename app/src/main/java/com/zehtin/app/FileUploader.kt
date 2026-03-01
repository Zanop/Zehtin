package com.zehtin.app

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object FileUploader {
    private const val UPLOAD_URL = "https://torbalan.ddns.net/zehtin/upload"
    private val client = OkHttpClient()

    data class UploadResult(
        val fileUrl: String,
        val fileName: String,
        val fileSize: String,
        val isImage: Boolean
    )

    suspend fun uploadFile(context: Context, uri: Uri): UploadResult? {
        return withContext(Dispatchers.IO) {
            try {
                // Get file name and size
                var fileName = "file"
                var fileSize = 0L
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    cursor.moveToFirst()
                    fileName = cursor.getString(nameIndex) ?: "file"
                    fileSize = cursor.getLong(sizeIndex)
                }

                // Read file bytes
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    ?: return@withContext null

                // Detect mime type
                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

                // Build multipart request
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file", fileName,
                        bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                    )
                    .build()

                val request = Request.Builder()
                    .url(UPLOAD_URL)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext null

                val json = JSONObject(response.body?.string() ?: return@withContext null)
                UploadResult(
                    fileUrl = json.getString("fileUrl"),
                    fileName = json.getString("fileName"),
                    fileSize = json.getString("fileSize"),
                    isImage = json.getBoolean("isImage")
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}