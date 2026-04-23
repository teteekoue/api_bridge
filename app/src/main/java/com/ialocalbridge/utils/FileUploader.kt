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
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val TAG = "FileUploader"

    data class UploadResult(
        val success: Boolean,
        val url: String? = null,
        val errorLog: String = ""
    )

    fun uploadWithFallback(file: File, filename: String): UploadResult {
        val errors = StringBuilder()
        Log.d(TAG, "Starting upload fallback chain for $filename")
        
        // 1. Catbox
        try {
            Log.d(TAG, "Trying Catbox...")
            val url = uploadToCatbox(file, filename)
            if (url != null) return UploadResult(true, url)
            else errors.append("Catbox: Réponse invalide\n")
        } catch (e: Exception) {
            errors.append("Catbox: ${e.message}\n")
        }

        // 2. Tmp.ninja
        try {
            Log.d(TAG, "Trying Tmp.ninja...")
            val url = uploadToTmpNinja(file, filename)
            if (url != null) return UploadResult(true, url)
            else errors.append("Tmp.ninja: Réponse invalide\n")
        } catch (e: Exception) {
            errors.append("Tmp.ninja: ${e.message}\n")
        }

        // 3. File.io
        try {
            Log.d(TAG, "Trying File.io...")
            val url = uploadToFileIo(file, filename)
            if (url != null) return UploadResult(true, url)
            else errors.append("File.io: Erreur API\n")
        } catch (e: Exception) {
            errors.append("File.io: ${e.message}\n")
        }

        return UploadResult(false, errorLog = errors.toString())
    }

    private fun uploadToCatbox(file: File, filename: String): String? {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("reqtype", "fileupload")
            .addFormDataPart("fileToUpload", filename, file.asRequestBody("application/octet-stream".toMediaTypeOrNull()))
            .build()
        val request = Request.Builder().url("https://catbox.moe/user/api.php").post(body).build()
        client.newCall(request).execute().use { resp ->
            val out = resp.body?.string()?.trim()
            return if (resp.isSuccessful && out?.startsWith("http") == true) out else null
        }
    }

    private fun uploadToTmpNinja(file: File, filename: String): String? {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", filename, file.asRequestBody("application/octet-stream".toMediaTypeOrNull()))
            .build()
        val request = Request.Builder().url("https://tmp.ninja/api.php?d=upload").post(body).build()
        client.newCall(request).execute().use { resp ->
            val out = resp.body?.string()?.trim()
            return if (resp.isSuccessful && out?.startsWith("http") == true) out else null
        }
    }

    private fun uploadToFileIo(file: File, filename: String): String? {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", filename, file.asRequestBody("application/octet-stream".toMediaTypeOrNull()))
            .build()
        val request = Request.Builder().url("https://file.io").post(body).build()
        client.newCall(request).execute().use { resp ->
            val out = resp.body?.string()
            if (resp.isSuccessful && out != null) {
                val json = JSONObject(out)
                return if (json.optBoolean("success")) json.optString("link") else null
            }
            return null
        }
    }
}
