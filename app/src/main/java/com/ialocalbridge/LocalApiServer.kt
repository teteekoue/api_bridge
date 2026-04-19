package com.ialocalbridge

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.ialocalbridge.utils.NetworkHelper
import com.ialocalbridge.utils.WebInterface
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture

class LocalApiServer(private val port: Int, private val context: Context) : NanoHTTPD(port) {

    private val coordinator = AutomationCoordinator(context)
    private val mainHandler = Handler(Looper.getMainLooper())

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
            "/ask" -> {
                try {
                    val params = if (method == Method.POST) {
                        val files = HashMap<String, String>()
                        session.parseBody(files)
                        session.parameters
                    } else {
                        session.parameters
                    }

                    val question = params["q"]?.get(0) ?: params["question"]?.get(0)
                    
                    if (question != null) {
                        // Exécuter l'automation sur le thread principal pour éviter les erreurs de Looper
                        val future = CompletableFuture<String>()
                        mainHandler.post {
                            runBlocking {
                                try {
                                    val result = coordinator.processQuestion(question)
                                    future.complete(result)
                                } catch (e: Exception) {
                                    future.complete("Erreur Automation: ${e.message}")
                                }
                            }
                        }
                        
                        val responseText = future.get() // Attendre le résultat
                        val response = newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, responseText)
                        addCORSHeaders(response)
                        response
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
            "/status" -> {
                val isServiceActive = ClickAccessibilityService.instance != null
                val status = if (isServiceActive) "Ready" else "Accessibility Service Disabled"
                val response = newFixedLengthResponse(status)
                addCORSHeaders(response)
                response
            }
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
        }
    }

    private fun addCORSHeaders(response: Response) {
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
    }
}
