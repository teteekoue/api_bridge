package com.ialocalbridge

import android.content.Context
import com.ialocalbridge.utils.ClipboardHelper
import kotlinx.coroutines.delay

class AutomationCoordinator(private val context: Context) {

    private val calibrationManager = CalibrationManager(context)
    private val accessibilityService get() = ClickAccessibilityService.instance

    suspend fun processQuestion(question: String): String {
        val service = accessibilityService ?: return "Erreur: Service d'accessibilité non activé"
        val coords = calibrationManager.getCoordinates("default_provider")

        // 1. Cliquer sur la barre de texte pour donner le focus
        service.clickAt(coords.textFieldX, coords.textFieldY)
        delay(500)

        // 2. Coller le texte
        service.pasteText(question)
        delay(500)

        // 3. Cliquer sur Envoyer
        service.clickAt(coords.sendButtonX, coords.sendButtonY)
        
        // 4. Attendre la fin de la génération (On utilise le délai calibré par défaut)
        // Note: L'observation du bouton "Stop" pourrait être ajoutée ici
        delay(coords.delayAfterSendMs)

        // 5. Faire défiler vers le bas (pour s'assurer que le bouton copier est visible)
        service.scrollDown()
        delay(500)

        // 6. Cliquer sur le bouton copier
        service.clickAt(coords.copyButtonX, coords.copyButtonY)
        delay(500)

        // 7. Récupérer le contenu du presse-papier
        return ClipboardHelper.getFromClipboard(context)
    }
}
