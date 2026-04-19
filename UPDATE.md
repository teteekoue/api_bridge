# Mise à jour : Version Finale Stable (v2.0)

Cette version transforme le smartphone en une passerelle API robuste pour les IA mobiles grâce à une approche mécanique et morphologique.

## 🚀 Nouvelles Fonctionnalités & Améliorations

### 1. Système de Calibration Intelligent
*   **Calibration à 3 points uniquement** : Barre de texte, Bouton Envoyer, Bouton Copier.
*   **Mode Défilement** : Pendant la calibration, un bouton permet de rendre l'overlay transparent pour manipuler l'application d'IA (ouvrir le clavier, défiler l'historique) avant de marquer les points.
*   **Mémoire Morphologique** : Lors du clic sur le bouton "Copier", l'application enregistre son ADN technique (Resource ID, Classe Android, Description).

### 2. Détection de Fin de Génération "Physique"
*   **Boucle de Stabilité (4s)** : L'application effectue un balayage (Swipe) long toutes les 4 secondes et compare le dernier texte visible en bas à droite de l'écran.
*   **Verdict Mathématique** : Si le texte reste identique après un swipe, la génération est considérée comme terminée. Cela évite les erreurs liées aux signaux système bloqués.

### 3. Capture de Réponse Haute Fiabilité
*   **Swipe Ultime** : Une fois la fin détectée, un balayage final aligne parfaitement le dernier message.
*   **Recherche Morphologique** : Si le clic aux coordonnées échoue, l'application utilise l'ADN enregistré pour retrouver le bouton Copier n'importe où sur l'écran.
*   **Triple Retry avec "Secousse"** : En cas d'échec de copie, l'application fait un nouveau swipe pour forcer l'interface à rafraîchir ses boutons et réessaye jusqu'à 3 fois.

### 4. Serveur API Optimisé
*   **Exécution sur Main Looper** : Toutes les actions d'automation sont forcées sur le thread principal d'Android, éliminant les erreurs de threading.
*   **CORS & POST** : Support complet des requêtes cross-origin et parsing robuste du corps des messages.

## 🛠️ Schéma de Fonctionnement
1. **Réception** : Le serveur reçoit la question via HTTP POST.
2. **Injection** : Clic barre -> Collage -> Touche Retour (ferme le clavier) -> Attente 1.2s -> Clic Envoyer.
3. **Attente** : Boucle de Swipes longs (4s) jusqu'à stabilité du texte final.
4. **Extraction** : Triple tentative de copie (Coordonnées + Morphologie) avec swipes de sécurité.
5. **Réponse** : Le contenu du presse-papier est renvoyé à l'utilisateur.
