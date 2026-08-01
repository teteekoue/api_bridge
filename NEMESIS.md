# NEMESIS - Documentation du projet NEMAPI Bridge

## Objectif

Transformer un smartphone Android en **passerelle API HTTP compatible OpenAI** pour exploiter gratuitement les applications mobiles d'IA (DeepSeek, ChatGPT, Claude, Gemini...) via le service d'accessibilite Android.

### Probleme resolu
Les applications mobiles d'IA offrent un acces gratuit a des modeles puissants, mais sans API publique. NEMAPI Bridge cree un pont entre ces applications et les outils de developpement (Qwen Code, Cursor, SDK OpenAI, etc.) en simulant des interactions tactiles humaines.

---

## Architecture

```
Poste de travail (curl, Qwen Code, Cursor, SDK OpenAI)
    |
    | HTTP (WiFi / USB ADB reverse)
    v
NEMAPI Bridge (:8080) - Android
    |
    +-- LocalApiServer.kt        Serveur HTTP (NanoHTTPD)
    |   +-- /v1/chat/completions  Endpoint OpenAI
    |   +-- /v1/models            Liste des modeles
    |   +-- /stats                Metriques live
    |   +-- /config               Configuration (toggle stream)
    |   +-- /ask, /stop           Endpoints legacy
    |
    +-- AutomationCoordinator.kt  Orchestration des interactions
    |   +-- processQuestion()     Cycle: coller -> clic envoi -> polling reponse
    |   +-- stopGeneration()      Arreter la generation en cours
    |
    +-- ClickAccessibilityService.kt  Service d'accessibilite Android
    |   +-- clickAt()             Clic a des coordonnees specifiques
    |   +-- swipeUp()             Defilement vers le bas
    |   +-- pasteText()           Coller dans le champ de saisie
    |   +-- closeKeyboard()       Fermer le clavier virtuel
    |
    +-- ToolParser.kt             Parsing de tool calls (3 niveaux)
    |   +-- Niveau 1: XML  (<tool_calls>/<invoke>)
    |   +-- Niveau 2: JSON ([{"name":...,"arguments":...}])
    |   +-- Niveau 3: Reparation JSON automatique
    |
    +-- WebInterface.kt           Interface web (panneau admin)
    |   +-- Dashboard             Metriques live
    |   +-- Models                Liste des modeles
    |   +-- Config                Toggle stream, connexion
    |   +-- Logs                  Historique des requetes
    |
    +-- CalibrationManager.kt     Gestion des coordonnees de calibration
    +-- FloatingWindowService.kt  Fenetre flottante (overlay)
    +-- CalibrationOverlayManager.kt  Interface de calibration visuelle
    |
    +-- Utilitaires
        +-- ClipboardHelper.kt    Gestion du presse-papier
        +-- FileUploader.kt       Upload vers 6 hebergeurs
        +-- FileMessageBuilder.kt Construction de messages avec fichiers
        +-- NetworkHelper.kt      Detection d'adresse IP
        +-- PermissionHelper.kt   Gestion des permissions Android
```

---

## Stack technique

| Couche | Technologie |
|---|---|
| **Langage** | Kotlin |
| **Build** | Gradle (AGP 8.x, compileSdk 33) |
| **Serveur HTTP** | NanoHTTPD 2.3.1 |
| **JSON** | org.json (Android SDK) + Gson 2.10.1 |
| **HTTP Client** | OkHttp 4.11.0 |
| **Coroutines** | kotlinx-coroutines-android 1.7.3 |
| **Accessibilite** | AccessibilityService (Android SDK) |
| **UI Android** | AppCompat + Material Design + ViewBinding |
| **UI Web** | HTML/CSS vanilla (inspire shadcn/ui + Tailwind) |
| **CI/CD** | GitHub Actions (build APK automatique) |
| **API exposee** | OpenAI Compatible (/v1/chat/completions, /v1/models) |

---

## Flux d'une requete

```
1. Client envoie POST /v1/chat/completions avec {"messages":[...]}
2. LocalApiServer extrait le prompt du dernier message
3. Si tools presents -> ToolParser.buildToolCallPrompt() ajoute les consignes
4. executeBlocking() lance AutomationCoordinator.processQuestion()
5. AutomationCoordinator:
   a. Copie le prompt dans le presse-papier
   b. Clique sur la zone de texte
   c. Colle le texte (ACTION_PASTE)
   d. Ferme le clavier
   e. Clique sur le bouton Envoyer
   f. Boucle toutes les 1s: swipe bas -> clic copie -> verifie presse-papier
   g. Detecte un changement -> reponse complete recuperee
6. ToolParser.parseToolCalls() analyse la reponse pour extraire les tool_calls
7. Si stream=true: decoupage en chunks SSE avec 40ms de delai
8. Si stream=false: reponse JSON OpenAI complete
```

---

## Etat d'avancement (v2.0.1)

### Fait
- [x] Serveur HTTP NanoHTTPD sur port 8080
- [x] Endpoint `/v1/chat/completions` compatible OpenAI
- [x] Endpoint `/v1/models`
- [x] Streaming SSE simule (chunks 15 caracteres, delai 40ms)
- [x] Tool Calling 3 niveaux (XML, JSON, Invoke + reparation JSON)
- [x] Toggle stream ON/OFF depuis l'interface web (`POST /config`)
- [x] Dashboard live avec metriques (uptime, requetes, tokens prompt/completion)
- [x] Interface web design sombre (inspire shadcn/ui)
- [x] Endpoints `/stats`, `/status`, `/ask`, `/stop`
- [x] Upload fichiers vers 6 hebergeurs avec fallback
- [x] Calibration multi-profils
- [x] Fenetre flottante (overlay) pour controle rapide
- [x] Delais optimises (clic ~500ms, swipe ~1s, copie ~1s)
- [x] GitHub Actions: build APK automatique a chaque push
- [x] Compatible Qwen Code, Cursor, Continue.dev, SDK OpenAI
- [x] Timeout 180 secondes pour les longues generations

### A faire (roadmap)
- [ ] Authentification par token API configurable
- [ ] Support multi-modeles (plusieurs apps IA simultanees)
- [ ] Compatibilite API Anthropic (`/anthropic/v1/messages`)
- [ ] Persistance de la configuration (SharedPreferences pour le toggle stream)
- [ ] Gestion des erreurs amelioree (retry automatique sur echec de copie)
- [ ] Mode USB (ADB reverse) avec detection automatique
- [ ] Compression des reponses HTTP (gzip)
- [ ] Tests automatises (scenarios de tool calling)

---

## Installation et deploiement

### Build local
```bash
git clone https://github.com/teteekoue/api_bridge.git
cd api_bridge
./gradlew assembleDebug
# APK dans app/build/outputs/apk/debug/app-debug.apk
```

### Installation Android
```bash
adb install app-debug.apk
```

### Permissions a activer manuellement
1. Parametres -> Accessibilite -> NEMAPI Bridge -> Activer
2. Parametres -> Applications -> NEMAPI Bridge -> Autoriser la superposition

### Utilisation
- **Panneau admin**: `http://<IP>:8080`
- **API OpenAI**: `http://<IP>:8080/v1`
- **Modele**: `deepseek-chat`
- **API Key**: non verifiee (mettre n'importe quelle valeur)

---

## Licence

MIT

---

*Projet documente par NEMESIS Agent - Aout 2026*
