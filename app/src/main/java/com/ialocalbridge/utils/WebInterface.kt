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
    <title>NEMAPI Pro v6.3</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        :root { --primary: #4361ee; --primary-dark: #3f37c9; --secondary: #4cc9f0; --bg: #f8f9fa; --text: #2b2d42; --white: #ffffff; --sidebar-width: 280px; }
        body { font-family: 'Inter', system-ui, sans-serif; background: var(--bg); color: var(--text); margin: 0; display: flex; height: 100vh; overflow: hidden; }
        .sidebar { width: var(--sidebar-width); background: #1a1c2c; color: white; display: flex; flex-direction: column; z-index: 1000; }
        .sidebar-header { padding: 30px 20px; text-align: center; border-bottom: 1px solid rgba(255,255,255,0.1); }
        .menu-items { flex: 1; padding: 20px 0; }
        .menu-item { padding: 15px 25px; display: flex; align-items: center; gap: 15px; cursor: pointer; color: rgba(255,255,255,0.7); }
        .menu-item.active { background: rgba(255,255,255,0.1); color: white; }
        .main-container { flex: 1; display: flex; flex-direction: column; }
        .content-section { display: none; flex: 1; padding: 30px; overflow-y: auto; }
        .content-section.active { display: flex; flex-direction: column; }
        .messages { flex: 1; padding: 20px; display: flex; flex-direction: column; gap: 15px; overflow-y: auto; background: #fdfdfd; }
        .msg { max-width: 85%; padding: 12px 18px; border-radius: 18px; line-height: 1.5; font-size: 0.95rem; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }
        .msg.user { align-self: flex-end; background: var(--primary); color: white; border-bottom-right-radius: 4px; }
        .msg.bot { align-self: flex-start; background: #ececf1; color: var(--text); border-bottom-left-radius: 4px; white-space: pre-wrap; }
        .input-container { padding: 20px; background: var(--white); border-top: 1px solid #eee; }
        .input-wrapper { max-width: 1000px; margin: 0 auto; background: #f4f4f9; border-radius: 15px; padding: 8px 15px; display: flex; align-items: center; gap: 12px; }
        textarea { flex: 1; background: transparent; border: none; outline: none; padding: 10px 0; font-family: inherit; font-size: 1rem; resize: none; max-height: 150px; }
        .action-btn { width: 42px; height: 42px; border-radius: 50%; border: none; cursor: pointer; display: flex; align-items: center; justify-content: center; }
        .btn-send { background: var(--primary); color: white; }
        .btn-stop { background: #D32F2F; color: white; display: none; }
        .file-previews { display: flex; flex-wrap: wrap; gap: 8px; max-width: 1000px; margin: 0 auto 10px auto; }
        .file-chip { background: #e8eaf6; border: 1px solid var(--primary); color: var(--primary); padding: 4px 12px; border-radius: 15px; font-size: 0.8rem; display: flex; align-items: center; gap: 8px; }
        .debug-panel { margin-top: 20px; background: #ffebee; color: #b71c1c; padding: 15px; border-radius: 8px; font-family: monospace; font-size: 0.8rem; display: none; border: 1px solid #ffcdd2; }
    </style>
</head>
<body>
    <div class="sidebar">
        <div class="sidebar-header"><h2>NEMAPI PRO</h2></div>
        <div class="menu-items">
            <div id="m-chat" class="menu-item active" onclick="showSection('chat')"><i class="fas fa-comment-dots"></i> Mode Chat</div>
            <div id="m-config" class="menu-item" onclick="showSection('config')"><i class="fas fa-sliders-h"></i> Configuration</div>
        </div>
        <div style="padding: 20px; font-size: 0.7rem; opacity: 0.4;">v6.3 Build (6-Host Fallback)</div>
    </div>
    <div class="main-container">
        <section id="chat-section" class="content-section active">
            <div class="messages" id="msgs"><div class="msg bot">NEMAPI Pro prêt. 6 hébergeurs actifs pour vos fichiers.</div></div>
            <div id="debug-panel" class="debug-panel"></div>
            <div class="input-container">
                <div id="filePreviews" class="file-previews"></div>
                <div class="input-wrapper">
                    <button class="action-btn" onclick="document.getElementById('fileInput').click()"><i class="fas fa-plus"></i></button>
                    <input type="file" id="fileInput" multiple style="display:none" onchange="handleFileSelect()">
                    <textarea id="q" placeholder="Posez votre question..." rows="1" oninput="this.style.height='auto';this.style.height=this.scrollHeight+'px'"></textarea>
                    <button id="btnStop" class="action-btn btn-stop" onclick="stopAI()"><i class="fas fa-stop"></i></button>
                    <button id="btnSend" class="action-btn btn-send" onclick="send()"><i class="fas fa-arrow-up"></i></button>
                </div>
            </div>
        </section>
        <section id="config-section" class="content-section">
            <div style="background:white; padding:25px; border-radius:12px; box-shadow:0 4px 6px rgba(0,0,0,0.05)">
                <h3>Réseau NEMAPI</h3>
                <p>IP: $ipAddress</p>
                <p>Port: $port</p>
                <p>Hébergeurs: Catbox, Tmp.ninja, 0x0.st, Pomf.cat, File.io, GoFile</p>
            </div>
        </section>
    </div>
    <script>
        const BASE = window.location.origin;
        let filesQueue = [];
        function showSection(id) { document.querySelectorAll('.content-section, .menu-item').forEach(e => e.classList.remove('active')); document.getElementById(id + '-section').classList.add('active'); document.getElementById('m-' + id).classList.add('active'); }
        function handleFileSelect() { const chosen = Array.from(document.getElementById('fileInput').files); filesQueue = [...filesQueue, ...chosen]; renderChips(); document.getElementById('fileInput').value = ''; }
        function renderChips() { const container = document.getElementById('filePreviews'); container.innerHTML = ''; filesQueue.forEach((f, i) => { const chip = document.createElement('div'); chip.className = 'file-chip'; chip.innerHTML = `<i class="fas fa-file"></i> ${dollar}{f.name.substring(0,10)} <i class="fas fa-times-circle" style="cursor:pointer" onclick="removeFile(${dollar}{i})"></i>`; container.appendChild(chip); }); }
        function removeFile(i) { filesQueue.splice(i, 1); renderChips(); }
        async function send() {
            const qInput = document.getElementById('q'); const text = qInput.value.trim();
            const btnSend = document.getElementById('btnSend'); const btnStop = document.getElementById('btnStop');
            const debug = document.getElementById('debug-panel');
            if (!text && filesQueue.length === 0) return;
            addMsg((filesQueue.length > 0 ? "📁 [" + filesQueue.map(f => f.name).join(', ') + "]\n\n" : "") + text, 'user');
            qInput.value = ''; qInput.disabled = true; btnSend.style.display = 'none'; btnStop.style.display = 'flex'; debug.style.display = 'none';
            const loadingMsg = addMsg("Initialisation...", 'bot');
            try {
                let finalPrompt = text;
                if (filesQueue.length > 0) {
                    const links = [];
                    for (let i = 0; i < filesQueue.length; i++) {
                        const f = filesQueue[i];
                        loadingMsg.innerText = "Upload " + (i + 1) + "/" + filesQueue.length + " (" + f.name + ")...";
                        const fd = new FormData(); fd.append('file', f);
                        const upResp = await fetch(BASE + "/upload?file=" + encodeURIComponent(f.name), { method: 'POST', body: fd });
                        if (!upResp.ok) { const err = await upResp.text(); throw new Error("Upload " + f.name + " échoué:\n" + err); }
                        const data = await upResp.json();
                        links.push("- " + f.name + " : " + data.url);
                    }
                    finalPrompt = "[FICHIERS JOINTS]\n" + links.join('\n') + "\n\nQuestion: " + text;
                    filesQueue = []; renderChips();
                }
                loadingMsg.innerText = "L'IA réfléchit...";
                const askResp = await fetch(BASE + "/ask", { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: 'q=' + encodeURIComponent(finalPrompt) });
                const jobId = await askResp.text();
                let done = false;
                while (!done) {
                    await new Promise(r => setTimeout(r, 3000));
                    const res = await fetch(BASE + "/result?id=" + jobId);
                    const out = await res.text();
                    if (out !== "STILL_WORKING") { loadingMsg.innerText = out; done = true; }
                }
            } catch (e) {
                loadingMsg.innerText = "Échec du processus.";
                debug.innerText = "LOGS DE DÉBOGAGE :\n" + e.message;
                debug.style.display = 'block';
            } finally { qInput.disabled = false; btnSend.style.display = 'flex'; btnStop.style.display = 'none'; qInput.focus(); }
        }
        async function stopAI() { await fetch(BASE + "/stop"); addMsg("Arrêt envoyé.", 'bot'); }
        function addMsg(text, type) { const div = document.createElement('div'); div.className = 'msg ' + type; div.innerText = text; document.getElementById('msgs').appendChild(div); document.getElementById('msgs').scrollTop = document.getElementById('msgs').scrollHeight; return div; }
    </script>
</body>
</html>
        """.trimIndent()
    }
}
