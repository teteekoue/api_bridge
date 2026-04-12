package com.ialocalbridge

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

        // Gestion de la page d'accueil (Dashboard)
        if (uri == "/" || uri == "/index.html") {
            val ip = NetworkHelper.getIPAddress()
            return newFixedLengthResponse(Response.Status.OK, MIME_HTML, WebInterface.getHtml(ip, port))
        }

        return when (uri) {
            "/ask" -> {
                // Lecture des paramètres pour GET et POST
                val params = if (method == Method.POST) {
                    val files = HashMap<String, String>()
                    session.parseBody(files)
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
                    // Ajout des headers CORS pour permettre l'appel depuis un navigateur
                    response.addHeader("Access-Control-Allow-Origin", "*")
                    response
                } else {
                    newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Erreur: Paramètre 'q' manquant")
                }
            }
            "/status" -> {
                val isServiceActive = ClickAccessibilityService.instance != null
                val status = if (isServiceActive) "Ready" else "Accessibility Service Disabled"
                val response = newFixedLengthResponse(status)
                response.addHeader("Access-Control-Allow-Origin", "*")
                response
            }
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
        }
    }
}
