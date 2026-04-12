# IA-Local-Bridge 📱🤖

Transformez votre smartphone Android en une passerelle API physique pour n'importe quelle application d'IA mobile (DeepSeek, ChatGPT, Claude, Gemini, etc.).

## 🌟 Le Concept

De nombreuses IA puissantes disposent d'applications mobiles gratuites mais n'offrent pas d'API accessible ou gratuite. **IA-Local-Bridge** utilise les services d'accessibilité d'Android pour simuler des interactions humaines (clics, copier-coller) et exposer ces capacités via un serveur HTTP local (NanoHTTPD).

### Pourquoi l'utiliser ?
- **Gratuit** : Utilisez les modèles gratuits des apps mobiles comme une API.
- **Local** : Tout se passe sur votre appareil, pas de serveurs intermédiaires.
- **Compatible** : Fonctionne avec n'importe quelle application grâce au système de calibration manuel.

---

## 🚀 Fonctionnalités

- 🖱️ **Simulation de clics native** via `AccessibilityService`.
- 🎯 **Calibration visuelle** : Définissez les zones de clic directement sur l'écran.
- 🌐 **Serveur HTTP intégré** : Port 8080 par défaut.
- 🪟 **Fenêtre flottante** : Contrôlez l'API tout en restant sur l'application cible.
- ⛓️ **Automatisation complète** : Envoi de texte -> Attente -> Copie de la réponse.

---

## 🛠️ Installation & Configuration

### 1. Compiler l'APK
Ce projet est configuré avec **GitHub Actions**. 
1. Poussez le code sur GitHub.
2. Allez dans l'onglet **Actions**.
3. Téléchargez l'artefact `app-debug` une fois la compilation terminée.

### 2. Autorisations requises
Pour fonctionner, l'application a besoin de deux permissions critiques :
- **Service d'Accessibilité** : Pour simuler les clics et lire le presse-papier.
- **Superposition d'applications (Overlay)** : Pour afficher le bouton de contrôle et l'interface de calibration.

### 3. Calibration (Étape Cruciale)
Une fois l'application lancée :
1. Ouvrez l'application d'IA cible (ex: DeepSeek).
2. Cliquez sur **Calibrer** sur le bouton flottant.
3. Suivez les instructions :
   - Cliquez sur la **barre de saisie** de texte.
   - Cliquez sur le **bouton envoyer**.
   - Cliquez sur le **bouton copier** du dernier message généré.

---

## 📡 Utilisation de l'API

Une fois le serveur activé (Bouton **API: ON**), vous pouvez interroger votre téléphone depuis votre PC.

### Via ADB (Recommandé - USB)
```bash
# Rediriger le port du téléphone vers le PC
adb reverse tcp:8080 tcp:8080

# Envoyer une requête
curl "http://localhost:8080/ask?q=Raconte+une+blague"
```

### Via WiFi
```bash
# Remplacez l'IP par celle de votre téléphone
curl "http://192.168.1.15:8080/ask?q=Quelle+est+la+capitale+de+la+France"
```

---

## ⚠️ Limites & Précautions

- **Temps réel** : La latence dépend de la vitesse de génération de l'IA (environ 5-15 secondes).
- **Écran allumé** : Le téléphone doit rester allumé et sur l'application cible pendant le traitement.
- **Mises à jour** : Si l'interface de l'application d'IA change, une nouvelle calibration sera nécessaire.

---

## 🛠️ Structure Technique

- **Langage** : Kotlin 100%
- **Serveur** : [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd)
- **JSON** : Gson
- **Architecture** : Service-driven avec `AccessibilityService` et `ForegroundService`.

---

## 📝 Licence
Ce projet est destiné à un usage personnel et éducatif. Respectez les conditions d'utilisation des applications d'IA que vous interfacez.
