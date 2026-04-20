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
        
        // 2. Détection de fin (Mixte : Empreinte globale + Détection directe du bouton)
        delay(4000)
        var lastScreenFingerprint = ""
        var isFinished = false
        var stabilityCounter = 0
        val requiredStability = 3
        val timeoutMax = 600000L // Augmenté à 10 minutes pour les générations très longues
        val startTime = System.currentTimeMillis()
        val metrics = context.resources.displayMetrics
        val centerX = metrics.widthPixels / 2f
        val startY = metrics.heightPixels * 0.8f
        val endY = metrics.heightPixels * 0.2f

        while (!isFinished && (System.currentTimeMillis() - startTime) < timeoutMax) {
            service.performSwipe(centerX, startY, centerX, endY)
            delay(1500) // Attente stabilisation UI après swipe
            
            // MÉTHODE A : Détection directe du bouton Copier aux coordonnées
            val isCopyButtonVisible = service.isNodeAtMatchingSignature(
                coords.copyButtonX, 
                coords.copyButtonY, 
                coords.copyButtonResourceId, 
                coords.copyButtonClassName
            )
            
            if (isCopyButtonVisible) {
                Log.d(TAG, "Succès : Bouton Copier détecté aux coordonnées ! Fin de génération.")
                isFinished = true
                continue // On sort de la boucle immédiatement
            }

            // MÉTHODE B : Empreinte globale de l'écran (Stabilité totale)
            val currentFingerprint = service.getAllVisibleText()
            
            if (currentFingerprint.isNotEmpty() && currentFingerprint == lastScreenFingerprint) {
                stabilityCounter++
                Log.d(TAG, "Stabilité Écran: $stabilityCounter/$requiredStability (Pas de changement détecté)")
                if (stabilityCounter >= requiredStability) {
                    Log.d(TAG, "Génération terminée par stabilité globale.")
                    isFinished = true
                } else {
                    delay(4000)
                }
            } else {
                lastScreenFingerprint = currentFingerprint
                stabilityCounter = 0
                Log.d(TAG, "L'écran bouge encore (IA en cours ou défilement...)")
                delay(4000)
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
