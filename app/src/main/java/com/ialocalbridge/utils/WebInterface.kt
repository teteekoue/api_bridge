package com.ialocalbridge.utils

object WebInterface {
    fun getHtml(ipAddress: String, port: Int): String {
        return """
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>IA Bridge - Dashboard</title>
    <style>
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f0f2f5; margin: 0; display: flex; height: 100vh; }
        .sidebar { width: 300px; background-color: #1a237e; color: white; padding: 25px; display: flex; flex-direction: column; }
        .main { flex: 1; padding: 20px; display: flex; flex-direction: column; }
        .card { background: white; padding: 20px; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); margin-bottom: 20px; }
        .chat-box { flex: 1; background: white; border-radius: 12px; display: flex; flex-direction: column; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
        .messages { flex: 1; padding: 20px; overflow-y: auto; display: flex; flex-direction: column; gap: 12px; background: #f8f9fa; }
        .msg { padding: 12px 16px; border-radius: 20px; max-width: 80%; line-height: 1.5; font-size: 15px; }
        .msg.user { align-self: flex-end; background: #3949ab; color: white; }
        .msg.bot { align-self: flex-start; background: #e0e0e0; color: #212121; white-space: pre-wrap; }
        .input-area { padding: 20px; background: white; display: flex; gap: 10px; border-top: 1px solid #eee; }
        input { flex: 1; padding: 12px; border: 1px solid #ddd; border-radius: 8px; outline: none; font-size: 15px; }
        button { padding: 12px 24px; background: #2e7d32; color: white; border: none; border-radius: 8px; cursor: pointer; font-weight: bold; }
        button:disabled { background: #bdbdbd; }
        .api-info { background: #3949ab; padding: 15px; border-radius: 8px; font-family: monospace; font-size: 13px; margin-top: 20px; color: #e8eaf6; }
        .error-log { color: #ff5252; font-size: 12px; margin-top: 10px; font-family: monospace; }
    </style>
</head>
<body>
    <div class="sidebar">
        <h2>IA Bridge</h2>
        <p>Statut: <span style="color: #4caf50">● Connecté</span></p>
        <div class="api-info">
            URL API:<br>
            <span id="url">http://$ipAddress:$port/ask</span>
        </div>
        <button style="margin-top:10px; background: #5c6bc0" onclick="copyUrl()">Copier URL</button>
        <div id="logs" class="error-log"></div>
    </div>
    <div class="main">
        <div class="chat-box">
            <div class="messages" id="msgs">
                <div class="msg bot">Système prêt. Configurez la calibration sur le téléphone, puis posez votre question ici.</div>
            </div>
            <div class="input-area">
                <input type="text" id="q" placeholder="Posez votre question..." onkeypress="if(event.key==='Enter') send()">
                <button id="btn" onclick="send()">ENVOYER</button>
            </div>
        </div>
    </div>

    <script>
        const API_URL = "http://$ipAddress:$port/ask";
        
        function copyUrl() {
            navigator.clipboard.writeText(API_URL);
            alert("URL copiée !");
        }

        async function send() {
            const input = document.getElementById('q');
            const btn = document.getElementById('btn');
            const msgs = document.getElementById('msgs');
            const logs = document.getElementById('logs');
            const text = input.value.trim();

            if(!text) return;

            addMsg(text, 'user');
            input.value = '';
            input.disabled = true;
            btn.disabled = true;
            logs.innerText = "";

            const loadingId = 'L-' + Date.now();
            addMsg("L'IA génère la réponse (attente silence)...", 'bot', loadingId);

            try {
                const controller = new AbortController();
                const timeoutId = setTimeout(() => controller.abort(), 120000); // 2 min timeout

                const resp = await fetch(API_URL, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: 'q=' + encodeURIComponent(text),
                    signal: controller.signal
                });

                if (!resp.ok) throw new Error("Erreur HTTP " + resp.status);
                
                const data = await resp.text();
                document.getElementById(loadingId).innerText = data;
            } catch (e) {
                document.getElementById(loadingId).innerText = "ERREUR : " + e.message;
                logs.innerText = "Détails: " + e.toString();
                console.error(e);
            } finally {
                input.disabled = false;
                btn.disabled = false;
                input.focus();
                msgs.scrollTop = msgs.scrollHeight;
            }
        }

        function addMsg(text, type, id) {
            const div = document.createElement('div');
            div.className = 'msg ' + type;
            if(id) div.id = id;
            div.innerText = text;
            document.getElementById('msgs').appendChild(div);
            document.getElementById('msgs').scrollTop = document.getElementById('msgs').scrollHeight;
        }
    </script>
</body>
</html>
        """.trimIndent()
    }
}
