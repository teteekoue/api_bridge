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
        delay(1500)

        // 3. Cliquer sur le bouton envoyer
        Log.d(TAG, "Step 3: Clicking send button...")
        service.clickAt(coords.sendButtonX, coords.sendButtonY)
        
        // 4. BOUCLE DE SWIPE ET VÉRIFICATION (Toutes les 4 secondes)
        Log.d(TAG, "Step 4: Starting 4s Swipe Loop to detect end...")
        delay(4000) // Attente initiale
        
        var lastText = ""
        var isFinished = false
        val timeoutMax = 120000L
        val startTime = System.currentTimeMillis()
        
        val metrics = context.resources.displayMetrics
        val centerX = metrics.widthPixels / 2f
        val startY = metrics.heightPixels * 0.8f
        val endY = metrics.heightPixels * 0.2f

        while (!isFinished && (System.currentTimeMillis() - startTime) < timeoutMax) {
            // Swipe Long pour descendre
            service.performSwipe(centerX, startY, centerX, endY)
            delay(1200)

            // Comparer le dernier texte
            val currentLastText = service.getLastVisibleText()
            Log.d(TAG, "Last text detected: ${if(currentLastText.length > 30) currentLastText.take(30) + "..." else currentLastText}")

            if (currentLastText.isNotEmpty() && currentLastText == lastText) {
                Log.d(TAG, "Text is stable. Generation finished.")
                isFinished = true
            } else {
                lastText = currentLastText
                Log.d(TAG, "Still generating, waiting 4s...")
                delay(4000)
            }
        }
        
        delay(1000)

        // 5. COPIE INTELLIGENTE
        Log.d(TAG, "Step 5: Attempting to copy response...")
        var finalResult = ""
        
        // Tentative 1 : Coordonnées
        service.clickAt(coords.copyButtonX, coords.copyButtonY)
        delay(1500)
        finalResult = ClipboardHelper.getFromClipboard(context)
        
        // Tentative 2 : Morphologie (Recherche du bouton Copier)
        if (finalResult == oldClipboard || finalResult.isEmpty()) {
            Log.d(TAG, "Coordinate click failed. Searching morphologically...")
            val copyNode = service.findLastNodeByKeywords(listOf("Copier", "Copy", "Copy response", "Copier le message"))
            if (copyNode != null) {
                service.clickNode(copyNode)
                delay(1500)
                finalResult = ClipboardHelper.getFromClipboard(context)
            }
        }

        return if (finalResult.isEmpty() || finalResult == oldClipboard) {
            "Erreur: Impossible de copier la réponse finale. Le bouton Copier n'a pas été trouvé ou n'a pas réagi."
        } else {
            Log.d(TAG, "Success.")
            finalResult
        }
    }
}
