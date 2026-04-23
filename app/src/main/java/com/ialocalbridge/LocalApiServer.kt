package com.ialocalbridge

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.ialocalbridge.utils.NetworkHelper
import com.ialocalbridge.utils.WebInterface
import com.ialocalbridge.utils.FileUploader
import com.ialocalbridge.utils.FileMessageBuilder
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import kotlinx.coroutines.runBlocking
import java.io.File

class LocalApiServer(private val port: Int, private val context: Context) : NanoHTTPD(port) {

    private val coordinator = AutomationCoordinator(context)
    private val fileUploader = FileUploader()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val TAG = "LocalApiServer"
    
    var isFileModeEnabled = false
    private val jobs = HashMap<String, JobStatus>()

    data class JobStatus(
        val status: String,
        val result: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    // Configurer NanoHTTPD pour utiliser le cache de l'app pour les fichiers temporaires
    init {
        val tempDir = File(context.cacheDir, "nanohttpd_temp")
        if (!tempDir.exists()) tempDir.mkdirs()
        System.setProperty("java.io.tmpdir", tempDir.absolutePath)
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        if (method == Method.OPTIONS) {
            val response = newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "")
            addCORSHeaders(response)
            return response
        }

        if (uri == "/" || uri == "/index.html") {
            val ip = NetworkHelper.getIPAddress()
            return newFixedLengthResponse(Response.Status.OK, MIME_HTML, WebInterface.getHtml(ip, port))
        }

        return try {
            val response = when (uri) {
                "/ask" -> handleAskRequest(session)
                "/upload" -> handleUploadRequest(session)
                "/ask-with-file" -> handleAskWithFileRequest(session)
                "/result" -> {
                    val jobId = session.parameters["id"]?.get(0)
                    val job = jobs[jobId]
                    val text = when {
                        jobId == null -> "Erreur: ID manquant"
                        job == null -> "Erreur: Job introuvable"
                        job.status == "pending" -> "STILL_WORKING"
                        else -> job.result ?: "Erreur inconnue"
                    }
                    newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, text)
                }
                "/status" -> {
                    val isServiceActive = ClickAccessibilityService.instance != null
                    val status = if (isServiceActive) "Ready" else "Accessibility Service Disabled"
                    val fileModeStatus = if (isFileModeEnabled) " (File Mode ON)" else ""
                    newFixedLengthResponse(status + fileModeStatus)
                }
                else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
            }
            addCORSHeaders(response)
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error serving request $uri", e)
            val response = newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Erreur serveur: ${e.message}")
            addCORSHeaders(response)
            response
        }
    }

    private fun handleAskRequest(session: IHTTPSession): Response {
        val params = if (session.method == Method.POST) {
            val files = HashMap<String, String>()
            session.parseBody(files)
            session.parameters
        } else {
            session.parameters
        }

        val question = params["q"]?.get(0) ?: params["question"]?.get(0)
        return if (question != null) {
            startAutomationJob(question)
        } else {
            newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Erreur: Paramètre 'q' manquant")
        }
    }

    private fun handleUploadRequest(session: IHTTPSession): Response {
        if (session.method != Method.POST) return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "POST requis")

        val files = HashMap<String, String>()
        session.parseBody(files)
        
        val tempPath = files["file"]
        // Dans NanoHTTPD multipart, le nom original est souvent stocké dans session.parameters["file"]
        val originalName = session.parameters["file"]?.get(0) ?: "file_upload"

        return if (tempPath != null) {
            val fileToUpload = File(tempPath)
            val result = fileUploader.uploadFile(fileToUpload, originalName)
            if (result.success) {
                val json = "{\"success\": true, \"filename\": \"${result.filename}\", \"url\": \"${result.url}\"}"
                newFixedLengthResponse(Response.Status.OK, "application/json", json)
            } else {
                newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", "{\"success\": false, \"error\": \"${result.error}\"}")
            }
        } else {
            newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Erreur: Aucun fichier reçu")
        }
    }

    private fun handleAskWithFileRequest(session: IHTTPSession): Response {
        if (session.method != Method.POST) return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "POST requis")

        val files = HashMap<String, String>()
        session.parseBody(files)
        
        val tempPath = files["file"]
        val originalName = session.parameters["file"]?.get(0) ?: "file_upload"
        val question = session.parameters["q"]?.get(0) ?: session.parameters["question"]?.get(0)

        return if (tempPath != null) {
            val fileToUpload = File(tempPath)
            val uploadResult = fileUploader.uploadFile(fileToUpload, originalName)
            if (uploadResult.success) {
                val formattedMessage = FileMessageBuilder.build(uploadResult.filename ?: originalName, uploadResult.url!!, question)
                startAutomationJob(formattedMessage)
            } else {
                newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Erreur upload: ${uploadResult.error}")
            }
        } else {
            newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Erreur: Aucun fichier reçu")
        }
    }

    private fun startAutomationJob(content: String): Response {
        val jobId = java.util.UUID.randomUUID().toString()
        jobs[jobId] = JobStatus("pending")

        mainHandler.post {
            runBlocking {
                try {
                    val result = coordinator.processQuestion(content)
                    jobs[jobId] = JobStatus("completed", result)
                } catch (e: Exception) {
                    Log.e(TAG, "Automation error", e)
                    jobs[jobId] = JobStatus("error", "Erreur: ${e.message}")
                }
            }
        }
        return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, jobId)
    }

    private fun addCORSHeaders(response: Response) {
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
    }
}
