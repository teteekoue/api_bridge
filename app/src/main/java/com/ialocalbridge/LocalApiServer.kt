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

class LocalApiServer(private val port: Int, private val context: Context) : NanoHTTPD(null, port) {

    private val coordinator = AutomationCoordinator(context)
    private val fileUploader = FileUploader()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val TAG = "LocalApiServer"
    
    private val jobs = HashMap<String, JobStatus>()

    data class JobStatus(
        val status: String,
        val result: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    init {
        val tempDir = File(context.cacheDir, "nanohttpd_temp")
        if (!tempDir.exists()) tempDir.mkdirs()
        System.setProperty("java.io.tmpdir", tempDir.absolutePath)
        Log.d(TAG, "Server initialized at ${tempDir.absolutePath}")
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
                    newFixedLengthResponse(status)
                }
                "/stop" -> {
                    mainHandler.post {
                        runBlocking {
                            coordinator.stopGeneration()
                        }
                    }
                    newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "STOP_SENT")
                }
                else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
            }
            addCORSHeaders(response)
            response
        } catch (e: Exception) {
            Log.e(TAG, "Serve Exception", e)
            val response = newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Crash: ${e.message}")
            addCORSHeaders(response)
            response
        }
    }

    private fun handleAskRequest(session: IHTTPSession): Response {
        val params = if (session.method == Method.POST) {
            val files = HashMap<String, String>()
            try { session.parseBody(files) } catch (e: Exception) {}
            session.parameters
        } else {
            session.parameters
        }
        val q = params["q"]?.get(0) ?: params["question"]?.get(0)
        return if (q != null) startAutomationJob(q) 
        else newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Manquant: q")
    }

    private fun handleUploadRequest(session: IHTTPSession): Response {
        if (session.method != Method.POST) return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "POST requis")
        val files = HashMap<String, String>()
        session.parseBody(files)
        val temp = files["file"]
        val name = session.parameters["file"]?.get(0) ?: "file.bin"

        return if (temp != null) {
            val res = fileUploader.uploadWithFallback(File(temp), name)
            if (res.success) {
                val json = "{\"success\":true, \"url\":\"${res.url}\"}"
                newFixedLengthResponse(Response.Status.OK, "application/json", json)
            } else {
                newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", "{\"success\":false, \"error\":\"${res.errorLog}\"}")
            }
        } else newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "No file")
    }

    private fun handleAskWithFileRequest(session: IHTTPSession): Response {
        val files = HashMap<String, String>()
        session.parseBody(files)
        val temp = files["file"]
        val name = session.parameters["file"]?.get(0) ?: "file.bin"
        val q = session.parameters["q"]?.get(0) ?: session.parameters["question"]?.get(0)

        return if (temp != null) {
            val res = fileUploader.uploadWithFallback(File(temp), name)
            if (res.success) {
                val msg = FileMessageBuilder.build(name, res.url!!, q)
                startAutomationJob(msg)
            } else {
                newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Upload Failed:\n${res.errorLog}")
            }
        } else newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "No file")
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
                    jobs[jobId] = JobStatus("error", "Error: ${e.message}")
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
