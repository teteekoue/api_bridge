package com.ialocalbridge

import android.content.Context
import android.util.Log
import com.ialocalbridge.utils.ClipboardHelper
import kotlinx.coroutines.delay

class AutomationCoordinator(private val context: Context) {

    private val accessibilityService get() = ClickAccessibilityService.instance
    private val calibrationManager = CalibrationManager(context)
    private val TAG = "AutomationCoordinator"

    suspend fun processQuestion(question: String): String {
        val service = accessibilityService ?: return "Erreur: Service d'accessibilité non activé"
        val coords = calibrationManager.getCoordinates("default_provider")
        val oldClipboard = ClipboardHelper.getFromClipboard(context)

        // 1. Cliquer sur la barre de message et coller le texte
        Log.d(TAG, "Step 1: Clicking message bar and pasting text...")
        service.clickAt(coords.textFieldX, coords.textFieldY)
        delay(600)
        service.pasteText(question)
        delay(800) // Laisser le temps au clavier de s'ouvrir complètement

        // 2. Fermer le clavier via l'action système (Touche Retour)
        Log.d(TAG, "Step 2: Closing keyboard via back action...")
        service.closeKeyboard()
        delay(1200) // DÉLAI DE 1S (plus un peu de marge) pour que l'interface se replace

        // 3. Cliquer sur le bouton envoyer
        Log.d(TAG, "Step 3: Clicking send button...")
        service.resetEventTimer() // On remet le chrono à zéro juste avant
        service.clickAt(coords.sendButtonX, coords.sendButtonY)
        
        // 4. ATTENTE DE STABILITÉ (Détection de fin de génération via flux de données)
        Log.d(TAG, "Step 4: Waiting for data flow stability (silence)...")
        delay(2000) // On attend que la génération démarre vraiment
        
        var isStable = false
        val stabilityThreshold = 3000L // 3 secondes de silence = fin de réponse
        val timeoutMax = 120000L // 2 minutes max
        val startTime = System.currentTimeMillis()

        while (!isStable && (System.currentTimeMillis() - startTime) < timeoutMax) {
            val idleTime = service.getTimeSinceLastUpdate()
            if (idleTime >= stabilityThreshold) {
                Log.d(TAG, "Interface is stable for ${idleTime}ms. Generation finished.")
                isStable = true
            } else {
                delay(500)
            }
        }
        delay(1000)

        // 5. Cliquer sur le bouton de bas pour descendre à l'extrême fin
        Log.d(TAG, "Step 5: Scrolling to extreme bottom...")
        service.clickAt(coords.scrollDownButtonX, coords.scrollDownButtonY)
        delay(1000)

        // 6. Cliquer sur le bouton copier
        Log.d(TAG, "Step 6: Clicking copy button...")
        var finalResult = ""
        for (i in 0..2) {
            service.clickAt(coords.copyButtonX, coords.copyButtonY)
            delay(1500) // Attente de mise à jour du presse-papier
            val currentClipboard = ClipboardHelper.getFromClipboard(context)
            if (currentClipboard.isNotEmpty() && currentClipboard != oldClipboard) {
                finalResult = currentClipboard
                Log.d(TAG, "Successfully copied and retrieved response.")
                break
            } else {
                Log.d(TAG, "Clipboard update retry $i/2...")
            }
        }

        return if (finalResult.isEmpty()) {
            "Erreur: Impossible de copier la réponse finale. Le presse-papier est resté inchangé."
        } else {
            finalResult
        }
    }
}
