package com.ialocalbridge.utils

object WebInterface {
    fun getHtml(ipAddress: String, port: Int): String {
        val dollar = "$"
        return """
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>IA Bridge Pro v6.2</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        :root {
            --primary: #4361ee;
            --primary-dark: #3f37c9;
            --secondary: #4cc9f0;
            --bg: #f8f9fa;
            --text: #2b2d42;
            --white: #ffffff;
            --sidebar-width: 280px;
        }

        body { font-family: 'Inter', system-ui, sans-serif; background: var(--bg); color: var(--text); margin: 0; display: flex; height: 100vh; overflow: hidden; }

        .sidebar { width: var(--sidebar-width); background: #1a1c2c; color: white; display: flex; flex-direction: column; transition: transform 0.3s ease; z-index: 1000; }
        .sidebar-header { padding: 30px 20px; text-align: center; border-bottom: 1px solid rgba(255,255,255,0.1); }
        .sidebar-header h2 { margin: 0; font-size: 1.5rem; letter-spacing: 1px; color: var(--secondary); }
        .menu-items { flex: 1; padding: 20px 0; }
        .menu-item { padding: 15px 25px; display: flex; align-items: center; gap: 15px; cursor: pointer; transition: background 0.2s; color: rgba(255,255,255,0.7); }
        .menu-item:hover, .menu-item.active { background: rgba(255,255,255,0.1); color: white; }

        .main-container { flex: 1; display: flex; flex-direction: column; position: relative; }
        .content-section { display: none; flex: 1; padding: 30px; overflow-y: auto; }
        .content-section.active { display: flex; flex-direction: column; }

        #chat-section { padding: 0; }
        .chat-header { padding: 15px 25px; background: var(--white); border-bottom: 1px solid #eee; display: flex; align-items: center; gap: 15px; }
        .messages { flex: 1; padding: 20px; display: flex; flex-direction: column; gap: 15px; overflow-y: auto; background: #fdfdfd; }
        .msg { max-width: 85%; padding: 12px 18px; border-radius: 18px; line-height: 1.5; font-size: 0.95rem; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }
        .msg.user { align-self: flex-end; background: var(--primary); color: white; border-bottom-right-radius: 4px; }
        .msg.bot { align-self: flex-start; background: #ececf1; color: var(--text); border-bottom-left-radius: 4px; white-space: pre-wrap; }
        
        .input-container { padding: 20px; background: var(--white); border-top: 1px solid #eee; }
        .input-wrapper { max-width: 1000px; margin: 0 auto; background: #f4f4f9; border-radius: 15px; padding: 8px 15px; display: flex; align-items: center; gap: 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.05); }
        textarea { flex: 1; background: transparent; border: none; outline: none; padding: 10px 0; font-family: inherit; font-size: 1rem; resize: none; max-height: 150px; }
        
        .action-btn { width: 42px; height: 42px; border-radius: 50%; border: none; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.2s; font-size: 1.1rem; }
        .btn-upload { background: #fff; color: #555; border: 1px solid #ddd; }
        .btn-send { background: var(--primary); color: white; }
        .btn-stop { background: #D32F2F; color: white; display: none; }
        .btn-send:disabled { background: #ccc; cursor: not-allowed; }

        .file-previews { display: flex; flex-wrap: wrap; gap: 8px; max-width: 1000px; margin: 0 auto 10px auto; }
        .file-chip { background: #e8eaf6; border: 1px solid var(--primary); color: var(--primary); padding: 4px 12px; border-radius: 15px; font-size: 0.8rem; display: flex; align-items: center; gap: 8px; font-weight: 500; }
        .file-chip i.remove-file { cursor: pointer; color: #ff5252; margin-left: 5px; }

        .card { background: white; border-radius: 12px; padding: 25px; box-shadow: 0 4px 6px rgba(0,0,0,0.05); margin-bottom: 20px; }
        .info-row { display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #eee; }
        .copy-box { background: #f4f4f9; padding: 15px; border-radius: 8px; font-family: monospace; display: flex; justify-content: space-between; align-items: center; margin-top: 10px; overflow-x: auto; }
        .status-badge { padding: 5px 12px; border-radius: 20px; font-size: 0.8rem; font-weight: bold; background: #e8f5e9; color: #2e7d32; }

        #debug-panel { margin-top: 20px; background: #1a1c2c; color: #ff5252; padding: 15px; border-radius: 8px; font-family: monospace; font-size: 0.8rem; display: none; white-space: pre-wrap; }
    </style>
</head>
<body>
    <div class="sidebar">
        <div class="sidebar-header"><h2>IA BRIDGE</h2></div>
        <div class="menu-items">
            <div id="m-chat" class="menu-item active" onclick="showSection('chat')"><i class="fas fa-comment-dots"></i> Mode Chat</div>
            <div id="m-config" class="menu-item" onclick="showSection('config')"><i class="fas fa-sliders-h"></i> Configuration</div>
            <div id="m-api" class="menu-item" onclick="showSection('api')"><i class="fas fa-link"></i> Endpoints API</div>
        </div>
        <div style="padding: 20px; font-size: 0.7rem; opacity: 0.4;">Pro Edition v6.2</div>
    </div>

    <div class="main-container">
        <section id="chat-section" class="content-section active">
            <div class="chat-header">
                <i class="fas fa-robot" style="color: var(--primary); font-size: 1.5rem;"></i>
                <div><div style="font-weight: bold;">IA Assistant</div><div style="font-size: 0.75rem; color: #2e7d32;">● Système Prêt</div></div>
            </div>
            <div class="messages" id="msgs">
                <div class="msg bot">Bonjour ! Sélectionnez un ou plusieurs fichiers et posez votre question.</div>
            </div>
            <div class="input-container">
                <div id="filePreviews" class="file-previews"></div>
                <div class="input-wrapper">
                    <button class="action-btn btn-upload" onclick="document.getElementById('fileInput').click()" title="Joindre des fichiers"><i class="fas fa-plus"></i></button>
                    <input type="file" id="fileInput" multiple style="display:none" onchange="handleFileSelect()">
                    <textarea id="q" placeholder="Posez votre question..." rows="1" oninput="this.style.height='auto';this.style.height=this.scrollHeight+'px'" onkeypress="if(event.key==='Enter'&&!event.shiftKey){event.preventDefault();send();}"></textarea>
                    <button id="btnStop" class="action-btn btn-stop" onclick="stopAI()"><i class="fas fa-stop"></i></button>
                    <button id="btnSend" class="action-btn btn-send" onclick="send()"><i class="fas fa-arrow-up"></i></button>
                </div>
            </div>
        </section>

        <section id="config-section" class="content-section">
            <div class="card">
                <h3>Réseau</h3>
                <div class="info-row"><span>Adresse IP</span><span>$ipAddress</span></div>
                <div class="info-row"><span>Port</span><span>$port</span></div>
                <div class="info-row"><span>Serveur</span><span class="status-badge">ACTIF</span></div>
            </div>
        </section>

        <section id="api-section" class="content-section">
            <div class="card">
                <h3>Endpoints API</h3>
                <div class="copy-box"><span id="u-ask">http://$ipAddress:$port/ask?q=...</span><i class="far fa-copy" onclick="copyText('u-ask')"></i></div>
                <div class="copy-box"><span id="u-stop">http://$ipAddress:$port/stop</span><i class="far fa-copy" onclick="copyText('u-stop')"></i></div>
            </div>
            <div id="debug-panel"></div>
        </section>
    </div>

    <script>
        const BASE = window.location.origin;
        let filesQueue = [];

        function showSection(id) {
            document.querySelectorAll('.content-section, .menu-item').forEach(e => e.classList.remove('active'));
            document.getElementById(id + '-section').classList.add('active');
            document.getElementById('m-' + id).classList.add('active');
        }

        function handleFileSelect() {
            const chosen = Array.from(document.getElementById('fileInput').files);
            filesQueue = [...filesQueue, ...chosen];
            renderChips();
            document.getElementById('fileInput').value = '';
        }

        function renderChips() {
            const container = document.getElementById('filePreviews');
            container.innerHTML = '';
            filesQueue.forEach((f, i) => {
                const chip = document.createElement('div');
                chip.className = 'file-chip';
                chip.innerHTML = `<i class="fas fa-file"></i> ${dollar}{f.name.substring(0,12)} <i class="fas fa-times-circle remove-file" onclick="removeFile(${dollar}{i})"></i>`;
                container.appendChild(chip);
            });
        }

        function removeFile(i) {
            filesQueue.splice(i, 1);
            renderChips();
        }

        async function send() {
            const qInput = document.getElementById('q');
            const text = qInput.value.trim();
            const btnSend = document.getElementById('btnSend');
            const btnStop = document.getElementById('btnStop');
            const debug = document.getElementById('debug-panel');

            if (!text && filesQueue.length === 0) return;

            addMsg((filesQueue.length > 0 ? "📁 [" + filesQueue.map(f => f.name).join(', ') + "]\n\n" : "") + text, 'user');
            qInput.value = ''; qInput.style.height = 'auto'; qInput.disabled = true;
            btnSend.style.display = 'none'; btnStop.style.display = 'flex';
            debug.style.display = 'none';

            const loadingId = 'bot-' + Date.now();
            const loadingMsg = addMsg("Initialisation...", 'bot', loadingId);

            try {
                let finalPrompt = text;
                if (filesQueue.length > 0) {
                    const links = [];
                    for (let i = 0; i < filesQueue.length; i++) {
                        const f = filesQueue[i];
                        loadingMsg.innerText = "Hébergement du fichier " + (i + 1) + "/" + filesQueue.length + "...";
                        const fd = new FormData(); fd.append('file', f);
                        const upResp = await fetch(BASE + "/upload?file=" + encodeURIComponent(f.name), { method: 'POST', body: fd });
                        const data = await upResp.json();
                        links.push("- " + f.name + " : " + data.url);
                    }
                    finalPrompt = "[FICHIERS JOINTS]\n" + links.join('\n') + "\n\nQuestion: " + text;
                    filesQueue = []; renderChips();
                }

                loadingMsg.innerText = "L'IA réfléchit...";
                const askResp = await fetch(BASE + "/ask", {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: 'q=' + encodeURIComponent(finalPrompt)
                });
                const jobId = await askResp.text();

                let done = false;
                while (!done) {
                    await new Promise(r => setTimeout(r, 3000));
                    const res = await fetch(BASE + "/result?id=" + jobId);
                    const out = await res.text();
                    if (out !== "STILL_WORKING") {
                        loadingMsg.innerText = out;
                        done = true;
                    }
                }
            } catch (e) {
                loadingMsg.innerText = "Erreur technique.";
                debug.innerText = e.message; debug.style.display = 'block';
            } finally {
                qInput.disabled = false; btnSend.style.display = 'flex'; btnStop.style.display = 'none';
                qInput.focus();
            }
        }

        async function stopAI() {
            try {
                await fetch(BASE + "/stop");
                addMsg("Demande d'arrêt envoyée.", 'bot');
            } catch (e) {}
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

        function copyText(id) {
            navigator.clipboard.writeText(document.getElementById(id).innerText);
            alert("Copié !");
        }
    </script>
</body>
</html>
        """.trimIndent()
    }
}
