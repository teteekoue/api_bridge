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
        delay(600)

        // 2. Fermer le clavier via l'action système
        Log.d(TAG, "Step 2: Closing keyboard via system action...")
        service.closeKeyboard()
        delay(1000)

        // 3. Cliquer sur le bouton envoyer
        Log.d(TAG, "Step 3: Clicking send button...")
        service.resetEventTimer() // On remet le chrono à zéro juste avant
        service.clickAt(coords.sendButtonX, coords.sendButtonY)
        
        // 4. ATTENTE DE STABILITÉ (Détection de fin de génération)
        Log.d(TAG, "Step 4: Waiting for interface stability (end of generation)...")
        delay(2000) // On attend 2s que la génération démarre vraiment
        
        var isStable = false
        var attempts = 0
        val stabilityThreshold = 3000L // On veut 3 secondes de silence total
        val timeoutMax = 90000L // On n'attend pas plus de 90 secondes au total
        val startTime = System.currentTimeMillis()

        while (!isStable && (System.currentTimeMillis() - startTime) < timeoutMax) {
            val idleTime = service.getTimeSinceLastUpdate()
            
            if (idleTime >= stabilityThreshold) {
                Log.d(TAG, "Interface is stable for ${idleTime}ms. Generation likely finished.")
                isStable = true
            } else {
                // L'interface bouge encore, on attend un peu avant de re-vérifier
                delay(500)
                attempts++
                if (attempts % 10 == 0) Log.d(TAG, "Still detecting movement...")
            }
        }
        
        if (!isStable) Log.w(TAG, "Timed out waiting for stability, proceeding anyway.")
        delay(1000)

        // 5. Cliquer sur le bouton de bas pour descendre à l'extrême fin
        Log.d(TAG, "Step 5: Scrolling to bottom...")
        service.clickAt(coords.scrollDownButtonX, coords.scrollDownButtonY)
        delay(1200)

        // 6. Cliquer sur le bouton copier
        Log.d(TAG, "Step 6: Attempting to copy response...")
        var finalResult = ""
        for (i in 0..2) {
            service.clickAt(coords.copyButtonX, coords.copyButtonY)
            delay(1500) // Attente que le presse-papier se mette à jour
            val currentClipboard = ClipboardHelper.getFromClipboard(context)
            if (currentClipboard.isNotEmpty() && currentClipboard != oldClipboard) {
                finalResult = currentClipboard
                Log.d(TAG, "Successfully copied response.")
                break
            } else {
                Log.d(TAG, "Clipboard update not detected. Retry $i/2...")
            }
        }

        return if (finalResult.isEmpty()) {
            Log.e(TAG, "Failed to retrieve response.")
            "Erreur: Impossible de récupérer la réponse après génération."
        } else {
            Log.d(TAG, "Success. Returning response.")
            finalResult
        }
    }
}
