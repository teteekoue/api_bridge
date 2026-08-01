package com.ialocalbridge.utils

object WebInterface {
    fun getHtml(ipAddress: String, port: Int): String {
        return """<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>NEMAPI Bridge - Admin Panel</title>
    <style>
        :root {
            --bg: #0f1117;
            --card: #1a1d27;
            --border: #2a2d3a;
            --text: #e1e4ed;
            --muted: #8b8fa3;
            --primary: #6366f1;
            --primary-hover: #818cf8;
            --success: #22c55e;
            --danger: #ef4444;
            --warning: #f59e0b;
            --sidebar-w: 220px;
        }
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Inter', system-ui, -apple-system, sans-serif; background: var(--bg); color: var(--text); display: flex; min-height: 100vh; }
        .sidebar { width: var(--sidebar-w); background: var(--card); border-right: 1px solid var(--border); display: flex; flex-direction: column; position: fixed; top: 0; left: 0; bottom: 0; z-index: 100; }
        .sidebar-header { padding: 20px; display: flex; align-items: center; gap: 10px; border-bottom: 1px solid var(--border); }
        .sidebar-header .logo { width: 32px; height: 32px; background: var(--primary); border-radius: 8px; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 16px; }
        .sidebar-header .title { font-weight: 600; font-size: 16px; }
        .nav { flex: 1; padding: 12px 8px; display: flex; flex-direction: column; gap: 4px; }
        .nav-item { display: flex; align-items: center; gap: 10px; padding: 10px 12px; border-radius: 8px; cursor: pointer; color: var(--muted); font-size: 14px; transition: all 0.15s; border: none; background: none; width: 100%; text-align: left; }
        .nav-item:hover { background: rgba(255,255,255,0.04); color: var(--text); }
        .nav-item.active { background: rgba(99,102,241,0.12); color: var(--primary); }
        .nav-item .icon { width: 20px; text-align: center; font-size: 16px; }
        .sidebar-footer { padding: 12px; border-top: 1px solid var(--border); }
        .status-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; margin-right: 6px; }
        .status-dot.online { background: var(--success); }
        .status-dot.offline { background: var(--danger); }
        .main { margin-left: var(--sidebar-w); flex: 1; display: flex; flex-direction: column; }
        .topbar { padding: 16px 24px; border-bottom: 1px solid var(--border); background: var(--bg); display: flex; align-items: center; justify-content: space-between; position: sticky; top: 0; z-index: 50; }
        .topbar h2 { font-size: 18px; font-weight: 600; }
        .content { flex: 1; padding: 24px; overflow-y: auto; }
        .page { display: none; }
        .page.active { display: block; }
        .cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 16px; margin-bottom: 24px; }
        .card { background: var(--card); border: 1px solid var(--border); border-radius: 12px; padding: 20px; }
        .card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
        .card-title { font-size: 13px; color: var(--muted); text-transform: uppercase; letter-spacing: 0.5px; }
        .card-value { font-size: 28px; font-weight: 700; }
        .card-icon { width: 40px; height: 40px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 18px; }
        .card-icon.blue { background: rgba(99,102,241,0.15); color: var(--primary); }
        .card-icon.green { background: rgba(34,197,94,0.15); color: var(--success); }
        .card-icon.amber { background: rgba(245,158,11,0.15); color: var(--warning); }
        .card-icon.red { background: rgba(239,68,68,0.15); color: var(--danger); }
        .logs-container { background: var(--card); border: 1px solid var(--border); border-radius: 12px; padding: 16px; max-height: 400px; overflow-y: auto; font-family: 'JetBrains Mono', monospace; font-size: 13px; line-height: 1.7; }
        .log-entry { padding: 4px 0; border-bottom: 1px solid rgba(255,255,255,0.03); }
        .log-time { color: var(--muted); margin-right: 10px; }
        .log-info { color: var(--primary); }
        .log-success { color: var(--success); }
        .log-error { color: var(--danger); }
        .btn { padding: 8px 16px; border-radius: 8px; font-size: 14px; font-weight: 500; cursor: pointer; border: none; transition: all 0.15s; }
        .btn-primary { background: var(--primary); color: white; }
        .btn-primary:hover { background: var(--primary-hover); }
        .btn-outline { background: transparent; border: 1px solid var(--border); color: var(--text); }
        .btn-outline:hover { background: rgba(255,255,255,0.04); }
        .btn-danger { background: var(--danger); color: white; }
        .btn-sm { padding: 4px 10px; font-size: 12px; }
        .input-group { display: flex; gap: 8px; margin-bottom: 16px; }
        .input { flex: 1; padding: 10px 14px; background: var(--bg); border: 1px solid var(--border); border-radius: 8px; color: var(--text); font-size: 14px; outline: none; }
        .input:focus { border-color: var(--primary); }
        .badge { display: inline-block; padding: 3px 10px; border-radius: 20px; font-size: 11px; font-weight: 600; }
        .badge-success { background: rgba(34,197,94,0.15); color: var(--success); }
        .badge-warning { background: rgba(245,158,11,0.15); color: var(--warning); }
        .badge-danger { background: rgba(239,68,68,0.15); color: var(--danger); }
        .chat-area { background: var(--card); border: 1px solid var(--border); border-radius: 12px; display: flex; flex-direction: column; height: calc(100vh - 200px); }
        .chat-messages { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 12px; }
        .chat-msg { max-width: 80%; padding: 10px 16px; border-radius: 12px; font-size: 14px; line-height: 1.6; }
        .chat-msg.user { align-self: flex-end; background: var(--primary); color: white; border-bottom-right-radius: 4px; }
        .chat-msg.assistant { align-self: flex-start; background: #252836; color: var(--text); border-bottom-left-radius: 4px; }
        .chat-input-area { padding: 16px; border-top: 1px solid var(--border); display: flex; gap: 8px; }
        table { width: 100%; border-collapse: collapse; }
        th, td { padding: 12px 16px; text-align: left; border-bottom: 1px solid var(--border); font-size: 14px; }
        th { color: var(--muted); font-weight: 500; font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px; }
        @media (max-width: 768px) {
            .sidebar { display: none; }
            .main { margin-left: 0; }
            .cards { grid-template-columns: 1fr; }
        }
    </style>
</head>
<body>
    <aside class="sidebar">
        <div class="sidebar-header">
            <div class="logo">N</div>
            <span class="title">NEMAPI Bridge</span>
        </div>
        <nav class="nav">
            <button class="nav-item active" data-page="dashboard"><span class="icon">📊</span> Dashboard</button>
            <button class="nav-item" data-page="chat"><span class="icon">💬</span> Chat</button>
            <button class="nav-item" data-page="models"><span class="icon">🧩</span> Models</button>
            <button class="nav-item" data-page="config"><span class="icon">⚙️</span> Config</button>
            <button class="nav-item" data-page="logs"><span class="icon">📜</span> Logs</button>
        </nav>
        <div class="sidebar-footer">
            <div style="font-size:12px;color:var(--muted);">
                <span class="status-dot" id="statusDot"></span><span id="statusLabel">Checking...</span>
            </div>
            <div style="font-size:11px;color:var(--muted);margin-top:4px;">${ipAddress}:${port}</div>
        </div>
    </aside>
    <div class="main">
        <div class="topbar"><h2 id="pageTitle">Dashboard</h2><span id="clock" style="color:var(--muted);font-size:14px;"></span></div>
        <div class="content">
            <div id="page-dashboard" class="page active">
                <div class="cards">
                    <div class="card"><div class="card-header"><span class="card-title">Uptime</span><span class="card-icon blue">⏱</span></div><div class="card-value" id="uptime">--</div></div>
                    <div class="card"><div class="card-header"><span class="card-title">Requests</span><span class="card-icon green">📨</span></div><div class="card-value" id="reqCount">0</div></div>
                    <div class="card"><div class="card-header"><span class="card-title">Success Rate</span><span class="card-icon amber">📈</span></div><div class="card-value" id="successRate">--</div></div>
                    <div class="card"><div class="card-header"><span class="card-title">Active Jobs</span><span class="card-icon red">⚡</span></div><div class="card-value" id="activeJobs">0</div></div>
                </div>
                <div class="card" style="margin-top:16px;">
                    <div class="card-header"><span class="card-title">Recent Activity</span></div>
                    <div id="recentActivity" style="color:var(--muted);font-size:14px;">No recent activity</div>
                </div>
            </div>
            <div id="page-chat" class="page">
                <div class="chat-area">
                    <div class="chat-messages" id="chatMessages"><div class="chat-msg assistant">Hello! I am NEMAPI Bridge, your local AI API gateway. Type a message to begin.</div></div>
                    <div class="chat-input-area">
                        <input type="text" class="input" id="chatInput" placeholder="Type your message..." onkeydown="if(event.key==='Enter')sendChat()">
                        <button class="btn btn-primary" onclick="sendChat()">Send</button>
                    </div>
                </div>
            </div>
            <div id="page-models" class="page">
                <div class="card">
                    <div class="card-header"><span class="card-title">Available Models</span><button class="btn btn-outline btn-sm" onclick="loadModels()">Refresh</button></div>
                    <table><thead><tr><th>ID</th><th>Object</th><th>Owner</th></tr></thead><tbody id="modelsTable"><tr><td colspan="3" style="color:var(--muted);">Loading...</td></tr></tbody></table>
                </div>
            </div>
            <div id="page-config" class="page">
                <div class="card" style="margin-bottom:16px;">
                    <div class="card-header"><span class="card-title">Server Configuration</span></div>
                    <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;">
                        <div><span style="color:var(--muted);">IP Address</span><br><strong>${ipAddress}</strong></div>
                        <div><span style="color:var(--muted);">Port</span><br><strong>${port}</strong></div>
                        <div><span style="color:var(--muted);">Endpoint OpenAI</span><br><code style="color:var(--primary);">/v1/chat/completions</code></div>
                        <div><span style="color:var(--muted);">Endpoint Models</span><br><code style="color:var(--primary);">/v1/models</code></div>
                    </div>
                </div>
                <div class="card">
                    <div class="card-header"><span class="card-title">Calibration</span><span class="badge" id="calibBadge">Unknown</span></div>
                    <p style="color:var(--muted);font-size:14px;">Calibration is managed on the Android device via the floating button overlay.</p>
                </div>
            </div>
            <div id="page-logs" class="page">
                <div class="card" style="margin-bottom:16px;">
                    <div class="card-header"><span class="card-title">System Logs</span><button class="btn btn-outline btn-sm" onclick="clearLogs()">Clear</button></div>
                    <div class="logs-container" id="logsContainer"><div class="log-entry"><span class="log-time">--:--:--</span><span class="log-info">System started</span></div></div>
                </div>
            </div>
        </div>
    </div>
    <script>
        const BASE = window.location.origin;
        let reqCount = 0;
        let startTime = Date.now();
        const logs = [];

        function $(id) { return document.getElementById(id); }

        document.querySelectorAll('.nav-item').forEach(btn => {
            btn.addEventListener('click', () => {
                document.querySelectorAll('.nav-item').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                const page = btn.dataset.page;
                document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
                $('page-' + page).classList.add('active');
                $('pageTitle').textContent = btn.textContent.trim();
                if (page === 'models') loadModels();
                if (page === 'dashboard') refreshDashboard();
            });
        });

        function addLog(msg, type) {
            const time = new Date().toLocaleTimeString();
            logs.unshift({ time, msg, type });
            if (logs.length > 50) logs.pop();
            renderLogs();
        }

        function renderLogs() {
            const container = $('logsContainer');
            container.innerHTML = logs.map(l => '<div class="log-entry"><span class="log-time">' + l.time + '</span><span class="log-' + l.type + '">' + l.msg + '</span></div>').join('');
        }

        function clearLogs() { logs.length = 0; renderLogs(); addLog('Logs cleared', 'info'); }

        async function checkStatus() {
            try {
                const r = await fetch(BASE + '/status');
                const status = await r.text();
                const dot = $('statusDot');
                const label = $('statusLabel');
                if (status.includes('Ready')) {
                    dot.className = 'status-dot online';
                    label.textContent = 'Online';
                    $('calibBadge').textContent = 'Ready';
                    $('calibBadge').className = 'badge badge-success';
                } else {
                    dot.className = 'status-dot offline';
                    label.textContent = 'Service Disabled';
                    $('calibBadge').textContent = 'Not Calibrated';
                    $('calibBadge').className = 'badge badge-danger';
                }
            } catch(e) {}
        }

        async function loadModels() {
            try {
                const r = await fetch(BASE + '/v1/models');
                const data = await r.json();
                const tbody = $('modelsTable');
                tbody.innerHTML = data.data.map(m => '<tr><td><strong>' + m.id + '</strong></td><td>' + m.object + '</td><td>' + m.owned_by + '</td></tr>').join('');
            } catch(e) {
                $('modelsTable').innerHTML = '<tr><td colspan="3" style="color:var(--danger);">Failed to load models</td></tr>';
            }
        }

        function refreshDashboard() {
            const uptimeSec = Math.floor((Date.now() - startTime) / 1000);
            const d = Math.floor(uptimeSec / 86400);
            const h = Math.floor((uptimeSec % 86400) / 3600);
            const m = Math.floor((uptimeSec % 3600) / 60);
            $('uptime').textContent = d + 'd ' + h + 'h ' + m + 'm';
            $('reqCount').textContent = reqCount;
            $('successRate').textContent = reqCount > 0 ? '100%' : '--';
        }

        async function sendChat() {
            const input = $('chatInput');
            const text = input.value.trim();
            if (!text) return;
            const messagesDiv = $('chatMessages');
            messagesDiv.innerHTML += '<div class="chat-msg user">' + text.replace(/</g,'&lt;') + '</div>';
            input.value = '';
            messagesDiv.innerHTML += '<div class="chat-msg assistant" id="waitingMsg"><em>Thinking...</em></div>';
            messagesDiv.scrollTop = messagesDiv.scrollHeight;
            reqCount++;
            addLog('Chat request sent', 'info');
            refreshDashboard();
            try {
                const r = await fetch(BASE + '/ask?q=' + encodeURIComponent(text));
                const jobId = await r.text();
                let result = null;
                for (let i = 0; i < 200; i++) {
                    await new Promise(resolve => setTimeout(resolve, 3000));
                    const rr = await fetch(BASE + '/result?id=' + jobId);
                    const rt = await rr.text();
                    if (rt !== 'STILL_WORKING') { result = rt; break; }
                }
                const waiting = $('waitingMsg');
                if (waiting) waiting.remove();
                if (result) {
                    messagesDiv.innerHTML += '<div class="chat-msg assistant">' + result.replace(/</g,'&lt;').replace(/\n/g,'<br>') + '</div>';
                    addLog('Response received', 'success');
                } else {
                    messagesDiv.innerHTML += '<div class="chat-msg assistant"><em>Timeout - still working, check /result?id=' + jobId + '</em></div>';
                    addLog('Timeout waiting for response', 'error');
                }
            } catch(e) {
                const waiting = $('waitingMsg');
                if (waiting) waiting.remove();
                messagesDiv.innerHTML += '<div class="chat-msg assistant"><em>Error: ' + e.message.replace(/</g,'&lt;') + '</em></div>';
                addLog('Error: ' + e.message, 'error');
            }
            messagesDiv.scrollTop = messagesDiv.scrollHeight;
        }

        function updateClock() {
            $('clock').textContent = new Date().toLocaleTimeString();
        }

        setInterval(checkStatus, 5000);
        setInterval(refreshDashboard, 10000);
        setInterval(updateClock, 1000);
        checkStatus();
        refreshDashboard();
        updateClock();
        addLog('NEMAPI Bridge admin panel loaded', 'info');
    </script>
</body>
</html>"""
    }
}
