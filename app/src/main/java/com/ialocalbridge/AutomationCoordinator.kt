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
        delay(600)

        // 2. Coller le texte directement sans simuler de touches clavier
        service.pasteText(question)
        delay(400)

        // 3. Fermer IMMÉDIATEMENT le clavier s'il s'est ouvert (mesure de sécurité)
        service.closeKeyboard()
        delay(600)

        // 4. Cliquer sur Envoyer (après fermeture du clavier, les coordonnées sont stables)
        service.clickAt(coords.sendButtonX, coords.sendButtonY)
        
        // 5. Attendre la fin de la génération (7s par défaut)
        delay(coords.delayAfterSendMs)

        // 6. Fermer le clavier si jamais il s'est ré-ouvert (parfois l'app d'IA le fait après l'envoi)
        service.closeKeyboard()
        delay(400)

        // 7. Défilement agressif vers la fin de la page
        service.forceScrollToBottom()
        delay(1000)

        // 8. Cliquer sur le bouton copier
        service.clickAt(coords.copyButtonX, coords.copyButtonY)
        delay(800)

        // 9. Récupérer le contenu du presse-papier
        val result = ClipboardHelper.getFromClipboard(context)
        return if (result.isEmpty()) "Erreur: Le presse-papier est vide. L'IA n'a peut-être pas terminé ou le bouton copier est invisible." else result
    }
}
