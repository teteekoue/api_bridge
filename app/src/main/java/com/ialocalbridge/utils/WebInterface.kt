package com.ialocalbridge.utils

object WebInterface {
    fun getHtml(ipAddress: String, port: Int): String {
        return """
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>IA Local Bridge - Dashboard</title>
    <style>
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f7f6; margin: 0; display: flex; height: 100vh; }
        .sidebar { width: 300px; background-color: #2c3e50; color: white; padding: 20px; display: flex; flex-direction: column; }
        .main-content { flex: 1; display: flex; flex-direction: column; padding: 20px; overflow-y: auto; }
        h1, h2 { color: #ecf0f1; margin-top: 0; }
        h2 { color: #333; }
        .card { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); margin-bottom: 20px; }
        .chat-container { flex: 1; display: flex; flex-direction: column; background: white; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); overflow: hidden; }
        .chat-messages { flex: 1; padding: 20px; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; }
        .message { padding: 10px 15px; border-radius: 18px; max-width: 80%; line-height: 1.4; }
        .message.user { align-self: flex-end; background-color: #3498db; color: white; }
        .message.bot { align-self: flex-start; background-color: #ecf0f1; color: #333; }
        .chat-input { display: flex; padding: 15px; border-top: 1px solid #eee; gap: 10px; }
        input[type="text"] { flex: 1; padding: 10px; border: 1px solid #ddd; border-radius: 4px; outline: none; }
        button { padding: 10px 20px; background-color: #27ae60; color: white; border: none; border-radius: 4px; cursor: pointer; transition: background 0.3s; }
        button:hover { background-color: #219150; }
        button:disabled { background-color: #95a5a6; cursor: not-allowed; }
        .api-url { font-family: monospace; background: #eee; padding: 10px; border-radius: 4px; display: block; margin: 10px 0; word-break: break-all; }
        .status-badge { display: inline-block; padding: 5px 10px; border-radius: 20px; font-size: 0.8em; font-weight: bold; }
        .status-online { background-color: #2ecc71; color: white; }
    </style>
</head>
<body>
    <div class="sidebar">
        <h1>IA Bridge</h1>
        <p>Statut: <span class="status-badge status-online">Connecté</span></p>
        <hr style="width:100%; border: 0.5px solid #34495e;">
        <h3>Configuration API</h3>
        <p>Utilisez cet URL pour vos requêtes :</p>
        <div class="api-url" id="apiUrl">http://$ipAddress:$port/ask</div>
        <button onclick="copyApiUrl()">Copier l'URL</button>
        <div style="margin-top: auto; font-size: 0.8em; color: #bdc3c7;">
            IP: $ipAddress | Port: $port
        </div>
    </div>
    <div class="main-content">
        <h2>Interface de Chat</h2>
        <div class="chat-container">
            <div class="chat-messages" id="chatMessages">
                <div class="message bot">Bonjour ! Je suis votre pont IA. Posez-moi une question.</div>
            </div>
            <div class="chat-input">
                <input type="text" id="userInput" placeholder="Écrivez votre message ici..." onkeypress="if(event.key === 'Enter') sendMessage()">
                <button id="sendBtn" onclick="sendMessage()">Envoyer</button>
            </div>
        </div>
    </div>

    <script>
        const apiUrl = "http://$ipAddress:$port/ask";
        
        function copyApiUrl() {
            navigator.clipboard.writeText(apiUrl);
            alert("URL de l'API copiée !");
        }

        async function sendMessage() {
            const input = document.getElementById('userInput');
            const sendBtn = document.getElementById('sendBtn');
            const messages = document.getElementById('chatMessages');
            const text = input.value.trim();

            if (!text) return;

            // Ajouter message utilisateur
            addMessage(text, 'user');
            input.value = '';
            input.disabled = true;
            sendBtn.disabled = true;

            // Ajouter indicateur de chargement
            const loadingId = 'loading-' + Date.now();
            addMessage("L'IA réfléchit...", 'bot', loadingId);

            try {
                // Utilisation de POST pour envoyer la question
                const response = await fetch(apiUrl, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: 'q=' + encodeURIComponent(text)
                });
                
                const data = await response.text();
                document.getElementById(loadingId).innerText = data;
            } catch (error) {
                document.getElementById(loadingId).innerText = "Erreur de connexion : " + error.message;
            } finally {
                input.disabled = false;
                sendBtn.disabled = false;
                input.focus();
                messages.scrollTop = messages.scrollHeight;
            }
        }

        function addMessage(text, type, id = null) {
            const messages = document.getElementById('chatMessages');
            const div = document.createElement('div');
            div.className = 'message ' + type;
            if (id) div.id = id;
            div.innerText = text;
            messages.appendChild(div);
            messages.scrollTop = messages.scrollHeight;
        }
    </script>
</body>
</html>
        """.trimIndent()
    }
}
        