package com.ialocalbridge

import android.content.Context
import android.util.Log
import com.ialocalbridge.utils.ClipboardHelper
import kotlinx.coroutines.delay

class AutomationCoordinator(private val context: Context) {

    private val accessibilityService get() = ClickAccessibilityService.instance
    private val calibrationManager = CalibrationManager(context)
    private val TAG = "AutomationCoordinator"

    companion object {
        @Volatile
        var isRunning = false
        
        fun stopCurrentJob() {
            isRunning = false
        }
    }

    suspend fun stopGeneration(): String {
        stopCurrentJob()
        val service = accessibilityService ?: return "Erreur: Service non activé"
        // ... reste du code pour cliquer sur stop dans l'UI de l'IA
        // On récupère le dernier profil utilisé ou par défaut
        val currentProvider = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("current_provider", "default_provider") ?: "default_provider"
        val coords = calibrationManager.getCoordinates(currentProvider)
        
        service.clickAt(coords.sendButtonX, coords.sendButtonY)
        return "Commande STOP envoyée"
    }

    suspend fun processQuestion(question: String): String {
        val service = accessibilityService ?: return "Erreur: Service d'accessibilité non activé"
        isRunning = true
        
        val currentProvider = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("current_provider", "default_provider") ?: "default_provider"
        val coords = calibrationManager.getCoordinates(currentProvider)
        
        val oldClipboard = ClipboardHelper.getFromClipboard(context)

        Log.d(TAG, "Démarrage automatisation pour profil: $currentProvider")

        // 1. Collage dans la barre de texte
        if (!isRunning) return "Annulé"
        ClipboardHelper.copyToClipboard(context, question)
        service.clickAt(coords.textFieldX, coords.textFieldY)
        delay(800)
        if (!isRunning) return "Annulé"
        service.pasteText()
        delay(500)

        // 2. Fermer le clavier (Bouton Back)
        if (!isRunning) return "Annulé"
        service.closeKeyboard()
        delay(1000)

        // 3. Appuyer sur Envoyer
        if (!isRunning) return "Annulé"
        service.clickAt(coords.sendButtonX, coords.sendButtonY)
        delay(coords.delayAfterSendMs) // Attente initiale pour la réponse

        // 4. Boucle de détection infinie (Swipe bas -> Attente -> Copie)
        var finalResult = ""
        var attempts = 0

        while (isRunning) {
            attempts++
            
            // On swipe vers le bas pour forcer l'affichage du dernier message et du bouton copier
            service.swipeUp() 
            delay(1000)
            if (!isRunning) break

            // On tente de cliquer sur le bouton copier
            service.clickAt(coords.copyButtonX, coords.copyButtonY)
            delay(1500)
            if (!isRunning) break

            val newClipboard = ClipboardHelper.getFromClipboard(context)
            if (newClipboard.isNotEmpty() && newClipboard != oldClipboard && newClipboard != question) {
                finalResult = newClipboard
                break
            }

            Log.d(TAG, "Tentative $attempts : pas encore de nouvelle réponse complète...")
            delay(2000) 
        }

        isRunning = false
        return if (finalResult.isNotEmpty()) finalResult else "Opération annulée ou arrêtée."
    }
}
