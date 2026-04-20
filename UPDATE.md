# Mise à jour : Version Asynchrone v3.0 (Polling & Brute-Force)

Cette version résout définitivement les problèmes de déconnexion ("Aborted") sur les messages longs et simplifie la détection de fin de génération.

## 🚀 Nouvelles Fonctionnalités & Améliorations

### 1. Architecture Asynchrone (Polling)
*   **Fin des Timeouts HTTP** : Le serveur ne maintient plus de connexion ouverte pendant la génération.
*   **Point `/ask`** : Déclenche l'automatisation en arrière-plan et retourne immédiatement un `jobId` unique.
*   **Point `/result?id=...`** : Permet au client de venir chercher le résultat quand il le souhaite. Répond `STILL_WORKING` tant que l'IA n'a pas fini.
*   **Patience Illimitée** : Le système supporte désormais des sessions allant jusqu'à **3 heures**.

### 2. Stratégie de Détection "Click & Verify"
*   **Approche Brute-Force** : Au lieu d'analyser l'état technique des boutons (souvent trompeur), l'application tente de copier la réponse toutes les 3 secondes.
*   **Cycle Mécanique** : Swipe vers le bas -> Clic aveugle aux coordonnées calibrées -> Lecture du presse-papier.
*   **Preuve par le Résultat** : La génération est considérée terminée **uniquement** quand le contenu du presse-papier change. C'est la méthode la plus fiable contre les interfaces IA instables.

### 3. Dashboard Web Intelligent
*   **Automated Polling** : Le tableau de bord web intègre désormais une logique JavaScript qui gère seule la boucle `/ask` -> `/result`.
*   **Suivi en Temps Réel** : Affiche le statut "L'IA travaille toujours..." avec le compteur de tentatives.

### 4. Stabilité Système
*   **Main Looper Garanti** : L'automation tourne dans des coroutines gérées par le handler principal, assurant qu'aucun swipe ou clic n'est ignoré.
*   **Délai de Swipe Réduit** : Passage à 3 secondes pour plus de réactivité sans sacrifier la stabilité.

## 🛠️ Nouveau Schéma de Fonctionnement
1. **Requête Client** : Appelle `/ask?q=...`.
2. **Réponse Serveur** : Retourne immédiatement un UUID (Job ID).
3. **Travail Background** :
   - Injection de la question.
   - Boucle (3s) : Swipe -> Clic Copier -> Vérif Presse-papier.
4. **Polling Client** : Le client appelle `/result` toutes les 3-5s.
5. **Livraison** : Dès que le presse-papier a changé, le résultat est stocké et livré au prochain appel du client.
