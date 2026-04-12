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

        // 1. Cliquer sur la barre de texte pour donner le focus
        service.clickAt(coords.textFieldX, coords.textFieldY)
        delay(800)

        // 2. Coller le texte
        service.pasteText(question)
        delay(500)

        // 3. Fermer le clavier pour ne pas gêner les futurs clics
        service.closeKeyboard()
        delay(500)

        // 4. Cliquer sur Envoyer
        service.clickAt(coords.sendButtonX, coords.sendButtonY)
        
        // 5. Attendre la fin de la génération
        delay(coords.delayAfterSendMs)

        // 6. Faire défiler plusieurs fois pour être sûr d'arriver au dernier message
        service.scrollDown()
        delay(800)
        service.scrollDown()
        delay(800)

        // 7. Cliquer sur le bouton copier
        service.clickAt(coords.copyButtonX, coords.copyButtonY)
        delay(800)

        // 8. Récupérer le contenu du presse-papier
        val result = ClipboardHelper.getFromClipboard(context)
        return if (result.isEmpty()) "Erreur: Le presse-papier est vide ou la copie a échoué" else result
    }
}
