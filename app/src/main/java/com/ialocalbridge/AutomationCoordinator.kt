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
        delay(800)

        // 2. Fermer le clavier via l'action système (Touche Retour)
        Log.d(TAG, "Step 2: Closing keyboard via back action...")
        service.closeKeyboard()
        delay(1200)

        // 3. Cliquer sur le bouton envoyer
        Log.d(TAG, "Step 3: Clicking send button...")
        service.resetEventTimer()
        service.clickAt(coords.sendButtonX, coords.sendButtonY)
        
        // 4. ATTENTE DE STABILITÉ (Fin de génération)
        Log.d(TAG, "Step 4: Waiting for stability (3s silence)...")
        delay(2000) // Attente démarrage
        
        var isStable = false
        val stabilityThreshold = 3500L // Augmenté pour plus de sûreté
        val timeoutMax = 120000L
        val startTime = System.currentTimeMillis()

        while (!isStable && (System.currentTimeMillis() - startTime) < timeoutMax) {
            val idleTime = service.getTimeSinceLastUpdate()
            if (idleTime >= stabilityThreshold) {
                isStable = true
                Log.d(TAG, "Interface stable.")
            } else {
                delay(500)
            }
        }
        delay(1000)

        // 5. TRIPLE SWIPE pour forcer le bas (Évite les bordures système)
        Log.d(TAG, "Step 5: Performing Triple Swipe to bottom...")
        val metrics = context.resources.displayMetrics
        val centerX = metrics.widthPixels / 2f
        val startY = metrics.heightPixels * 0.7f // Part du bas (au dessus des boutons système)
        val endY = metrics.heightPixels * 0.3f   // Vers le haut
        
        repeat(3) {
            service.performSwipe(centerX, startY, centerX, endY)
            delay(400)
        }
        delay(1000)

        // 6. COPIE INTELLIGENTE
        Log.d(TAG, "Step 6: Attempting to copy response...")
        var finalResult = ""
        
        // Tentative 1 : Coordonnées calibrées
        service.clickAt(coords.copyButtonX, coords.copyButtonY)
        delay(1500)
        finalResult = ClipboardHelper.getFromClipboard(context)
        
        // Tentative 2 : Recherche morphologique si le presse-papier n'a pas changé
        if (finalResult == oldClipboard || finalResult.isEmpty()) {
            Log.d(TAG, "Coordinate click failed. Searching for copy node morphologically...")
            val copyNode = service.findLastNodeByKeywords(listOf("Copier", "Copy", "Copy response"))
            if (copyNode != null) {
                service.clickNode(copyNode)
                delay(1500)
                finalResult = ClipboardHelper.getFromClipboard(context)
                copyNode.recycle()
            }
        }

        return if (finalResult.isEmpty() || finalResult == oldClipboard) {
            "Erreur: Impossible de récupérer la réponse. Vérifiez la calibration du bouton Copier."
        } else {
            Log.d(TAG, "Success.")
            finalResult
        }
    }
}
