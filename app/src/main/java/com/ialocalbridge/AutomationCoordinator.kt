package com.ialocalbridge

import android.content.Context
import com.ialocalbridge.CalibrationManager
import com.ialocalbridge.models.ProviderCoordinates
import com.ialocalbridge.utils.ClipboardHelper
import kotlinx.coroutines.delay

class AutomationCoordinator(private val context: Context) {

    private val calibrationManager = CalibrationManager(context)
    private val accessibilityService get() = ClickAccessibilityService.instance

    suspend fun processQuestion(question: String): String {
        val service = accessibilityService ?: return "Erreur: Service d'accessibilité non activé"
        val coords = calibrationManager.getCoordinates("default_provider")
        val oldClipboard = ClipboardHelper.getFromClipboard(context)

        // 1. Injection du texte SANS CLIC (évite le clavier dans la plupart des cas)
        service.findAndFocusEditable()
        delay(200)
        service.pasteText(question)
        delay(200)

        // 2. Fermeture préventive immédiate du clavier
        service.closeKeyboard()
        delay(500)

        // 3. Clic sur Envoyer
        service.clickAt(coords.sendButtonX, coords.sendButtonY)
        delay(1000) // On attend que la génération commence

        // 4. BOUCLE D'ATTENTE INTELLIGENTE (Attente de la fin de génération)
        // On observe le bouton Envoyer. S'il est désactivé (Génération), on attend.
        var isGenerating = true
        var attempts = 0
        while (isGenerating && attempts < 100) { // Timeout max ~25-30 secondes
            val isButtonEnabled = service.isNodeEnabledAt(coords.sendButtonX, coords.sendButtonY)
            if (isButtonEnabled) {
                isGenerating = false
            } else {
                delay(300)
                attempts++
            }
        }

        // 5. Fermeture supplémentaire du clavier après génération (certaines apps le ré-ouvrent)
        service.closeKeyboard()
        delay(300)

        // 6. DÉFILEMENT EXTRÊME
        for (i in 0..2) {
            service.forceScrollToBottom()
            delay(500)
        }

        // 7. BOUCLE DE COPIE (Vérification que le presse-papier a bien changé)
        var finalResult = ""
        for (i in 0..3) {
            service.clickAt(coords.copyButtonX, coords.copyButtonY)
            delay(1000)
            val currentClipboard = ClipboardHelper.getFromClipboard(context)
            if (currentClipboard.isNotEmpty() && currentClipboard != oldClipboard) {
                finalResult = currentClipboard
                break
            }
        }

        return if (finalResult.isEmpty()) {
            "Erreur: Impossible de récupérer la réponse. La copie a échoué ou l'IA n'a rien renvoyé."
        } else {
            finalResult
        }
    }
}
