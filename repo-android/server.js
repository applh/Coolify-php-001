import http from 'http';
import { spawn } from 'child_process';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const PORT = 3000;
const APK_RELATIVE_PATH = path.join('app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk');

// Helper to format the downloaded filename with timestamp
function getFormattedFilename() {
  const apkFullPath = path.join(process.cwd(), APK_RELATIVE_PATH);
  try {
    if (fs.existsSync(apkFullPath)) {
      const stats = fs.statSync(apkFullPath);
      const date = stats.mtime;
      const YY = String(date.getFullYear()).slice(-2);
      const mm = String(date.getMonth() + 1).padStart(2, '0');
      const dd = String(date.getDate()).padStart(2, '0');
      const H = String(date.getHours()).padStart(2, '0');
      const M = String(date.getMinutes()).padStart(2, '0');
      const S = String(date.getSeconds()).padStart(2, '0');
      return `fraise-${YY}${mm}${dd}-${H}${M}${S}.apk`;
    }
  } catch (err) {
    // Ignore error and fall through to default fallback
  }
  return 'fraise-app.apk';
}

// In-Memory state
let buildState = {
  status: 'idle', // 'idle' | 'building' | 'success' | 'failed'
  startTime: null,
  endTime: null,
  error: null,
  logs: [] // Keeps recent logs for terminal refresh
};

// Queue of streaming connections
const logSubscribers = new Set();

// Ensure gradlew has execute permission
function ensureGradlewExecutable() {
  const gradlewPath = path.join(process.cwd(), 'gradlew');
  if (fs.existsSync(gradlewPath)) {
    try {
      fs.chmodSync(gradlewPath, '755');
      appendLog('[System] Verified and set executable permissions on gradlew');
    } catch (err) {
      appendLog(`[System Warning] Failed to chmod gradlew: ${err.message}`);
    }
  } else {
    appendLog('[System Warning] gradlew script was not found in working directory!');
  }
}

// Function to append logs and broadcast to connected SSE clients
function appendLog(message) {
  const formattedMsg = typeof message === 'string' ? message : message.toString();
  const rawLines = formattedMsg.split(/\r?\n/);
  
  rawLines.forEach(line => {
    // Basic scrubbing of ANSI escape characters if any
    const cleanLine = line.replace(/[\u001b\u009b][[()#;?]*(?:[0-9]{1,4}(?:;[0-9]{0,4})*)?[0-9A-ORZcf-nqry=><]/g, '').trimRight();
    if (!cleanLine && line) return; // Skip blank lines created by scrub but keep original structure
    
    const logObj = {
      timestamp: new Date().toISOString(),
      message: cleanLine
    };
    
    buildState.logs.push(logObj);
    // Max log threshold 2000 to prevent OOM
    if (buildState.logs.length > 2000) {
      buildState.logs.shift();
    }
    
    // Broadcast to subscribers
    const payload = JSON.stringify({ type: 'log', data: logObj });
    logSubscribers.forEach(res => {
      res.write(`data: ${payload}\n\n`);
    });
  });
}

// Broadcast general state transitions to clients
function broadcastState() {
  const payload = JSON.stringify({
    type: 'state',
    data: {
      status: buildState.status,
      startTime: buildState.startTime,
      endTime: buildState.endTime,
      error: buildState.error,
      apkExists: fs.existsSync(path.join(process.cwd(), APK_RELATIVE_PATH)),
      filename: getFormattedFilename()
    }
  });
  logSubscribers.forEach(res => {
    res.write(`data: ${payload}\n\n`);
  });
}

// Triggers the non-daemon Gradle build
function triggerBuild() {
  if (buildState.status === 'building') return false;

  buildState.status = 'building';
  buildState.startTime = new Date().toISOString();
  buildState.endTime = null;
  buildState.error = null;
  buildState.logs = [];

  ensureGradlewExecutable();
  broadcastState();

  appendLog('[Build Server] Initializing isolated Android APK build worker...');
  appendLog('[Build Server] Executing Command: ./gradlew assembleDebug --no-daemon --max-workers=1');

  // Launch gradle process containing build parameters
  const gradleProcess = spawn('./gradlew', ['assembleDebug', '--no-daemon', '--max-workers=1'], {
    cwd: process.cwd(),
    shell: true,
    env: { ...process.env, GRADLE_OPTS: '-Dorg.gradle.daemon=false -Dorg.gradle.parallel=false -Dorg.gradle.jvmargs="-Xmx3072m"' }
  });

  gradleProcess.stdout.on('data', (data) => {
    appendLog(data.toString());
  });

  gradleProcess.stderr.on('data', (data) => {
    appendLog(`[ERROR] ${data.toString()}`);
  });

  gradleProcess.on('close', (code) => {
    buildState.endTime = new Date().toISOString();
    if (code === 0) {
      buildState.status = 'success';
      appendLog('[Build Server] 🎉 Build SUCCESSFUL! APK is compiled and ready for deployment.');
    } else {
      buildState.status = 'failed';
      buildState.error = `Gradle completed with exit code ${code}`;
      appendLog(`[Build Server] ❌ Build FAILED with exit code: ${code}`);
    }
    broadcastState();
  });

  gradleProcess.on('error', (err) => {
    buildState.status = 'failed';
    buildState.endTime = new Date().toISOString();
    buildState.error = err.message;
    appendLog(`[Build Server] ❌ Executor Error: ${err.message}`);
    broadcastState();
  });

  return true;
}

// HTTP request handler
const server = http.createServer((req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`);
  const method = req.method;

  // 1. Direct APK Download Route
  if (url.pathname === '/download') {
    const apkFullPath = path.join(process.cwd(), APK_RELATIVE_PATH);
    if (fs.existsSync(apkFullPath)) {
      const filename = getFormattedFilename();
      res.writeHead(200, {
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Disposition': `attachment; filename="${filename}"`,
        'Cache-Control': 'no-store, no-cache, must-revalidate, private'
      });
      const fileStream = fs.createReadStream(apkFullPath);
      fileStream.pipe(res);
    } else {
      res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('APK has not been built yet. Please start a compilation build on the main dashboard.');
    }
    return;
  }

  // 2. Trigger Build Route
  if (url.pathname === '/build' && method === 'POST') {
    if (buildState.status === 'building') {
      res.writeHead(409, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'A compilation is already in progress.' }));
      return;
    }
    const started = triggerBuild();
    res.writeHead(started ? 202 : 500, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: started ? 'started' : 'failed' }));
    return;
  }

  // 3. Status Route
  if (url.pathname === '/build/status' && method === 'GET') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      status: buildState.status,
      startTime: buildState.startTime,
      endTime: buildState.endTime,
      error: buildState.error,
      apkExists: fs.existsSync(path.join(process.cwd(), APK_RELATIVE_PATH)),
      filename: getFormattedFilename()
    }));
    return;
  }

  // 4. Server Sent Events (SSE) Live Log Stream Route
  if (url.pathname === '/build/stream') {
    res.writeHead(200, {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache, no-transform',
      'Connection': 'keep-alive',
      'X-Accel-Buffering': 'no'
    });

    // Write current state and logs immediately as initial payload
    res.write(`data: ${JSON.stringify({
      type: 'init',
      data: {
        status: buildState.status,
        startTime: buildState.startTime,
        endTime: buildState.endTime,
        error: buildState.error,
        logs: buildState.logs,
        apkExists: fs.existsSync(path.join(process.cwd(), APK_RELATIVE_PATH)),
        filename: getFormattedFilename()
      }
    })}\n\n`);

    logSubscribers.add(res);

    req.on('close', () => {
      logSubscribers.delete(res);
    });
    return;
  }

  // 5. Root Admin & Live Diagnostic View
  if (url.pathname === '/' && method === 'GET') {
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(`<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Companion Android Build Server</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <script src="https://cdnjs.cloudflare.com/ajax/libs/qrious/4.0.2/qrious.min.js"></script>
  <style>
    /* Custom refined scrollbars */
    ::-webkit-scrollbar { width: 6px; height: 6px; }
    ::-webkit-scrollbar-track { background: #18181b; }
    ::-webkit-scrollbar-thumb { background: #3f3f46; border-radius: 3px; }
    ::-webkit-scrollbar-thumb:hover { background: #52525b; }
  </style>
</head>
<body class="bg-[#0b0c10] text-slate-100 min-h-screen font-sans flex flex-col antialiased selection:bg-teal-500 selection:text-black">

  <!-- Header -->
  <header class="border-b border-zinc-800 bg-[#0d0e12]/80 backdrop-blur-md sticky top-0 z-10">
    <div class="max-w-6xl mx-auto px-6 py-4 flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="h-9 w-9 rounded-lg bg-teal-500/10 flex items-center justify-center border border-teal-500/20">
          <svg class="h-5 w-5 text-teal-400" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
          </svg>
        </div>
        <div>
          <h1 class="text-md font-semibold tracking-wider uppercase text-zinc-300">Android Companion App</h1>
          <p class="text-xs text-zinc-500">Local Build & Diagnostics Daemon</p>
        </div>
      </div>
      <div class="flex items-center gap-2">
        <span class="inline-flex h-2 w-2 rounded-full bg-teal-500 animate-pulse"></span>
        <span class="text-xs text-zinc-400 font-mono">DOCKER NODE</span>
      </div>
    </div>
  </header>

  <!-- Main Container -->
  <main class="flex-1 max-w-6xl w-full mx-auto p-6 grid grid-cols-1 lg:grid-cols-3 gap-6">

    <!-- Controls Column -->
    <div class="lg:col-span-1 flex flex-col gap-6">
      
      <!-- Status Card -->
      <div class="bg-zinc-900/50 p-6 rounded-xl border border-zinc-800/80 backdrop-blur">
        <h2 class="text-sm font-semibold text-zinc-400 uppercase tracking-widest mb-4">Compilation Status</h2>
        <div class="flex items-center justify-between mb-4">
          <span class="text-zinc-500 text-sm">Active State:</span>
          <span id="badge" class="px-2.5 py-1 rounded-full text-xs font-semibold uppercase tracking-wider bg-zinc-800 text-zinc-400">
            Checking...
          </span>
        </div>
        
        <div class="space-y-2 mb-6 text-xs text-zinc-500 font-mono">
          <div class="flex justify-between">
            <span>Started:</span>
            <span id="startTime" class="text-zinc-400">-</span>
          </div>
          <div class="flex justify-between">
            <span>Finished:</span>
            <span id="endTime" class="text-zinc-400">-</span>
          </div>
        </div>

        <button id="btnBuild" class="w-full bg-zinc-800 border border-zinc-700 text-zinc-300 font-semibold py-2.5 px-4 rounded-lg hover:bg-zinc-700 hover:text-white transition duration-200 disabled:opacity-40 disabled:cursor-not-allowed">
          Compile APK
        </button>
      </div>

      <!-- Download & QR Code -->
      <div class="bg-zinc-900/50 p-6 rounded-xl border border-zinc-800/80 backdrop-blur flex flex-col items-center">
        <h3 class="text-sm font-semibold text-zinc-400 uppercase tracking-widest w-full text-left mb-4">Serve Package</h3>
        
        <!-- Live QR Placeholder -->
        <div class="relative bg-zinc-950 p-4 rounded-lg border border-zinc-800 flex items-center justify-center h-48 w-48 mb-4">
          <canvas id="qrCode" class="opacity-30 transition-opacity duration-300"></canvas>
          <div id="qrLockout" class="absolute inset-0 bg-zinc-950/90 text-center flex flex-col items-center justify-center p-4">
            <svg class="h-8 w-8 text-zinc-600 mb-2 animate-bounce" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
            </svg>
            <p class="text-xs text-zinc-500">Compile a successful build to generate device installer code</p>
          </div>
        </div>

        <a id="btnDownload" href="/download" class="w-full text-center bg-teal-600 hover:bg-teal-500 text-zinc-950 font-bold py-2.5 px-4 rounded-lg transition duration-200 pointer-events-none opacity-40">
          Download app.apk
        </a>
      </div>
    </div>

    <!-- Live Console Terminal -->
    <div class="lg:col-span-2 flex flex-col bg-zinc-950 border border-zinc-800/80 rounded-xl overflow-hidden min-h-[500px]">
      <div class="bg-zinc-900/80 shrink-0 px-4 py-3 border-b border-zinc-800 flex items-center justify-between">
        <div class="flex items-center gap-2">
          <span class="h-2.5 w-2.5 rounded-full bg-red-500"></span>
          <span class="h-2.5 w-2.5 rounded-full bg-yellow-500"></span>
          <span class="h-2.5 w-2.5 rounded-full bg-green-500"></span>
          <span class="text-xs text-zinc-500 font-mono ml-2">gradlew console stream</span>
        </div>
        <div class="flex items-center gap-3">
          <button id="btnClear" class="text-xs text-zinc-500 hover:text-zinc-300 font-mono transition">CLEAR</button>
          <span class="h-4 w-px bg-zinc-800"></span>
          <label class="flex items-center gap-1.5 cursor-pointer selection:bg-transparent">
            <input type="checkbox" id="chkAutoScroll" checked class="accent-teal-500 rounded text-zinc-950">
            <span class="text-xs text-zinc-500 font-mono hover:text-zinc-400">AUTOSCROLL</span>
          </label>
        </div>
      </div>
      
      <!-- Output Shell -->
      <div id="terminal" class="flex-1 p-6 font-mono text-zinc-400 text-xs overflow-y-auto leading-relaxed space-y-1">
        <div class="text-zinc-600">[Service] Initializing diagnostics stream connection...</div>
      </div>
    </div>

  </main>

  <!-- Footer -->
  <footer class="border-t border-zinc-900 bg-zinc-950 py-4 px-6 text-center text-xs text-zinc-600">
    Companion Android Build Server Node | Process: ${process.pid}
  </footer>

  <!-- App Client Shell State -->
  <script>
    const term = document.getElementById('terminal');
    const badge = document.getElementById('badge');
    const btnBuild = document.getElementById('btnBuild');
    const btnDownload = document.getElementById('btnDownload');
    const btnClear = document.getElementById('btnClear');
    const chkAutoScroll = document.getElementById('chkAutoScroll');
    const startTimeEl = document.getElementById('startTime');
    const endTimeEl = document.getElementById('endTime');
    const qrCanvas = document.getElementById('qrCode');
    const qrLockout = document.getElementById('qrLockout');

    let lockScroll = true;

    // Helper functions
    function appendTerminal(line, isError = false) {
      const lineDiv = document.createElement('div');
      lineDiv.className = isError ? 'text-red-400' : 'text-zinc-300';
      
      // Auto wrap system descriptors nicely
      const cleanLine = line.replace(/^\\[Build Server\\]/, '<span class="text-teal-400 font-semibold">$0</span>')
                            .replace(/^\\[System\\]/, '<span class="text-zinc-500 font-semibold">$0</span>')
                            .replace(/^\\[ERROR\\]/, '<span class="text-red-500 font-semibold">$0</span>');

      lineDiv.innerHTML = \`<span class="text-zinc-700 select-none mr-2">$ {new Date().toLocaleTimeString()}</span>\${cleanLine}\`;
      term.appendChild(lineDiv);

      if (chkAutoScroll.checked) {
        term.scrollTop = term.scrollHeight;
      }
    }

    function updateBadge(status) {
      badge.textContent = status;
      badge.className = "px-2.5 py-1 rounded-full text-xs font-semibold uppercase tracking-wider";
      
      if (status === 'idle') {
        badge.classList.add('bg-zinc-800', 'text-zinc-400');
        btnBuild.disabled = false;
        btnBuild.textContent = 'Compile APK';
      } else if (status === 'building') {
        badge.classList.add('bg-amber-500/10', 'text-amber-400', 'border', 'border-amber-500/20');
        btnBuild.disabled = true;
        btnBuild.textContent = 'Building...';
      } else if (status === 'success') {
        badge.classList.add('bg-teal-500/10', 'text-teal-400', 'border', 'border-teal-500/20');
        btnBuild.disabled = false;
        btnBuild.textContent = 'Recompile APK';
      } else if (status === 'failed') {
        badge.classList.add('bg-red-500/10', 'text-red-400', 'border', 'border-red-500/20');
        btnBuild.disabled = false;
        btnBuild.textContent = 'Retry Build';
      }
    }

    function updateInstaller(apkExists, filename) {
      if (apkExists) {
        btnDownload.className = "w-full text-center bg-teal-600 hover:bg-teal-500 text-zinc-950 font-bold py-2.5 px-4 rounded-lg transition duration-200 shadow-lg shadow-teal-500/10 cursor-pointer block";
        btnDownload.style.pointerEvents = "auto";
        btnDownload.textContent = filename ? `Download ${filename}` : "Download app.apk";
        qrCanvas.style.opacity = "1";
        qrLockout.classList.add('hidden');
        
        // Dynamically compile device-local QR Code targeting their precise download endpoint
        const dlUrl = window.location.origin + '/download';
        new QRious({
          element: qrCanvas,
          value: dlUrl,
          size: 160,
          background: '#000000',
          foreground: '#2dd4bf',
          level: 'H'
        });
      } else {
        btnDownload.className = "w-full text-center bg-zinc-800 border border-zinc-700 text-zinc-500 font-bold py-2.5 px-4 rounded-lg transition duration-200 pointer-events-none";
        btnDownload.style.pointerEvents = "none";
        btnDownload.textContent = "Download app.apk";
        qrCanvas.style.opacity = "0.3";
        qrLockout.classList.remove('hidden');
      }
    }

    // Connect to Server Sent Events (SSE) Live Feed
    function connectSSE() {
      const evtSource = new EventSource("/build/stream");
      
      evtSource.onopen = () => {
        term.innerHTML = '<div class="text-zinc-600 font-mono">[Stream] Connected successfully to Android Build Server live channel.</div>';
      };

      evtSource.onerror = (err) => {
        console.error("SSE stream reconnecting:", err);
        const errDiv = document.createElement('div');
        errDiv.className = 'text-amber-500/70 font-mono my-2';
        errDiv.textContent = '[Stream Warning] SSE connection lost. Attempting background reconnect...';
        term.appendChild(errDiv);
      };

      evtSource.onmessage = (event) => {
        const payload = JSON.parse(event.data);
        const { type, data } = payload;

        if (type === 'init') {
          updateBadge(data.status);
          updateInstaller(data.apkExists, data.filename);
          startTimeEl.textContent = data.startTime ? new Date(data.startTime).toLocaleTimeString() : '-';
          endTimeEl.textContent = data.endTime ? new Date(data.endTime).toLocaleTimeString() : '-';
          
          if (data.logs && data.logs.length > 0) {
            term.innerHTML = '';
            data.logs.forEach(log => {
              appendTerminal(log.message, log.message.startsWith('[ERROR]'));
            });
          }
        } else if (type === 'state') {
          updateBadge(data.status);
          updateInstaller(data.apkExists, data.filename);
          startTimeEl.textContent = data.startTime ? new Date(data.startTime).toLocaleTimeString() : '-';
          endTimeEl.textContent = data.endTime ? new Date(data.endTime).toLocaleTimeString() : '-';
        } else if (type === 'log') {
          appendTerminal(data.message, data.message.startsWith('[ERROR]'));
        }
      };
    }

    // Trigger Compilation Build Event
    btnBuild.addEventListener('click', async () => {
      try {
        term.innerHTML = '<div class="text-zinc-600 font-mono">[Build Server] Dispatched compilation build instructions to Docker Daemon...</div>';
        const response = await fetch('/build', { method: 'POST' });
        if (response.status === 409) {
          appendTerminal('[Warning] A build run is indeed already actively executing!');
        } else if (!response.ok) {
          appendTerminal('[ERROR] Server responded with error status: ' + response.status);
        }
      } catch (err) {
        appendTerminal('[ERROR] Network handshake failure: ' + err.message, true);
      }
    });

    btnClear.addEventListener('click', () => {
      term.innerHTML = '<div class="text-zinc-600 font-mono">[Terminal] Standard output screen cleared.</div>';
    });

    // Start lifecycle
    connectSSE();
  </script>
</body>
</html>
`);
  }
});

server.listen(PORT, () => {
  console.log(`🚀 Companion Android Build Server listening on Port ${PORT}`);
  // Run pro-active permission check at startup
  ensureGradlewExecutable();
});
