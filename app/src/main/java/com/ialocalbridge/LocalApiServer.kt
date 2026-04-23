package com.ialocalbridge

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.ialocalbridge.utils.NetworkHelper
import com.ialocalbridge.utils.WebInterface
import com.ialocalbridge.utils.FileUploader
import com.ialocalbridge.utils.FileMessageBuilder
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.CompletableFuture

class LocalApiServer(private val port: Int, private val context: Context) : NanoHTTPD(port) {

    private val coordinator = AutomationCoordinator(context)
    private val fileUploader = FileUploader()
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // État du mode fichier
    var isFileModeEnabled = false

    // Stockage des résultats des jobs
    private val jobs = HashMap<String, JobStatus>()

    data class JobStatus(
        val status: String, // "pending", "completed", "error"
        val result: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

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

        return when (uri) {
            "/ask" -> handleAskRequest(session)
            "/upload" -> handleUploadRequest(session)
            "/ask-with-file" -> handleAskWithFileRequest(session)
            "/result" -> {
                val jobId = session.parameters["id"]?.get(0)
                val job = jobs[jobId]
                
                val responseText = when {
                    jobId == null -> "Erreur: ID manquant"
                    job == null -> "Erreur: Job introuvable"
                    job.status == "pending" -> "STILL_WORKING"
                    else -> job.result ?: "Erreur inconnue"
                }
                
                val response = newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, responseText)
                addCORSHeaders(response)
                response
            }
            "/status" -> {
                val isServiceActive = ClickAccessibilityService.instance != null
                val status = if (isServiceActive) "Ready" else "Accessibility Service Disabled"
                val fileModeStatus = if (isFileModeEnabled) " (File Mode ON)" else ""
                val response = newFixedLengthResponse(status + fileModeStatus)
                addCORSHeaders(response)
                response
            }
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
        }
    }

    private fun handleAskRequest(session: IHTTPSession): Response {
        try {
            val params = if (session.method == Method.POST) {
                val files = HashMap<String, String>()
                session.parseBody(files)
                session.parameters
            } else {
                session.parameters
            }

            val question = params["q"]?.get(0) ?: params["question"]?.get(0)
            
            if (question != null) {
                return startAutomationJob(question)
            } else {
                val response = newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Erreur: Paramètre 'q' manquant")
                addCORSHeaders(response)
                response
            }
        } catch (e: Exception) {
            val response = newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Erreur serveur: ${e.message}")
            addCORSHeaders(response)
            response
        }
    }

    private fun handleUploadRequest(session: IHTTPSession): Response {
        if (session.method != Method.POST) {
            return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "POST requis")
        }

        return try {
            val files = HashMap<String, String>()
            session.parseBody(files)
            
            val tempPath = files["file"]
            val originalName = session.parameters["file"]?.get(0) ?: "file_upload"

            if (tempPath != null) {
                val fileToUpload = File(tempPath)
                val result = fileUploader.uploadFile(fileToUpload, originalName)
                
                if (result.success) {
                    val jsonResponse = "{\"success\": true, \"filename\": \"${result.filename}\", \"url\": \"${result.url}\"}"
                    val response = newFixedLengthResponse(Response.Status.OK, "application/json", jsonResponse)
                    addCORSHeaders(response)
                    response
                } else {
                    val response = newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", "{\"success\": false, \"error\": \"${result.error}\"}")
                    addCORSHeaders(response)
                    response
                }
            } else {
                val response = newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Erreur: Aucun fichier reçu")
                addCORSHeaders(response)
                response
            }
        } catch (e: Exception) {
            val response = newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Erreur upload: ${e.message}")
            addCORSHeaders(response)
            response
        }
    }

    private fun handleAskWithFileRequest(session: IHTTPSession): Response {
        if (session.method != Method.POST) {
            return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "POST requis")
        }

        return try {
            val files = HashMap<String, String>()
            session.parseBody(files)
            
            val tempPath = files["file"]
            val originalName = session.parameters["file"]?.get(0) ?: "file_upload"
            val question = session.parameters["q"]?.get(0) ?: session.parameters["question"]?.get(0)

            if (tempPath != null) {
                val fileToUpload = File(tempPath)
                val uploadResult = fileUploader.uploadFile(fileToUpload, originalName)
                
                if (uploadResult.success) {
                    val formattedMessage = FileMessageBuilder.build(uploadResult.filename!!, uploadResult.url!!, question)
                    startAutomationJob(formattedMessage)
                } else {
                    val response = newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Erreur upload: ${uploadResult.error}")
                    addCORSHeaders(response)
                    response
                }
            } else {
                val response = newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Erreur: Aucun fichier reçu")
                addCORSHeaders(response)
                response
            }
        } catch (e: Exception) {
            val response = newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Erreur: ${e.message}")
            addCORSHeaders(response)
            response
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
                    jobs[jobId] = JobStatus("error", "Erreur: ${e.message}")
                }
            }
        }
        
        val response = newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, jobId)
        addCORSHeaders(response)
        return response
    }

    private fun addCORSHeaders(response: Response) {
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
    }
}
