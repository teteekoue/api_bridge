package com.ialocalbridge.utils

object WebInterface {
    fun getHtml(ipAddress: String, port: Int): String {
        return """
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>IA Bridge v5 - Debug Mode</title>
    <style>
        body { font-family: 'Segoe UI', sans-serif; background: #f0f2f5; margin: 0; display: flex; height: 100vh; }
        .sidebar { width: 320px; background: #1a237e; color: white; padding: 20px; display: flex; flex-direction: column; box-shadow: 2px 0 5px rgba(0,0,0,0.1); }
        .main { flex: 1; padding: 20px; display: flex; flex-direction: column; overflow: hidden; }
        .chat-box { flex: 1; background: white; border-radius: 12px; display: flex; flex-direction: column; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
        .messages { flex: 1; padding: 20px; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; background: #fdfdfd; }
        .msg { padding: 10px 15px; border-radius: 15px; max-width: 85%; font-size: 14px; white-space: pre-wrap; }
        .msg.user { align-self: flex-end; background: #3949ab; color: white; }
        .msg.bot { align-self: flex-start; background: #eeeeee; color: #333; }
        .input-area { padding: 15px; background: white; display: flex; gap: 10px; border-top: 1px solid #eee; align-items: center; }
        input[type="text"] { flex: 1; padding: 12px; border: 1px solid #ddd; border-radius: 8px; }
        button { padding: 12px 20px; background: #2e7d32; color: white; border: none; border-radius: 8px; cursor: pointer; font-weight: bold; }
        .debug-panel { background: #ffebee; border: 1px solid #ffcdd2; border-radius: 8px; padding: 15px; margin-top: 20px; color: #b71c1c; font-family: monospace; font-size: 11px; display: none; overflow-x: auto; }
        .file-info { font-size: 12px; color: #fff; background: rgba(255,255,255,0.1); padding: 8px; border-radius: 5px; margin-bottom: 15px; }
    </style>
</head>
<body>
    <div class="sidebar">
        <h2>IA Bridge v5</h2>
        <div id="fileDisplay" class="file-info" style="display:none"></div>
        <div class="api-info" style="font-size: 12px; opacity: 0.8; margin-bottom: 20px;">
            Hôte: <span id="currentHost"></span><br>
            Multi-Upload: Catbox / Tmp / File.io
        </div>
        <button onclick="document.getElementById('fileInput').click()" style="background: #455a64; width: 100%; margin-bottom: 10px;">📁 SÉLECTIONNER FICHIER</button>
        <button onclick="copyUrl()" style="background: #3949ab; width: 100%;">🔗 COPIER URL API</button>
        
        <div id="debugPanel" class="debug-panel">
            <strong>LOGS DE DÉBOGAGE :</strong><br>
            <span id="debugContent"></span>
        </div>
    </div>

    <div class="main">
        <div class="chat-box">
            <div class="messages" id="msgs">
                <div class="msg bot">Système prêt (Mode Fallback Multi-Hébergeurs). Posez une question ou envoyez un fichier.</div>
            </div>
            <div class="input-area">
                <input type="file" id="fileInput" style="display:none" onchange="updateFileUI()">
                <input type="text" id="q" placeholder="Tapez votre message ici..." onkeypress="if(event.key==='Enter') send()">
                <button id="btnSend" onclick="send()">ENVOYER</button>
            </div>
        </div>
    </div>

    <script>
        const API_BASE = window.location.origin;
        document.getElementById('currentHost').innerText = API_BASE;

        function updateFileUI() {
            const file = document.getElementById('fileInput').files[0];
            const display = document.getElementById('fileDisplay');
            if(file) {
                display.innerText = "📎 Fichier: " + file.name + " (" + (file.size/1024).toFixed(1) + " KB)";
                display.style.display = "block";
            } else {
                display.style.display = "none";
            }
        }

        async function send() {
            const input = document.getElementById('q');
            const fileInput = document.getElementById('fileInput');
            const btn = document.getElementById('btnSend');
            const debug = document.getElementById('debugPanel');
            const text = input.value.trim();
            const file = fileInput.files[0];

            if(!text && !file) return;

            addMsg((file ? "📁 [" + file.name + "]\n" : "") + text, 'user');
            input.value = "";
            input.disabled = true;
            btn.disabled = true;
            debug.style.display = "none";

            const loadingId = 'L-' + Date.now();
            const loadingMsg = addMsg(file ? "Initialisation du Multi-Upload..." : "Envoi...", 'bot', loadingId);

            try {
                let jobId;
                if(file) {
                    const fd = new FormData();
                    fd.append('file', file);
                    if(text) fd.append('q', text);
                    
                    const resp = await fetch(API_BASE + "/ask-with-file", { method: 'POST', body: fd });
                    if(!resp.ok) {
                        const err = await resp.text();
                        showDebug(err);
                        throw new Error("Échec critique (Voir logs)");
                    }
                    jobId = await resp.text();
                } else {
                    const resp = await fetch(API_BASE + "/ask", {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                        body: 'q=' + encodeURIComponent(text)
                    });
                    if(!resp.ok) throw new Error("Erreur HTTP " + resp.status);
                    jobId = await resp.text();
                }

                loadingMsg.innerText = "Automatisation lancée (ID: " + jobId + ")...";

                // Polling
                let finished = false;
                while(!finished) {
                    await new Promise(r => setTimeout(r, 3000));
                    const res = await fetch(API_BASE + "/result?id=" + jobId);
                    const out = await res.text();
                    if(out !== "STILL_WORKING") {
                        loadingMsg.innerText = out;
                        finished = true;
                    } else {
                        loadingMsg.innerText = "L'IA travaille sur votre demande...";
                    }
                }
            } catch(e) {
                loadingMsg.innerText = "ERREUR: " + e.message;
            } finally {
                input.disabled = false;
                btn.disabled = false;
                fileInput.value = "";
                updateFileUI();
                document.getElementById('msgs').scrollTop = document.getElementById('msgs').scrollHeight;
            }
        }

        function showDebug(content) {
            document.getElementById('debugContent').innerText = content;
            document.getElementById('debugPanel').style.display = "block";
        }

        function addMsg(text, type, id) {
            const div = document.createElement('div');
            div.className = 'msg ' + type;
            if(id) div.id = id;
            div.innerText = text;
            const msgs = document.getElementById('msgs');
            msgs.appendChild(div);
            msgs.scrollTop = msgs.scrollHeight;
            return div;
        }

        function copyUrl() {
            navigator.clipboard.writeText(API_BASE + "/ask");
            alert("URL de l'API copiée !");
        }
    </script>
</body>
</html>
        """.trimIndent()
    }
}
