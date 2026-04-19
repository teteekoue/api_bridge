package com.ialocalbridge

import android.content.Context
import com.ialocalbridge.utils.NetworkHelper
import com.ialocalbridge.utils.WebInterface
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import kotlinx.coroutines.runBlocking

class LocalApiServer(private val port: Int, private val context: Context) : NanoHTTPD(port) {

    private val coordinator = AutomationCoordinator(context)

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        // Gestion de l'Option (Pre-flight) pour CORS
        if (method == Method.OPTIONS) {
            val response = newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "")
            addCORSHeaders(response)
            return response
        }

        // Gestion de la page d'accueil (Dashboard)
        if (uri == "/" || uri == "/index.html") {
            val ip = NetworkHelper.getIPAddress()
            return newFixedLengthResponse(Response.Status.OK, MIME_HTML, WebInterface.getHtml(ip, port))
        }

        return when (uri) {
            "/ask" -> {
                try {
                    val params = if (method == Method.POST) {
                        val files = HashMap<String, String>()
                        session.parseBody(files) // Crucial pour lire le corps POST
                        session.parameters
                    } else {
                        session.parameters
                    }

                    val question = params["q"]?.get(0) ?: params["question"]?.get(0)
                    
                    if (question != null) {
                        val responseText = runBlocking {
                            coordinator.processQuestion(question)
                        }
                        val response = newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, responseText)
                        addCORSHeaders(response)
                        response
                    } else {
                        val response = newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Erreur: Paramètre 'q' manquant")
                        addCORSHeaders(response)
                        response
                    }
                } catch (e: Exception) {
                    newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Erreur serveur: ${e.message}")
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
