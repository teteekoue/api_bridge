package com.ialocalbridge.utils

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class FileUploader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val TAG = "FileUploader"

    data class UploadResult(
        val success: Boolean,
        val url: String? = null,
        val filename: String? = null,
        val error: String? = null
    )

    fun uploadFile(file: File, originalFilename: String): UploadResult {
        Log.d(TAG, "Uploading file: ${file.absolutePath} (Original name: $originalFilename)")
        
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                originalFilename,
                file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("https://tmp.ninja/api.php?d=upload") // tmp.ninja utilise souvent api.php?d=upload
            .post(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.d(TAG, "Upload Response Code: ${response.code}")
                Log.d(TAG, "Upload Response Body: $responseBody")

                if (response.isSuccessful && responseBody != null) {
                    if (responseBody.trim().startsWith("http")) {
                        UploadResult(true, responseBody.trim(), originalFilename)
                    } else {
                        try {
                            val json = JSONObject(responseBody)
                            val url = if (json.has("url")) json.getString("url") else ""
                            if (url.isNotEmpty()) {
                                UploadResult(true, url, originalFilename)
                            } else {
                                UploadResult(false, error = "URL non trouvée dans JSON: $responseBody")
                            }
                        } catch (e: Exception) {
                            UploadResult(false, error = "Réponse non reconnue: $responseBody")
                        }
                    }
                } else {
                    UploadResult(false, error = "Erreur HTTP ${response.code}: $responseBody")
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Upload error", e)
            UploadResult(false, error = "Erreur réseau: ${e.message}")
        }
    }
}
