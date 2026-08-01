# NEMAPI Bridge v2.0

Transformez votre smartphone Android en une **API HTTP compatible OpenAI** pour n'importe quelle application d'IA mobile (DeepSeek, ChatGPT, Claude, Gemini...).

---

## Qu'est-ce que NEMAPI Bridge ?

De nombreuses IA puissantes proposent des applications mobiles **gratuites** mais n'offrent pas d'API publique ou gratuite. NEMAPI Bridge utilise le **service d'accessibilité Android** pour simuler des interactions humaines (clics, copier-coller, swipes) et expose ces capacités via une API HTTP **compatible OpenAI** avec **streaming SSE** simulé.

### Pourquoi l'utiliser ?
- **100% gratuit** — Exploite les apps mobiles gratuites comme une API
- **100% local** — Tout s'exécute sur votre appareil, aucun serveur tiers
- **Compatible OpenAI** — Fonctionne avec Qwen Code, Cursor, Continue.dev, CLI OpenAI, et tout client compatible `/v1/chat/completions`
- **Multi-app** — Fonctionne avec n'importe quelle application d'IA via le système de calibration

---

## Fonctionnalités

| Fonctionnalité | Description |
|---|---|
| **API OpenAI Complète** | Endpoints `/v1/chat/completions` et `/v1/models` |
| **Streaming SSE** | Streaming simulé pour compatibilité avec tous les outils modernes |
| **Tool Calling** | Support complet avec parsing 3 niveaux (XML, JSON, Invoke) et réparation JSON automatique |
| **Panneau Admin Web** | Dashboard live avec métriques (tokens, requêtes, uptime), logs, configuration |
| **Toggle Stream** | Activation/désactivation du streaming en un clic depuis l'interface web |
| **Upload de fichiers** | 6 hébergeurs en fallback (Catbox, Tmp.ninja, 0x0.st, Pomf.cat, File.io, Transfer.sh) |
| **Fenêtre flottante** | Contrôle rapide sans quitter l'application cible |
| **Calibration visuelle** | Définissez les zones de clic directement sur l'écran |
| **Multi-profils** | Sauvegardez plusieurs configurations (DeepSeek, ChatGPT, Claude...) |

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│  Votre PC / Outil de code                           │
│  (Qwen Code, Cursor, curl, SDK OpenAI...)           │
└──────────────┬──────────────────────────────────────┘
               │ HTTP (WiFi/USB)
               ▼
┌─────────────────────────────────────────────────────┐
│  NEMAPI Bridge (Android)                            │
│  ┌───────────────────────────────────────────────┐  │
│  │  NanoHTTPD :8080                              │  │
│  │  /v1/chat/completions  /v1/models  /stats     │  │
│  │  /ask  /result  /config  /status              │  │
│  └────────────┬──────────────────────────────────┘  │
│               │                                      │
│  ┌────────────▼──────────────────────────────────┐  │
│  │  AutomationCoordinator                        │  │
│  │  → ClickAccessibilityService (clics/swipes)   │  │
│  │  → ClipboardHelper (copier/coller)            │  │
│  │  → ToolParser (tool calling 3 niveaux)        │  │
│  └────────────┬──────────────────────────────────┘  │
│               │                                      │
│  ┌────────────▼──────────────────────────────────┐  │
│  │  Application IA (DeepSeek, ChatGPT, etc.)     │  │
│  │  ← Interactions simulées automatiquement      │  │
│  └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

---

## Installation

### 1. Télécharger l'APK

Téléchargez le dernier APK depuis les [Releases](https://github.com/teteekoue/api_bridge/releases) (section Artifacts du workflow).

### 2. Installer sur Android

```bash
adb install app-debug.apk
```

Ou transférez l'APK sur votre téléphone et installez-le manuellement.

### 3. Autorisations requises

| Permission | Chemin |
|---|---|
| **Service d'Accessibilité** | Paramètres → Accessibilité → NEMAPI Bridge → Activer |
| **Superposition (Overlay)** | Paramètres → Applications → NEMAPI Bridge → Autoriser la superposition |

### 4. Calibrer

1. Ouvrez l'application d'IA cible (ex: DeepSeek)
2. Appuyez sur le **bouton flottant** NEMAPI
3. Sélectionnez **Calibrer**
4. Suivez les instructions pour pointer :
   - Zone de saisie de texte
   - Bouton Envoyer
   - Bouton Copier la réponse
5. Nommez le profil et sauvegardez

---

## Utilisation

### Accéder au panneau d'administration

```
http://<IP_DU_TELEPHONE>:8080
```

### Lister les modèles

```bash
curl http://192.168.1.65:8080/v1/models
```

### Chat Completion (non-streamé)

```bash
curl -X POST http://192.168.1.65:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "deepseek-chat",
    "messages": [{"role": "user", "content": "Quelle est la capitale du Togo?"}],
    "stream": false
  }'
```

Réponse :
```json
{
  "id": "chatcmpl-a1b2c3d4",
  "object": "chat.completion",
  "model": "deepseek-chat",
  "choices": [{
    "index": 0,
    "message": {
      "role": "assistant",
      "content": "La capitale du Togo est Lomé."
    },
    "finish_reason": "stop"
  }],
  "usage": {
    "prompt_tokens": 5,
    "completion_tokens": 8,
    "total_tokens": 13
  }
}
```

### Chat Completion (streamé SSE)

```bash
curl -X POST http://192.168.1.65:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "deepseek-chat",
    "messages": [{"role": "user", "content": "Explique-moi le RSA"}],
    "stream": true
  }'
```

### Tool Calling

```bash
curl -X POST http://192.168.1.65:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "deepseek-chat",
    "messages": [{"role": "user", "content": "Quel temps fait-il a Paris?"}],
    "tools": [{
      "type": "function",
      "function": {
        "name": "get_weather",
        "description": "Obtenir la meteo d une ville",
        "parameters": {
          "type": "object",
          "properties": {
            "city": {"type": "string"},
            "country": {"type": "string"}
          },
          "required": ["city"]
        }
      }
    }]
  }'
```

### Avec le SDK OpenAI Python

```python
from openai import OpenAI

client = OpenAI(
    base_url="http://192.168.1.65:8080/v1",
    api_key="not-needed"
)

response = client.chat.completions.create(
    model="deepseek-chat",
    messages=[{"role": "user", "content": "Bonjour!"}]
)
print(response.choices[0].message.content)
```

### Avec Qwen Code / Cursor / Continue.dev

| Champ | Valeur |
|---|---|
| **Base URL** | `http://<IP>:8080/v1` |
| **API Key** | N'importe quelle valeur (ex: `sk-test`) |
| **Model** | `deepseek-chat` |

---

## Endpoints API

| Méthode | Endpoint | Description |
|---|---|---|
| `POST` | `/v1/chat/completions` | Chat completion compatible OpenAI |
| `GET` | `/v1/models` | Liste des modèles disponibles |
| `GET` | `/status` | État du service d'accessibilité |
| `GET` | `/stats` | Métriques (requêtes, tokens, uptime) |
| `GET/POST` | `/ask?q=...` | Envoi simplifié (legacy) |
| `GET` | `/stop` | Arrêter la génération en cours |
| `POST` | `/config` | Configurer le stream (on/off) |
| `POST` | `/upload` | Upload de fichier vers hébergeur |
| `GET` | `/` | Panneau d'administration web |

---

## Configuration

Le panneau d'administration web (onglet **Config**) permet de :
- **Activer/désactiver le streaming SSE** en un clic
- Voir la configuration réseau (IP, port)
- Copier la chaîne de connexion curl
- Vérifier le statut de calibration

---

## Dépannage

| Problème | Solution |
|---|---|
| `Accessibility Service Disabled` | Réactivez le service dans Paramètres → Accessibilité |
| Timeout (>3 min) | L'IA génère une réponse très longue, le timeout est de 180s |
| `Connection refused` | Vérifiez que vous êtes sur le même réseau WiFi, vérifiez l'IP |
| Réponse vide | Vérifiez la calibration du bouton Copier |
| Qwen Code `API Error: non-SSE response` | Activez le streaming dans l'onglet Config du panneau admin |

---

## Build depuis les sources

```bash
git clone https://github.com/teteekoue/api_bridge.git
cd api_bridge
./gradlew assembleDebug
```

L'APK se trouve dans `app/build/outputs/apk/debug/app-debug.apk`.

Le workflow GitHub Actions compile automatiquement à chaque push sur `main`.

---

## Licence

MIT
