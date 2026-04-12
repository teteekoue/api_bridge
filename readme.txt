Parfait. Voici la fiche technique ultra détaillée.

---

FICHE TECHNIQUE – API Bridge pour IA (Android)

1. IDENTITÉ DE L'APPLICATION

Élément Valeur
Nom provisoire IA-Local-Bridge
Type Service Android avec interface de calibration
Android minimum API 24 (Android 7.0 Nougat)
Android cible API 33 (Android 13)
Langage Kotlin (100% natif)
Architecture MVC avec service d'accessibilité

---

2. PRINCIPE GÉNÉRAL

```
┌─────────────────────────────────────────────────────────────────┐
│                         PC UTILISATEUR                          │
│                    curl http://192.168.1.15:8080/ask            │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                      TÉLÉPHONE ANDROID                          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              IA-Local-Bridge (ton app)                   │   │
│  │                                                          │   │
│  │  ┌────────────────────────────────────────────────┐     │   │
│  │  │         Serveur HTTP local (port 8080)         │     │   │
│  │  │     écoute les requêtes du PC                   │     │   │
│  │  └────────────────────────────────────────────────┘     │   │
│  │                         │                                │   │
│  │                         ▼                                │   │
│  │  ┌────────────────────────────────────────────────┐     │   │
│  │  │      Coordinateur (Accès aux clics)            │     │   │
│  │  │   reçoit la question → exécute la séquence     │     │   │
│  │  └────────────────────────────────────────────────┘     │   │
│  │                         │                                │   │
│  └─────────────────────────│────────────────────────────────┘   │
│                            │                                    │
│                            ▼                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │         FENÊTRE FLOTTANTE (mode picture-in-picture)     │   │
│  │                                                          │   │
│  │   ┌────────────────────────────────────────────────┐    │   │
│  │   │          APP FOURNISSEUR (DeepSeek)            │    │   │
│  │   │                                                │    │   │
│  │   │   [Zone de texte]                              │    │   │
│  │   │   [Bouton Envoyer]                             │    │   │
│  │   │   [Zone réponse]                               │    │   │
│  │   │   [Bouton Copier]                              │    │   │
│  │   │                                                │    │   │
│  │   └────────────────────────────────────────────────┘    │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

3. COMPOSANTS TECHNIQUES DÉTAILLÉS

3.1 Service d'accessibilité (AccessibilityService)

Rôle : Le seul moyen légal sur Android pour simuler des clics à des coordonnées précises.

Fichier : ClickAccessibilityService.kt

Méthodes principales :

Méthode Action
clickAt(x, y) Simule un clic simple aux coordonnées
longClickAt(x, y) Simule un clic long
pasteText(text) Colle du texte dans le champ focalisé
getClipboardText() Lit le contenu du presse-papier

Configuration requise dans AndroidManifest.xml :

```xml
<service android:name=".ClickAccessibilityService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
</service>
```

Activation par l'utilisateur : Paramètres → Accessibilité → IA-Local-Bridge → Activer

---

3.2 Serveur HTTP local

Bibliothèque : NanoHTTPD (léger, pas de dépendances lourdes)

Fichier : LocalApiServer.kt

Endpoints :

Endpoint Méthode Description
/ask?q=texte GET Envoie une question, reçoit la réponse
/ask POST (JSON) {"question": "texte"} → {"response": "..."}
/status GET Retourne l'état (ready/busy/error)
/calibrate POST Reçoit les coordonnées mises à jour

Port : 8080 (configurable par l'utilisateur)

Accès depuis le PC :

```bash
# Sur le même WiFi
curl http://192.168.1.15:8080/ask?q=Bonjour

# Via USB (ADB reverse)
adb reverse tcp:8080 tcp:8080
curl http://localhost:8080/ask?q=Bonjour
```

---

3.3 Fenêtre flottante

Type : TYPE_APPLICATION_OVERLAY (Android 10+)

Fichier : FloatingWindowService.kt

Fonctionnalités :

· Lance l'app fournisseur choisie
· Redimensionnable par l'utilisateur
· Bouton "Calibrer" pour enregistrer les positions
· Bouton "Démarrer API"
· Peut être masquée/réaffichée via notification

Permissions requises :

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

---

3.4 Système de calibration

Principe : L'utilisateur clique sur un bouton "Enregistrer cette position", puis clique à l'endroit souhaité sur l'écran.

Fichier : CalibrationManager.kt

Coordonnées stockées par fournisseur :

Nom Description Exemple (DeepSeek)
textFieldX/Y Zone de saisie de la question (540, 1800)
sendButtonX/Y Bouton Envoyer/Entrée (980, 1950)
copyButtonX/Y Bouton Copier la réponse (540, 2200)

Stockage : SharedPreferences en pourcentage de l'écran pour s'adapter à tous les téléphones.

Exemple de stockage :

```json
{
  "deepseek": {
    "textField": {"x": 0.5, "y": 0.75},
    "sendButton": {"x": 0.9, "y": 0.85},
    "copyButton": {"x": 0.5, "y": 0.92},
    "delayAfterSend": 7000
  }
}
```

---

4. SÉQUENCE D'EXÉCUTION D'UNE REQUÊTE

```
Étape 1 : PC envoie "Raconte une blague"
                │
                ▼
Étape 2 : Serveur HTTP reçoit et met la requête en file d'attente
                │
                ▼
Étape 3 : Vérifier que la fenêtre flottante est ouverte
                │
                ▼
Étape 4 : AccessibilityService.clickAt(textFieldX, textFieldY)
                │  → focus sur la zone de texte
                ▼
Étape 5 : AccessibilityService.pasteText("Raconte une blague")
                │
                ▼
Étape 6 : AccessibilityService.clickAt(sendButtonX, sendButtonY)
                │
                ▼
Étape 7 : Attendre délai configuré (ex: 7 secondes)
                │  (l'IA génère la réponse)
                ▼
Étape 8 : AccessibilityService.clickAt(copyButtonX, copyButtonY)
                │
                ▼
Étape 9 : Lire AccessibilityService.getClipboardText()
                │
                ▼
Étape 10 : Renvoyer le texte via HTTP au PC
                │
                ▼
Étape 11 : Passer à la requête suivante (file d'attente)
```

---

5. INTERFACE UTILISATEUR (ÉCRANS)

Écran 1 : Accueil

```
┌─────────────────────────────────┐
│  IA-Local-Bridge                │
├─────────────────────────────────┤
│                                 │
│  Fournisseurs disponibles :     │
│  ○ DeepSeek    (installé)       │
│  ○ ChatGPT     (non installé)   │
│  ○ Claude      (non installé)   │
│  ○ Gemini      (non installé)   │
│                                 │
│  [Ouvrir fenêtre flottante]     │
│  [Démarrer serveur API]         │
│                                 │
│  État : ⚫ Serveur arrêté        │
│  IP : 192.168.1.15              │
│  Port : 8080                    │
│                                 │
└─────────────────────────────────┘
```

Écran 2 : Calibration

```
┌─────────────────────────────────┐
│  Calibration - DeepSeek         │
├─────────────────────────────────┤
│                                 │
│  Étape 1 : Zone de texte        │
│  [Enregistrer position]         │
│  ✓ Enregistré (540, 1800)       │
│                                 │
│  Étape 2 : Bouton Envoyer       │
│  [Enregistrer position]         │
│  ⚠ Non enregistré               │
│                                 │
│  Étape 3 : Bouton Copier        │
│  [Enregistrer position]         │
│  ⚠ Non enregistré               │
│                                 │
│  Délai après envoi : [7] secondes│
│                                 │
│  [Tester la configuration]      │
│  [Sauvegarder]                  │
│                                 │
└─────────────────────────────────┘
```

Écran 3 : Configuration serveur

```
┌─────────────────────────────────┐
│  Configuration API              │
├─────────────────────────────────┤
│                                 │
│  Port : [8080]                  │
│                                 │
│  File d'attente :               │
│  ○ Désactivée                   │
│  ● Activée (max 5 requêtes)     │
│                                 │
│  Délai par défaut : [7] secondes│
│                                 │
│  [Redémarrer le serveur]        │
│                                 │
└─────────────────────────────────┘
```

---

6. GESTION DES ERREURS

Scénario Comportement
App fournisseur non ouverte L'ouvre automatiquement via Intent
App fournisseur crash Recommence 3 fois, puis erreur HTTP 503
Délai trop court (réponse incomplète) L'utilisateur augmente le délai, retente
Coordonnées invalides Erreur HTTP 400 + message "Recalibrer"
Service d'accessibilité désactivé Affiche une notification pour le réactiver
Concurrence (requête pendant génération) Met en file d'attente (ou refuse avec 429)

---

7. LIMITES CONNUES ET ACCEPTÉES

Limite Impact Acceptabilité
Latence 5-15 secondes par requête Pas de conversation en temps réel ✅ Accepté
Pas de multitâche téléphone Écran bloqué pendant génération ✅ Accepté
Fragilité face aux mises à jour des apps Recalibration nécessaire ✅ Accepté
Une seule requête à la fois File d'attente ✅ Accepté
Consommation batterie élevée Fenêtre flottante + service ⚠️ À surveiller

---

8. STRUCTURE DES FICHIERS (À GÉNÉRER)

```
app/src/main/java/com/ialocalbridge/
├── MainActivity.kt                 # Écran d'accueil
├── ClickAccessibilityService.kt    # Service de clics
├── LocalApiServer.kt               # Serveur HTTP
├── FloatingWindowService.kt        # Fenêtre flottante
├── CalibrationManager.kt           # Gestion des coordonnées
├── QueueManager.kt                 # File d'attente des requêtes
├── models/
│   ├── Provider.kt                 # Structure d'un fournisseur
│   └── Coordinates.kt              # Coordonnées X/Y
└── utils/
    ├── PermissionHelper.kt         # Gestion des permissions
    └── ClipboardHelper.kt          # Presse-papier

app/src/main/res/
├── layout/
│   ├── activity_main.xml
│   ├── floating_window.xml
│   └── calibration_screen.xml
├── values/
│   └── strings.xml
└── AndroidManifest.xml
```

---

9. DÉPENDANCES (build.gradle)

```gradle
dependencies {
    implementation 'org.jetbrains.kotlin:kotlin-stdlib:1.9.0'
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    
    // Serveur HTTP
    implementation 'org.nanohttpd:nanohttpd:2.3.1'
    
    // JSON
    implementation 'com.google.code.gson:gson:2.10.1'
    
    // Coroutines pour la file d'attente
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
}
```

---

