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
    <title>IA Bridge Pro</title>
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

        body { font-family: 'Inter', system-ui, -apple-system, sans-serif; background: var(--bg); color: var(--text); margin: 0; display: flex; height: 100vh; overflow: hidden; }

        /* Sidebar & Menu */
        .sidebar { 
            width: var(--sidebar-width); 
            background: #1a1c2c; 
            color: white; 
            display: flex; 
            flex-direction: column; 
            transition: transform 0.3s ease;
            z-index: 1000;
        }

        .sidebar-header { padding: 30px 20px; text-align: center; border-bottom: 1px solid rgba(255,255,255,0.1); }
        .sidebar-header h2 { margin: 0; font-size: 1.5rem; letter-spacing: 1px; color: var(--secondary); }

        .menu-items { flex: 1; padding: 20px 0; }
        .menu-item { 
            padding: 15px 25px; 
            display: flex; 
            align-items: center; 
            gap: 15px; 
            cursor: pointer; 
            transition: background 0.2s;
            color: rgba(255,255,255,0.7);
        }
        .menu-item:hover, .menu-item.active { background: rgba(255,255,255,0.1); color: white; }
        .menu-item i { width: 20px; }

        /* Main Content Areas */
        .main-container { flex: 1; display: flex; flex-direction: column; position: relative; }
        .content-section { display: none; flex: 1; padding: 30px; overflow-y: auto; }
        .content-section.active { display: flex; flex-direction: column; }

        /* Chat Section */
        #chat-section { padding: 0; }
        .chat-header { padding: 15px 25px; background: var(--white); border-bottom: 1px solid #eee; display: flex; align-items: center; gap: 15px; }
        
        .messages { flex: 1; padding: 20px; display: flex; flex-direction: column; gap: 15px; overflow-y: auto; background: #fdfdfd; }
        .msg { max-width: 80%; padding: 12px 18px; border-radius: 18px; line-height: 1.5; font-size: 0.95rem; position: relative; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }
        .msg.user { align-self: flex-end; background: var(--primary); color: white; border-bottom-right-radius: 4px; }
        .msg.bot { align-self: flex-start; background: #ececf1; color: var(--text); border-bottom-left-radius: 4px; }
        
        .input-container { padding: 20px; background: var(--white); border-top: 1px solid #eee; }
        .input-wrapper { 
            max-width: 900px; 
            margin: 0 auto; 
            background: #f4f4f9; 
            border-radius: 12px; 
            padding: 10px; 
            display: flex; 
            align-items: flex-end; 
            gap: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.05);
        }
        
        textarea { 
            flex: 1; 
            background: transparent; 
            border: none; 
            outline: none; 
            padding: 10px; 
            font-family: inherit; 
            font-size: 1rem; 
            resize: none; 
            max-height: 200px; 
        }

        .action-btn { 
            width: 45px; 
            height: 45px; 
            border-radius: 10px; 
            border: none; 
            cursor: pointer; 
            display: flex; 
            align-items: center; 
            justify-content: center; 
            transition: all 0.2s;
            font-size: 1.1rem;
        }
        .btn-upload { background: #e8eaf6; color: var(--primary); }
        .btn-send { background: var(--primary); color: white; }
        .btn-send:disabled { background: #ccc; cursor: not-allowed; }

        /* File Preview */
        .file-previews { display: flex; flex-wrap: wrap; gap: 8px; padding: 0 10px 10px 10px; }
        .file-chip { background: #fff; border: 1px solid #ddd; padding: 5px 10px; border-radius: 20px; font-size: 0.8rem; display: flex; align-items: center; gap: 8px; }
        .file-chip i { cursor: pointer; color: #ff5252; }

        /* Config & API Sections */
        .card { background: white; border-radius: 12px; padding: 25px; box-shadow: 0 4px 6px rgba(0,0,0,0.05); margin-bottom: 20px; }
        .card h3 { margin-top: 0; color: var(--primary); }
        .info-row { display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #eee; }
        .info-label { font-weight: 600; color: #666; }
        .copy-box { background: #f4f4f9; padding: 15px; border-radius: 8px; font-family: monospace; display: flex; justify-content: space-between; align-items: center; margin-top: 10px; overflow-x: auto; }
        
        .status-badge { padding: 5px 12px; border-radius: 20px; font-size: 0.8rem; font-weight: bold; }
        .status-online { background: #e8f5e9; color: #2e7d32; }

        #debug-area { margin-top: 20px; background: #1a1c2c; color: #ff5252; padding: 15px; border-radius: 8px; font-family: monospace; font-size: 0.8rem; display: none; overflow-x: auto; }
    </style>
</head>
<body>
    <div class="sidebar">
        <div class="sidebar-header">
            <h2>IA BRIDGE PRO</h2>
        </div>
        <div class="menu-items">
            <div class="menu-item active" onclick="showSection('chat', this)">
                <i class="fas fa-comments"></i> Mode Chat
            </div>
            <div class="menu-item" onclick="showSection('config', this)">
                <i class="fas fa-cog"></i> Configuration
            </div>
            <div class="menu-item" onclick="showSection('api', this)">
                <i class="fas fa-code"></i> API & Endpoints
            </div>
        </div>
        <div style="padding: 20px; font-size: 0.75rem; opacity: 0.5;">v6.0 Build Ready</div>
    </div>

    <div class="main-container">
        <!-- Section Chat -->
        <section id="chat-section" class="content-section active">
            <div class="chat-header">
                <i class="fas fa-robot" style="color: var(--primary); font-size: 1.5rem;"></i>
                <div>
                    <div style="font-weight: bold;">Assistant Local</div>
                    <div style="font-size: 0.75rem; color: #2e7d32;">● Système en ligne</div>
                </div>
            </div>
            <div class="messages" id="msgs">
                <div class="msg bot">Bonjour ! Je suis votre passerelle IA. Posez une question ou glissez des fichiers pour commencer.</div>
            </div>
            <div class="input-container">
                <div id="filePreviews" class="file-previews"></div>
                <div class="input-wrapper">
                    <button class="action-btn btn-upload" onclick="document.getElementById('fileInput').click()" title="Ajouter des fichiers">
                        <i class="fas fa-paperclip"></i>
                    </button>
                    <input type="file" id="fileInput" multiple style="display:none" onchange="handleFileSelect()">
                    <textarea id="q" placeholder="Écrire un message..." rows="1" oninput="this.style.height='auto';this.style.height=this.scrollHeight+'px'" onkeypress="handleKeyPress(event)"></textarea>
                    <button id="btnSend" class="action-btn btn-send" onclick="send()">
                        <i class="fas fa-paper-plane"></i>
                    </button>
                </div>
            </div>
        </section>

        <!-- Section Config -->
        <section id="config-section" class="content-section">
            <div class="card">
                <h3>Paramètres Réseau</h3>
                <div class="info-row">
                    <span class="info-label">Adresse IP</span>
                    <span>$ipAddress</span>
                </div>
                <div class="info-row">
                    <span class="info-label">Port Serveur</span>
                    <span>$port</span>
                </div>
                <div class="info-row">
                    <span class="info-label">Statut Service</span>
                    <span class="status-badge status-online">ACTIF</span>
                </div>
            </div>
        </section>

        <!-- Section API -->
        <section id="api-section" class="content-section">
            <div class="card">
                <h3>Endpoints API</h3>
                <div style="margin-top:20px;">
                    <label class="info-label">TEXTE SIMPLE</label>
                    <div class="copy-box">
                        <span id="url-ask">http://$ipAddress:$port/ask?q=...</span>
                        <i class="far fa-copy" onclick="copyToClipboard('url-ask')"></i>
                    </div>
                </div>
                <div style="margin-top:20px;">
                    <label class="info-label">UPLOAD & ASK</label>
                    <div class="copy-box">
                        <span id="url-file">http://$ipAddress:$port/ask-with-file</span>
                        <i class="far fa-copy" onclick="copyToClipboard('url-file')"></i>
                    </div>
                </div>
            </div>
            <div id="debug-area"></div>
        </section>
    </div>

    <script>
        const API_BASE = window.location.origin;
        let selectedFiles = [];

        function showSection(sectionId, element) {
            document.querySelectorAll('.content-section').forEach(s => s.classList.remove('active'));
            document.querySelectorAll('.menu-item').forEach(m => m.classList.remove('active'));
            document.getElementById(sectionId + '-section').classList.add('active');
            element.classList.add('active');
        }

        function handleFileSelect() {
            const files = Array.from(document.getElementById('fileInput').files);
            selectedFiles = [...selectedFiles, ...files];
            renderFilePreviews();
            document.getElementById('fileInput').value = '';
        }

        function renderFilePreviews() {
            const container = document.getElementById('filePreviews');
            container.innerHTML = '';
            selectedFiles.forEach((file, index) => {
                const chip = document.createElement('div');
                chip.className = 'file-chip';
                chip.innerHTML = `
                    <i class="fas fa-file-alt"></i>
                    <span>${dollar}{file.name.substring(0, 15)}${dollar}{file.name.length > 15 ? '...' : ''}</span>
                    <i class="fas fa-times" onclick="removeFile(${dollar}{index})"></i>
                `;
                container.appendChild(chip);
            });
        }

        function removeFile(index) {
            selectedFiles.splice(index, 1);
            renderFilePreviews();
        }

        function handleKeyPress(e) {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                send();
            }
        }

        async function send() {
            const input = document.getElementById('q');
            const btn = document.getElementById('btnSend');
            const text = input.value.trim();
            if(!text && selectedFiles.length === 0) return;

            let userMsg = text;
            if(selectedFiles.length > 0) {
                userMsg = "📁 [" + selectedFiles.map(f => f.name).join(', ') + "]\n\n" + text;
            }
            addMsg(userMsg, 'user');

            input.value = '';
            input.style.height = 'auto';
            input.disabled = true;
            btn.disabled = true;
            
            const loadingId = 'bot-' + Date.now();
            const loadingMsg = addMsg("L'IA réfléchit...", 'bot', loadingId);

            try {
                let jobId;
                if(selectedFiles.length > 0) {
                    const fd = new FormData();
                    fd.append('file', selectedFiles[0]); 
                    if(text) fd.append('q', text);
                    const resp = await fetch(API_BASE + "/ask-with-file", { method: 'POST', body: fd });
                    if(!resp.ok) throw new Error(await resp.text());
                    jobId = await resp.text();
                } else {
                    const resp = await fetch(API_BASE + "/ask", {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                        body: 'q=' + encodeURIComponent(text)
                    });
                    if(!resp.ok) throw new Error("Erreur serveur");
                    jobId = await resp.text();
                }

                selectedFiles = [];
                renderFilePreviews();

                let finished = false;
                while(!finished) {
                    await new Promise(r => setTimeout(r, 3000));
                    const res = await fetch(API_BASE + "/result?id=" + jobId);
                    const out = await res.text();
                    if(out !== "STILL_WORKING") {
                        loadingMsg.innerText = out;
                        finished = true;
                    }
                }
            } catch(e) {
                loadingMsg.innerText = "Désolé, une erreur est survenue.";
                const debug = document.getElementById('debug-area');
                debug.innerText = "Logs: " + e.message;
                debug.style.display = 'block';
            } finally {
                input.disabled = false;
                btn.disabled = false;
                input.focus();
            }
        }

        function addMsg(text, type, id) {
            const div = document.createElement('div');
            div.className = 'msg ' + type;
            if(id) div.id = id;
            div.innerText = text;
            const container = document.getElementById('msgs');
            container.appendChild(div);
            container.scrollTop = container.scrollHeight;
            return div;
        }

        function copyToClipboard(id) {
            const text = document.getElementById(id).innerText;
            navigator.clipboard.writeText(text);
            alert("Copié !");
        }
    </script>
</body>
</html>
        """.trimIndent()
    }
}
