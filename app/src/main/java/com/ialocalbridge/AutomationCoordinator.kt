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

        // 1. Saisie et Envoi
        service.clickAt(coords.textFieldX, coords.textFieldY)
        delay(600)
        service.pasteText(question)
        delay(800)
        service.closeKeyboard()
        delay(1500)
        service.clickAt(coords.sendButtonX, coords.sendButtonY)
        
        // 2. Détection de fin (Stratégie : Clic Aveugle + Vérification Presse-papier)
        delay(3000)
        var isFinished = false
        val timeoutMax = 10800000L // 3 heures
        val startTime = System.currentTimeMillis()
        val metrics = context.resources.displayMetrics
        val centerX = metrics.widthPixels / 2f
        val startY = metrics.heightPixels * 0.8f
        val endY = metrics.heightPixels * 0.2f

        Log.d(TAG, "Démarrage de la boucle de détection (Timeout: 3h, Cycle: 3s)")

        while (!isFinished && (System.currentTimeMillis() - startTime) < timeoutMax) {
            // A. On force le défilement vers le bas
            service.performSwipe(centerX, startY, centerX, endY)
            delay(1000) // Attente stabilisation après swipe
            
            // B. On tente le clic aux coordonnées du bouton Copier (même s'il semble absent)
            Log.d(TAG, "Tentative de clic au bouton Copier aux coordonnées (${coords.copyButtonX}, ${coords.copyButtonY})")
            service.clickAt(coords.copyButtonX, coords.copyButtonY)
            
            // C. On attend un peu que le système traite la copie
            delay(1000)
            
            // D. VERIFICATION ULTIME : Est-ce que le presse-papier a changé ?
            val currentClipboard = ClipboardHelper.getFromClipboard(context)
            if (currentClipboard.isNotEmpty() && currentClipboard != oldClipboard) {
                Log.d(TAG, "SUCCÈS : Le presse-papier a changé ! Génération terminée.")
                isFinished = true
            } else {
                Log.d(TAG, "Rien dans le presse-papier ou contenu identique. Nouvelle tentative dans 1s...")
                delay(1000) // Pour compléter le cycle de 3s environ
            }
        }
        
        // 3. SWIPE ULTIME DE SÉCURITÉ (Pour aligner le bouton Copier)
        Log.d(TAG, "Performing Final Security Swipe...")
        service.performSwipe(centerX, startY, centerX, endY)
        delay(1500)

        // 4. COPIE PERFECTIONNÉE (Coordonnées + Morphologie + Retry)
        var finalResult = ""
        for (attempt in 0..2) {
            Log.d(TAG, "Copy attempt #$attempt...")
            
            // A. Essayer le clic aux coordonnées calibrées
            service.clickAt(coords.copyButtonX, coords.copyButtonY)
            delay(1500)
            finalResult = ClipboardHelper.getFromClipboard(context)
            
            // B. Si échec, essayer la recherche morphologique (par ID/Classe/Desc enregistrés)
            if (finalResult == oldClipboard || finalResult.isEmpty()) {
                Log.d(TAG, "Coordinate click failed. Searching by morphology...")
                val copyNode = service.findNodeByMorphology(
                    coords.copyButtonResourceId,
                    coords.copyButtonClassName,
                    coords.copyButtonDescription
                )
                if (copyNode != null) {
                    service.clickNode(copyNode)
                    delay(1500)
                    finalResult = ClipboardHelper.getFromClipboard(context)
                }
            }
            
            // C. Si succès, on arrête là
            if (finalResult.isNotEmpty() && finalResult != oldClipboard) {
                Log.d(TAG, "Success copying response.")
                break
            }
            
            // D. Si toujours échec, on refait un swipe pour débloquer l'interface
            if (attempt < 2) {
                Log.w(TAG, "Copy failed, retrying with extra swipe...")
                service.performSwipe(centerX, startY, centerX, endY)
                delay(1500)
            }
        }

        return if (finalResult.isEmpty() || finalResult == oldClipboard) {
            "Erreur: La copie a échoué après plusieurs tentatives morphologiques."
        } else {
            finalResult
        }
    }
}
