package com.ialocalbridge

import android.content.Context
import android.util.Log
import com.ialocalbridge.utils.ClipboardHelper
import kotlinx.coroutines.delay

class AutomationCoordinator(private val context: Context) {

    private val accessibilityService get() = ClickAccessibilityService.instance
    private val calibrationManager = CalibrationManager(context)
    private val TAG = "AutomationCoordinator"

    suspend fun stopGeneration(): String {
        val service = accessibilityService ?: return "Erreur: Service non activé"
        // On récupère le dernier profil utilisé ou par défaut
        val currentProvider = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("current_provider", "default_provider") ?: "default_provider"
        val coords = calibrationManager.getCoordinates(currentProvider)
        
        service.clickAt(coords.sendButtonX, coords.sendButtonY)
        return "Commande STOP envoyée"
    }

    suspend fun processQuestion(question: String): String {
        val service = accessibilityService ?: return "Erreur: Service d'accessibilité non activé"
        
        val currentProvider = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("current_provider", "default_provider") ?: "default_provider"
        val coords = calibrationManager.getCoordinates(currentProvider)
        
        val oldClipboard = ClipboardHelper.getFromClipboard(context)

        Log.d(TAG, "Démarrage automatisation pour profil: $currentProvider")

        // 1. Collage dans la barre de texte
        ClipboardHelper.copyToClipboard(context, question)
        service.clickAt(coords.textFieldX, coords.textFieldY)
        delay(800)
        service.pasteText()
        delay(500)

        // 2. Fermer le clavier (Bouton Back)
        service.closeKeyboard()
        delay(1000)

        // 3. Appuyer sur Envoyer
        service.clickAt(coords.sendButtonX, coords.sendButtonY)
        delay(coords.delayAfterSendMs) // Attente initiale pour la réponse

        // 4. Boucle de détection (Swipe bas -> Attente -> Copie)
        var finalResult = ""
        var attempts = 0
        val maxAttempts = 20

        while (attempts < maxAttempts) {
            attempts++
            
            // On swipe vers le bas pour forcer l'affichage du dernier message et du bouton copier
            service.swipeUp() 
            delay(1000)

            // On tente de cliquer sur le bouton copier
            service.clickAt(coords.copyButtonX, coords.copyButtonY)
            delay(1500)

            val newClipboard = ClipboardHelper.getFromClipboard(context)
            if (newClipboard.isNotEmpty() && newClipboard != oldClipboard && newClipboard != question) {
                finalResult = newClipboard
                break
            }

            Log.d(TAG, "Tentative $attempts : pas encore de nouvelle réponse...")
            delay(2000) // Attente de 3s au total entre chaque cycle
        }

        return if (finalResult.isNotEmpty()) {
            finalResult
        } else {
            "Erreur: Délai d'attente dépassé ou échec de la copie."
        }
    }
}
