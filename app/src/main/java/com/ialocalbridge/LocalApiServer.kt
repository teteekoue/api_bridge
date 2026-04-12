package com.ialocalbridge

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking

class LocalApiServer(port: Int, private val context: Context) : NanoHTTPD(port) {

    private val coordinator = AutomationCoordinator(context)

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val params = session.parameters

        return when (uri) {
            "/ask" -> {
                val question = params["q"]?.get(0) ?: params["question"]?.get(0)
                if (question != null) {
                    // On utilise runBlocking pour attendre la fin de l'automatisation
                    // car NanoHTTPD s'attend à une réponse immédiate
                    val responseText = runBlocking {
                        coordinator.processQuestion(question)
                    }
                    newFixedLengthResponse(responseText)
                } else {
                    newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Erreur: Paramètre 'q' manquant")
                }
            }
            "/status" -> {
                val isServiceActive = ClickAccessibilityService.instance != null
                val status = if (isServiceActive) "Ready" else "Accessibility Service Disabled"
                newFixedLengthResponse(status)
            }
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
        }
    }
}
