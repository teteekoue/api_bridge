package com.ialocalbridge

import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.ialocalbridge.utils.ClipboardHelper
import kotlinx.coroutines.delay

class AutomationCoordinator(private val context: Context) { // CalibrationManager is no longer needed

    private val accessibilityService get() = ClickAccessibilityService.instance
    private val TAG = "AutomationCoordinator"

    suspend fun processQuestion(question: String): String {
        val service = accessibilityService ?: return "Erreur: Service d'accessibilité non activé"
        val oldClipboard = ClipboardHelper.getFromClipboard(context)

        // 1. Trouver le champ de texte et y injecter la question.
        Log.d(TAG, "Finding editable node...")
        val textFieldNode = service.findEditableNode()
        if (textFieldNode == null) {
            Log.e(TAG, "Editable node not found. Cannot paste text.")
            return "Erreur: Champ de texte non trouvé sur l'écran."
        }
        
        // Focus on the editable node explicitly
        if (!textFieldNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) {
             Log.w(TAG, "Failed to focus editable node. Proceeding with paste anyway.")
        }
        delay(200)

        Log.d(TAG, "Pasting text...")
        if (!service.pasteText(question)) {
            Log.e(TAG, "Failed to paste text directly.")
            // If direct paste fails, report error. No fallback to coords anymore.
            return "Erreur: Échec de l'injection de texte dans le champ."
        }
        delay(400)

        // 2. Fermeture préventive immédiate du clavier.
        service.closeKeyboard()
        delay(500)

        // 3. Trouver et cliquer sur le bouton Envoyer.
        Log.d(TAG, "Finding send button...")
        val sendButtonNode = service.findSendButtonNode()
        if (sendButtonNode == null) {
            Log.e(TAG, "Send button not found.")
            return "Erreur: Bouton d'envoi non trouvé sur l'écran."
        }
        
        Log.d(TAG, "Clicking send button...")
        service.clickNode(sendButtonNode)
        sendButtonNode.recycle() // Recycle node after click
        delay(1000) // Wait for generation to start

        // 4. BOUCLE D'ATTENTE INTELLIGENTE (Attente de la fin de génération)
        Log.d(TAG, "Waiting for generation to finish...")
        var isGenerating = true
        var attempts = 0
        while (isGenerating && attempts < 100) { // Timeout max ~30 seconds
            if (service.isGenerationActive()) {
                Log.d(TAG, "Generation active, waiting...")
                delay(300)
                attempts++
            } else {
                isGenerating = false
            }
        }
        if (attempts >= 100) {
            Log.w(TAG, "Timeout waiting for generation to finish.")
        } else {
            Log.d(TAG, "Generation finished or not detected.")
        }

        // 5. Fermeture supplémentaire du clavier.
        service.closeKeyboard()
        delay(300)

        // 6. DÉFILEMENT EXTRÊME.
        Log.d(TAG, "Forcing scroll to bottom...")
        service.forceScrollToBottom()
        delay(1000)

        // 7. BOUCLE DE COPIE (Vérification que le presse-papier a bien changé).
        Log.d(TAG, "Finding copy button and attempting to copy...")
        var finalResult = ""
        for (i in 0..3) {
            val copyButtonNode = service.findCopyButtonNode()
            if (copyButtonNode != null) {
                service.clickNode(copyButtonNode)
                delay(1000) // Wait for clipboard update
                val currentClipboard = ClipboardHelper.getFromClipboard(context)
                if (currentClipboard.isNotEmpty() && currentClipboard != oldClipboard) {
                    finalResult = currentClipboard
                    Log.d(TAG, "Successfully copied response.")
                    break
                } else {
                    Log.d(TAG, "Clipboard content not updated or empty. Retry copy ($i). Current: '$currentClipboard', Old: '$oldClipboard'")
                }
                copyButtonNode.recycle()
            } else {
                Log.e(TAG, "Copy button not found for click attempt ($i).")
            }
        }

        return if (finalResult.isEmpty()) {
            Log.e(TAG, "Failed to retrieve response from clipboard.")
            "Erreur: Impossible de récupérer la réponse. La copie a échoué ou l'IA n'a rien renvoyé."
        } else {
            Log.d(TAG, "Returning final result.")
            finalResult
        }
    }
}
