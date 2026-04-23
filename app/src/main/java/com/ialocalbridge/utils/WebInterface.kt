package com.ialocalbridge.utils

object WebInterface {
fun getHtml(ip: String, port: Int): String {
return """

<!DOCTYPE html>

<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>IA Bridge - Dashboard</title>
    <style>
        body { font-family: 'Segoe UI', system-ui; background: #f0f2f5; margin: 0; padding: 20px; }
        .container { max-width: 800px; margin: 0 auto; }
        .card { background: white; border-radius: 16px; padding: 20px; margin-bottom: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
        h2 { margin-top: 0; color: #1a237e; }
        input, textarea, button { width: 100%; padding: 12px; margin: 8px 0; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; }
        button { background: #1a237e; color: white; font-weight: bold; cursor: pointer; transition: 0.2s; border: none; }
        button:hover { background: #0d1b5e; }
        .status { display: inline-block; padding: 4px 12px; border-radius: 20px; font-size: 12px; margin-left: 10px; }
        .status.ready { background: #4caf50; color: white; }
        .status.waiting { background: #ff9800; color: white; }
        .result { background: #f5f5f5; padding: 12px; border-radius: 8px; white-space: pre-wrap; font-family: monospace; }
        .tab-bar { display: flex; gap: 10px; margin-bottom: 20px; }
        .tab { padding: 10px 20px; background: #e0e0e0; border-radius: 8px; cursor: pointer; }
        .tab.active { background: #1a237e; color: white; }
        .tab-content { display: none; }
        .tab-content.active { display: block; }
        .file-input-wrapper { margin: 8px 0; }
        .file-info { font-size: 12px; color: #666; margin-top: -5px; margin-bottom: 10px; }
    </style>
</head>
<body>
<div class="container">
    <div class="card">
        <h1>🤖 IA Local Bridge</h1>
        <p>API accessible sur: <strong>http://$ip:$port</strong></p>
        <div id="serverStatus">Status: <span id="statusText">Vérification...</span></div>
    </div>

</div>

<script>
    function switchTab(tab) {
        document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
        document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
        if (tab === 'text') {
            document.querySelector('.tab-bar .tab:first-child').classList.add('active');
            document.getElementById('textTab').classList.add('active');
        } else {
            document.querySelector('.tab-bar .tab:last-child').classList.add('active');
            document.getElementById('fileTab').classList.add('active');
        }
    }

    async function sendText() {
        const text = document.getElementById('textInput').value;
        if (!text) return;
        const resultDiv = document.getElementById('textResult');
        resultDiv.innerText = 'Envoi en cours...';
        try {
            const response = await fetch('/ask?q=' + encodeURIComponent(text), { method: 'GET' });
            const jobId = await response.text();
            resultDiv.innerText = 'Job ID: ' + jobId + '\\nEn attente du résultat...';
            await pollResult(jobId, resultDiv);
        } catch(e) {
            resultDiv.innerText = 'Erreur: ' + e.message;
        }
    }

    async function sendFile() {
        const fileInput = document.getElementById('fileInput');
        const message = document.getElementById('fileMessageInput').value;
        const file = fileInput.files[0];
        if (!file) {
            alert('Sélectionnez un fichier');
            return;
        }
        if (file.size > 50 * 1024 * 1024) {
            alert('Fichier trop volumineux (max 50 MB)');
            return;
        }
        
        const resultDiv = document.getElementById('fileResult');
        resultDiv.innerText = '📤 Upload et envoi en cours...';
        
        const formData = new FormData();
        formData.append('file', file);
        formData.append('message', message);
        
        try {
            const response = await fetch('/upload-file', {
                method: 'POST',
                body: formData
            });
            const data = await response.json();
            resultDiv.innerText = '✅ Fichier uploadé: ' + data.fileUrl + '\\n\\n🤖 Réponse IA:\\n' + data.jobId + '\\nAttente...';
            await pollResult(data.jobId, resultDiv);
        } catch(e) {
            resultDiv.innerText = '❌ Erreur: ' + e.message;
        }
    }

    async function pollResult(jobId, resultDiv) {
        let attempts = 0;
        while (attempts < 60) {
            await new Promise(r => setTimeout(r, 2000));
            try {
                const res = await fetch('/result?id=' + jobId);
                const text = await res.text();
                if (text !== 'STILL_WORKING') {
                    resultDiv.innerText = text;
                    return;
                }
                resultDiv.innerText = '🔄 Travail en cours... (' + (attempts+1) + '/60)';
            } catch(e) {
                resultDiv.innerText = 'Erreur polling: ' + e.message;
                return;
            }
            attempts++;
        }
        resultDiv.innerText = '⏱️ Timeout après 2 minutes';
    }

    async function checkStatus() {
        try {
            const res = await fetch('/status');
            const status = await res.text();
            const span = document.getElementById('statusText');
            span.innerText = status;
            span.className = status.includes('Ready') ? 'status ready' : 'status waiting';
        } catch(e) {}
    }
    checkStatus();
    setInterval(checkStatus, 5000);
</script>

</body>
</html>
        """.trimIndent()
    }
}
