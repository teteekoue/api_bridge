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

        // 3. CAPTURE DE LA SIGNATURE DU BOUTON ENVOYER AU REPOS
        Log.d(TAG, "Step 3: Capturing Send Button signature at rest...")
        val restSignature = service.getNodeSignature(coords.sendButtonX, coords.sendButtonY)
        
        // 4. Cliquer sur le bouton envoyer
        Log.d(TAG, "Step 4: Clicking send button...")
        service.clickAt(coords.sendButtonX, coords.sendButtonY)
        
        // 5. ATTENTE DE FIN DE GÉNÉRATION (Retour à la signature de repos)
        Log.d(TAG, "Step 5: Waiting for generation to finish (Signature matching)...")
        delay(3000) // Laisser le temps à l'interface de passer en mode "Stop"
        
        var isFinished = false
        val timeoutMax = 90000L // 90 secondes max
        val startTime = System.currentTimeMillis()

        while (!isFinished && (System.currentTimeMillis() - startTime) < timeoutMax) {
            // Si on a pu capturer une signature au repos, on attend qu'elle revienne
            if (restSignature != null) {
                if (service.compareSignature(coords.sendButtonX, coords.sendButtonY, restSignature)) {
                    Log.d(TAG, "Signature matched! Send button is back. Generation finished.")
                    isFinished = true
                } else {
                    Log.d(TAG, "Signature mismatch (Still generating...).")
                    delay(800)
                }
            } else {
                // Secours : si on n'a pas pu capturer la signature, on attend 15s par défaut
                Log.w(TAG, "No rest signature captured, using fixed delay safety.")
                delay(15000)
                isFinished = true
            }
        }
        delay(1500)

        // 6. TRIPLE SWIPE pour forcer le bas
        Log.d(TAG, "Step 6: Performing Triple Swipe to bottom...")
        val metrics = context.resources.displayMetrics
        val centerX = metrics.widthPixels / 2f
        val startY = metrics.heightPixels * 0.7f
        val endY = metrics.heightPixels * 0.3f
        repeat(3) {
            service.performSwipe(centerX, startY, centerX, endY)
            delay(400)
        }
        delay(1000)

        // 7. COPIE INTELLIGENTE
        Log.d(TAG, "Step 7: Attempting to copy response...")
        var finalResult = ""
        service.clickAt(coords.copyButtonX, coords.copyButtonY)
        delay(1500)
        finalResult = ClipboardHelper.getFromClipboard(context)
        
        if (finalResult == oldClipboard || finalResult.isEmpty()) {
            Log.d(TAG, "Coordinate click failed. Searching morphologically...")
            val copyNode = service.findLastNodeByKeywords(listOf("Copier", "Copy", "Copy response"))
            if (copyNode != null) {
                service.clickNode(copyNode)
                delay(1500)
                finalResult = ClipboardHelper.getFromClipboard(context)
                copyNode.recycle()
            }
        }

        return if (finalResult.isEmpty() || finalResult == oldClipboard) {
            "Erreur: Impossible de récupérer la réponse finale."
        } else {
            Log.d(TAG, "Success.")
            finalResult
        }
    }
}
