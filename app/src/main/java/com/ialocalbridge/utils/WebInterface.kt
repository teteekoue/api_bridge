package com.ialocalbridge.utils

object WebInterface {
    fun getHtml(ipAddress: String, port: Int): String {
        return """<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DS Free API - Admin Panel</title>
    <link rel="icon" type="image/svg+xml" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>N</text></svg>">
    <style>
        *,*::before,*::after{box-sizing:border-box;margin:0;padding:0}
        :root{
            --background:#0f1117;--foreground:#e1e4ed;--card:#1a1d27;--card-foreground:#e1e4ed;
            --primary:#6366f1;--primary-foreground:#ffffff;--muted:#252836;--muted-foreground:#8b8fa3;
            --border:#2a2d3a;--ring:#6366f1;--radius:0.75rem;--destructive:#ef4444;
            --success:#22c55e;--warning:#f59e0b;--sidebar:#1a1d27;--sidebar-foreground:#e1e4ed;
            --sidebar-primary:#6366f1;--sidebar-accent:#252836;--font-sans:'Inter',system-ui,-apple-system,sans-serif;
        }
        body{font-family:var(--font-sans);background:var(--background);color:var(--foreground);display:flex;min-height:100vh;line-height:1.5;font-size:14px}
        .sidebar{width:220px;background:var(--sidebar);border-right:1px solid var(--border);display:flex;flex-direction:column;position:fixed;top:0;left:0;bottom:0;z-index:100}
        .sidebar-header{display:flex;align-items:center;gap:10px;padding:16px;border-bottom:1px solid var(--border)}
        .sidebar-header .logo{width:28px;height:28px;background:var(--primary);border-radius:6px;display:flex;align-items:center;justify-content:center;font-weight:700;font-size:14px;color:#fff}
        .sidebar-header .title{font-weight:600;font-size:14px}
        .nav{flex:1;padding:8px;display:flex;flex-direction:column;gap:2px}
        .nav-item{display:flex;align-items:center;gap:10px;padding:8px 12px;border-radius:6px;cursor:pointer;color:var(--muted-foreground);font-size:13px;font-weight:500;transition:all 0.15s;border:none;background:none;width:100%;text-align:left}
        .nav-item:hover{background:#ffffff08;color:var(--foreground)}
        .nav-item.active{background:#6366f11a;color:var(--primary)}
        .nav-item svg{width:16px;height:16px;flex-shrink:0}
        .sidebar-footer{padding:12px;border-top:1px solid var(--border);font-size:12px;color:var(--muted-foreground)}
        .status-dot{width:6px;height:6px;border-radius:50%;display:inline-block;margin-right:6px}
        .status-dot.online{background:var(--success)}
        .status-dot.offline{background:var(--destructive)}
        .main{margin-left:220px;flex:1;display:flex;flex-direction:column;min-width:0}
        .topbar{display:flex;align-items:center;justify-content:space-between;padding:12px 24px;border-bottom:1px solid var(--border);background:var(--background);position:sticky;top:0;z-index:50}
        .topbar h2{font-size:16px;font-weight:600}
        .content{flex:1;padding:24px;overflow-y:auto}
        .page{display:none}
        .page.active{display:block}
        .card{background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:20px;margin-bottom:16px}
        .card-header{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px}
        .card-title{font-size:12px;font-weight:600;color:var(--muted-foreground);text-transform:uppercase;letter-spacing:0.5px}
        .card-value{font-size:26px;font-weight:700;color:var(--foreground)}
        .card-icon{width:36px;height:36px;border-radius:8px;display:flex;align-items:center;justify-content:center;flex-shrink:0}
        .card-icon.blue{background:#6366f11a;color:var(--primary)}
        .card-icon.green{background:#22c55e1a;color:var(--success)}
        .card-icon.amber{background:#f59e0b1a;color:var(--warning)}
        .card-icon.red{background:#ef44441a;color:var(--destructive)}
        .stats-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:16px;margin-bottom:24px}
        .stat-card{background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:16px;display:flex;align-items:flex-start;justify-content:space-between}
        .stat-info{display:flex;flex-direction:column;gap:4px}
        .btn{display:inline-flex;align-items:center;gap:6px;padding:7px 14px;border-radius:6px;font-size:13px;font-weight:500;cursor:pointer;border:1px solid var(--border);background:var(--card);color:var(--foreground);transition:all 0.15s;font-family:inherit}
        .btn:hover{background:var(--muted)}
        .btn-primary{background:var(--primary);color:var(--primary-foreground);border-color:var(--primary)}
        .btn-primary:hover{background:#5558e6}
        .btn-sm{padding:4px 10px;font-size:12px}
        .btn-danger{background:var(--destructive);color:#fff;border-color:var(--destructive)}
        .btn-ghost{background:transparent;border-color:transparent;color:var(--muted-foreground)}
        .btn-ghost:hover{background:var(--muted);color:var(--foreground)}
        table{width:100%;border-collapse:collapse}
        th{text-align:left;padding:10px 14px;font-size:11px;font-weight:600;color:var(--muted-foreground);text-transform:uppercase;letter-spacing:0.5px;border-bottom:1px solid var(--border)}
        td{padding:10px 14px;font-size:13px;border-bottom:1px solid var(--border)}
        tr:last-child td{border-bottom:none}
        .badge{display:inline-flex;align-items:center;padding:2px 8px;border-radius:20px;font-size:11px;font-weight:600;border:1px solid}
        .badge-success{background:#22c55e1a;color:var(--success);border-color:#22c55e33}
        .badge-warning{background:#f59e0b1a;color:var(--warning);border-color:#f59e0b33}
        .badge-danger{background:#ef44441a;color:var(--destructive);border-color:#ef444433}
        .badge-info{background:#6366f11a;color:var(--primary);border-color:#6366f133}
        .badge-outline{background:transparent;color:var(--muted-foreground);border-color:var(--border)}
        .input{width:100%;padding:8px 12px;background:var(--background);border:1px solid var(--border);border-radius:6px;color:var(--foreground);font-size:13px;font-family:inherit;outline:none}
        .input:focus{border-color:var(--ring);box-shadow:0 0 0 2px #6366f120}
        .input-group{display:flex;gap:8px}
        .separator{height:1px;background:var(--border);margin:16px 0}
        .text-muted{color:var(--muted-foreground);font-size:12px}
        .code-block{background:var(--muted);border:1px solid var(--border);border-radius:6px;padding:12px 16px;font-family:'JetBrains Mono','Fira Code',monospace;font-size:12px;color:var(--foreground);overflow-x:auto;white-space:pre-wrap;word-break:break-all}
        .flex-row{display:flex;align-items:center;gap:8px}
        .grid-2{display:grid;grid-template-columns:1fr 1fr;gap:12px}
        .mb-4{margin-bottom:16px}
        .mb-2{margin-bottom:8px}
        .mt-2{margin-top:8px}
        .chat-container{border:1px solid var(--border);border-radius:var(--radius);background:var(--card);display:flex;flex-direction:column;height:calc(100vh - 180px);min-height:400px}
        .chat-messages{flex:1;overflow-y:auto;padding:20px;display:flex;flex-direction:column;gap:12px}
        .chat-msg{max-width:82%;padding:10px 14px;border-radius:10px;font-size:13px;line-height:1.6}
        .chat-msg.user{align-self:flex-end;background:var(--primary);color:#fff;border-bottom-right-radius:3px}
        .chat-msg.assistant{align-self:flex-start;background:var(--muted);color:var(--foreground);border-bottom-left-radius:3px}
        .chat-input-area{padding:12px 16px;border-top:1px solid var(--border);display:flex;gap:8px}
        .log-viewer{background:var(--muted);border:1px solid var(--border);border-radius:var(--radius);padding:16px;max-height:400px;overflow-y:auto;font-family:'JetBrains Mono','Fira Code',monospace;font-size:12px;line-height:1.8;color:var(--foreground)}
        .log-entry{display:flex;gap:12px;padding:2px 0}
        .log-time{color:var(--muted-foreground);flex-shrink:0}
        .log-info{color:var(--primary)}
        .log-success{color:var(--success)}
        .log-error{color:var(--destructive)}
        .log-warn{color:var(--warning)}
        @media(max-width:768px){.sidebar{display:none}.main{margin-left:0}}
    </style>
</head>
<body>
<aside class="sidebar">
    <div class="sidebar-header">
        <div class="logo">N</div>
        <span class="title">DS Free API</span>
    </div>
    <nav class="nav">
        <button class="nav-item active" data-page="dashboard">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>
            Dashboard
        </button>
        <button class="nav-item" data-page="models">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/></svg>
            Models
        </button>
        <button class="nav-item" data-page="config">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
            Config
        </button>
        <button class="nav-item" data-page="logs">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
            Logs
        </button>
    </nav>
    <div class="sidebar-footer">
        <div class="flex-row mb-2"><span class="status-dot" id="statusDot"></span><span id="statusLabel">Checking...</span></div>
        <div class="text-muted">${ipAddress}:${port}</div>
    </div>
</aside>
<div class="main">
    <div class="topbar"><h2 id="pageTitle">Dashboard</h2><span class="text-muted" id="clock"></span></div>
    <div class="content">
        <div id="page-dashboard" class="page active">
            <div class="stats-grid">
                <div class="stat-card"><div class="stat-info"><span class="card-title">Uptime</span><span class="card-value" id="uptime">--</span></div><div class="card-icon blue"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg></div></div>
                <div class="stat-card"><div class="stat-info"><span class="card-title">Total Requests</span><span class="card-value" id="reqCount">0</span></div><div class="card-icon green"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg></div></div>
                <div class="stat-card"><div class="stat-info"><span class="card-title">Status</span><span class="card-value" id="apiStatus">--</span></div><div class="card-icon amber"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg></div></div>
                <div class="stat-card"><div class="stat-info"><span class="card-title">Endpoint</span><span class="card-value" style="font-size:16px;">/v1</span></div><div class="card-icon red"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="2" y1="12" x2="22" y2="12"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg></div></div>
            </div>
            <div class="card">
                <div class="card-header"><span class="card-title">Quick Test</span></div>
                <div class="input-group mb-2"><input class="input" id="quickPrompt" placeholder="Type a test prompt..." onkeydown="if(event.key==='Enter')quickTest()"><button class="btn btn-primary" onclick="quickTest()">Send</button></div>
                <div id="quickResult" class="code-block" style="display:none;margin-top:8px;"></div>
            </div>
        </div>
        <div id="page-models" class="page">
            <div class="card">
                <div class="card-header"><span class="card-title">Available Models</span><button class="btn btn-sm" onclick="loadModels()">Refresh</button></div>
                <table><thead><tr><th>ID</th><th>Object</th><th>Owned By</th><th>Created</th></tr></thead><tbody id="modelsTable"><tr><td colspan="4" class="text-muted">Loading...</td></tr></tbody></table>
            </div>
        </div>
        <div id="page-config" class="page">
            <div class="card">
                <div class="card-header"><span class="card-title">Server Configuration</span></div>
                <div class="grid-2">
                    <div><span class="card-title mb-2">Host</span><div class="code-block">${ipAddress}</div></div>
                    <div><span class="card-title mb-2">Port</span><div class="code-block">${port}</div></div>
                </div>
                <div class="separator"></div>
                <span class="card-title mb-2">API Endpoints</span>
                <div class="code-block">POST /v1/chat/completions
GET  /v1/models
GET  /status
POST /ask</div>
            </div>
            <div class="card">
                <div class="card-header"><span class="card-title">Connection String</span></div>
                <div class="code-block">curl http://${ipAddress}:${port}/v1/chat/completions -H "Content-Type: application/json" -d '{"model":"deepseek-chat","messages":[{"role":"user","content":"Hello"}]}'</div>
            </div>
            <div class="card">
                <div class="card-header"><span class="card-title">Calibration Status</span><span id="calibBadge" class="badge badge-outline">Unknown</span></div>
                <p class="text-muted">Calibration is managed on-device via the floating button overlay.</p>
            </div>
        </div>
        <div id="page-logs" class="page">
            <div class="card">
                <div class="card-header"><span class="card-title">System Logs</span><div class="flex-row"><button class="btn btn-sm" onclick="clearLogs()">Clear</button><button class="btn btn-sm" onclick="exportLogs()">Export</button></div></div>
                <div class="log-viewer" id="logsContainer"><div class="log-entry"><span class="log-time">--:--:--</span><span class="log-info">System started. Admin panel loaded.</span></div></div>
            </div>
        </div>
    </div>
</div>
<script>
const BASE = window.location.origin;
let reqCount = 0;
let startTime = Date.now();
const logs = [];

function E(id){return document.getElementById(id)}

document.querySelectorAll('.nav-item').forEach(btn=>{
    btn.addEventListener('click',()=>{
        document.querySelectorAll('.nav-item').forEach(b=>b.classList.remove('active'));
        btn.classList.add('active');
        const page=btn.dataset.page;
        document.querySelectorAll('.page').forEach(p=>p.classList.remove('active'));
        E('page-'+page).classList.add('active');
        E('pageTitle').textContent=btn.textContent.trim();
        if(page==='models')loadModels();
        if(page==='dashboard')refreshDashboard();
    })
});

function addLog(msg,type){
    const time=new Date().toLocaleTimeString();
    logs.unshift({time,msg,type});
    if(logs.length>100)logs.pop();
    renderLogs()
}
function renderLogs(){
    E('logsContainer').innerHTML=logs.map(l=>'<div class="log-entry"><span class="log-time">'+l.time+'</span><span class="log-'+l.type+'">'+l.msg.replace(/</g,'&lt;')+'</span></div>').join('')
}
function clearLogs(){logs.length=0;renderLogs();addLog('Logs cleared','info')}
function exportLogs(){
    const text=logs.map(l=>'['+l.time+'] ['+l.type.toUpperCase()+'] '+l.msg).join('\n');
    const blob=new Blob([text],{type:'text/plain'});
    const a=document.createElement('a');a.href=URL.createObjectURL(blob);a.download='nemapi-logs.txt';a.click()
}

async function checkStatus(){
    try{
        const r=await fetch(BASE+'/status');
        const status=await r.text();
        const dot=E('statusDot'),label=E('statusLabel'),calib=E('calibBadge'),apiSt=E('apiStatus');
        if(status.includes('Ready')){
            dot.className='status-dot online';label.textContent='Online';calib.textContent='Ready';calib.className='badge badge-success';apiSt.textContent='Ready'
        }else{
            dot.className='status-dot offline';label.textContent='Disabled';calib.textContent='Not Calibrated';calib.className='badge badge-danger';apiSt.textContent='Disabled'
        }
    }catch(e){E('apiStatus').textContent='Offline'}
}

async function loadModels(){
    try{
        const r=await fetch(BASE+'/v1/models');
        const data=await r.json();
        E('modelsTable').innerHTML=data.data.map(m=>'<tr><td><strong>'+m.id+'</strong></td><td>'+m.object+'</td><td>'+m.owned_by+'</td><td>'+new Date(m.created*1000).toLocaleDateString()+'</td></tr>').join('')
    }catch(e){E('modelsTable').innerHTML='<tr><td colspan="4" style="color:var(--destructive)">Failed to load models</td></tr>'}
}

function refreshDashboard(){
    const s=Math.floor((Date.now()-startTime)/1000);
    const d=Math.floor(s/86400),h=Math.floor((s%86400)/3600),m=Math.floor((s%3600)/60);
    E('uptime').textContent=d+'d '+h+'h '+m+'m';
    E('reqCount').textContent=reqCount
}

function updateClock(){E('clock').textContent=new Date().toLocaleTimeString()}

async function quickTest(){
    const input=E('quickPrompt'),text=input.value.trim();
    if(!text)return;
    const resultDiv=E('quickResult');
    resultDiv.style.display='block';
    resultDiv.textContent='Sending...';
    input.disabled=true;
    reqCount++;refreshDashboard();
    addLog('API test: '+text.substring(0,50),'info');
    try{
        const r=await fetch(BASE+'/v1/chat/completions',{
            method:'POST',headers:{'Content-Type':'application/json'},
            body:JSON.stringify({model:'deepseek-chat',messages:[{role:'user',content:text}]})
        });
        const data=await r.json();
        if(data.choices&&data.choices[0]){
            const content=data.choices[0].message.content;
            resultDiv.textContent=content;
            addLog('Response received ('+content.length+' chars)','success')
        }else if(data.error){
            resultDiv.textContent='Error: '+data.error;
            addLog('API error: '+data.error,'error')
        }
    }catch(e){
        resultDiv.textContent='Error: '+e.message;
        addLog('Error: '+e.message,'error')
    }
    input.disabled=false;input.focus()
}

setInterval(checkStatus,5000);
setInterval(refreshDashboard,10000);
setInterval(updateClock,1000);
checkStatus();refreshDashboard();updateClock();
addLog('NEMAPI Bridge admin panel ready','info');
</script>
</body>
</html>"""
    }
}
