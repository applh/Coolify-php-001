/* eslint-disable @typescript-eslint/no-require-imports */
const http = require('http');
const { spawn, execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const port = 3000;
let buildStatus = 'Building'; // 'Building', 'Success', 'Failed'
let logContent = '';
let buildTimestamp = null;
let buildProcess = null;

// Get static environmental system diagnostics on startup
const sysDiagnostics = {
    jdkVersion: 'N/A',
    gradleVersion: 'N/A',
    diskUsage: 'N/A',
    memory: 'N/A',
    androidHome: process.env.ANDROID_HOME || 'Not Configured'
};

try {
    const javaOutput = execSync('java -version 2>&1', { encoding: 'utf8' });
    sysDiagnostics.jdkVersion = javaOutput.split('\n')[0] || 'Unknown';
} catch (err) {
    sysDiagnostics.jdkVersion = 'Failed to fetch JDK version';
    console.debug('JDK version query notice:', err.message);
}

try {
    const gradleOutput = execSync('gradle -v', { encoding: 'utf8' });
    const versionLine = gradleOutput.split('\n').find(l => l.trim().startsWith('Gradle '));
    sysDiagnostics.gradleVersion = versionLine ? versionLine.trim() : 'Unknown';
} catch (err) {
    sysDiagnostics.gradleVersion = 'Failed to fetch Gradle version';
    console.debug('Gradle query notice:', err.message);
}

try {
    const dfOutput = execSync('df -h / | tail -n 1', { encoding: 'utf8' }).trim();
    if (dfOutput) {
        const parts = dfOutput.split(/\s+/);
        sysDiagnostics.diskUsage = `Total: ${parts[1]}, Used: ${parts[2]} (${parts[4]} loaded)`;
    }
} catch (err) {
    sysDiagnostics.diskUsage = 'N/A (Non-Linux Host)';
    console.debug('Disk query notice:', err.message);
}

try {
    const memOutput = execSync('cat /proc/meminfo | grep MemTotal', { encoding: 'utf8' }).trim();
    if (memOutput) {
        sysDiagnostics.memory = memOutput.replace('MemTotal:', '').trim();
    }
} catch (err) {
    sysDiagnostics.memory = 'N/A (Non-Linux Host)';
    console.debug('Memory query notice:', err.message);
}


// Function to parse compile or lint errors from the build logs
function parseCompileErrors(logs) {
    const errorList = [];
    const lines = logs.split('\n');
    
    // Pattern matching Kotlin/Java compilation errors:
    // e: /app/app/src/main/java/com/example/cameraxapp/CameraScreen.kt: (125, 43): Unresolved reference: ZoomState
    // e: [file]: ([line], [col]): [message]
    const kotlinCompilePattern = /^e:\s+([^\s:]+):\s*\((\d+),\s*(\d+)\):\s*(.+)/;
    
    // Pattern matching generic Android gradle compilation issues or resource failures
    const resourcePattern = /error:\s+resource\s+([^\s]+)\s+(.+)/i;

    lines.forEach(line => {
        const ktMatch = line.match(kotlinCompilePattern);
        if (ktMatch) {
            errorList.push({
                type: 'Kotlin Compiler Error',
                file: ktMatch[1].replace('/app/', ''),
                line: ktMatch[2],
                col: ktMatch[3],
                message: ktMatch[4]
            });
            return;
        }

        const resMatch = line.match(resourcePattern);
        if (resMatch) {
            errorList.push({
                type: 'XML Resource Error',
                file: 'Android Resources',
                line: 'N/A',
                col: 'N/A',
                message: `${resMatch[1]}: ${resMatch[2]}`
            });
            return;
        }

        if (line.includes('* What went wrong:')) {
            errorList.push({
                type: 'Gradle Assembly Link Error',
                file: 'Gradle Build script',
                line: 'Global',
                col: 'N/A',
                message: line.trim()
            });
        }
    });

    return errorList;
}

// Spawns the Gradle build pipeline
function startBuild() {
    buildStatus = 'Building';
    
    // Use --no-daemon, precise caching, console-plain settings for isolated memory efficiency
    buildProcess = spawn('gradle', [
        'assembleDebug',
        '--no-daemon',
        '--max-workers', '1',
        '--stacktrace',
        '--console=plain'
    ], { 
        cwd: '/app',
        env: { ...process.env, GRADLE_OPTS: process.env.GRADLE_OPTS || "-Dorg.gradle.jvmargs=\"-Xmx3072m -XX:MaxMetaspaceSize=384m\"" }
    });

    buildProcess.stdout.on('data', (data) => {
        logContent += data.toString();
        console.log(data.toString());
    });

    buildProcess.stderr.on('data', (data) => {
        logContent += data.toString();
        console.error(data.toString());
    });

    buildProcess.on('close', (code) => {
        if (code === 0) {
            buildStatus = 'Success';
            buildTimestamp = new Date().toLocaleString();
        } else {
            buildStatus = 'Failed';
            buildTimestamp = new Date().toLocaleString();
        }
        console.log(`Build process completed with exit code: ${code}`);
    });
}

// Initial build trigger on server startup
startBuild();

const server = http.createServer((req, res) => {
    if (req.url === '/') {
        res.writeHead(200, { 'Content-Type': 'text/html' });
        res.end(`
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Android Build & Debug Server</title>
                <script src="https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js"></script>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, monospace;
                        background: #0d0e12;
                        color: #e3e4e8;
                        margin: 0;
                        padding: 30px;
                        line-height: 1.6;
                    }
                    .container {
                        max-width: 1200px;
                        margin: 0 auto;
                    }
                    header {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        border-bottom: 2px solid #202430;
                        padding-bottom: 15px;
                        margin-bottom: 25px;
                    }
                    h1 {
                        font-size: 1.8em;
                        margin: 0;
                        color: #ffffff;
                        letter-spacing: -0.5px;
                    }
                    h2 {
                        font-size: 1.3em;
                        color: #ffffff;
                        margin-top: 25px;
                        margin-bottom: 15px;
                    }
                    .status-panel {
                        display: flex;
                        gap: 20px;
                        margin-bottom: 25px;
                        flex-wrap: wrap;
                    }
                    .status-card {
                        flex: 1;
                        min-width: 280px;
                        background: #161821;
                        border: 1px solid #232735;
                        padding: 20px;
                        border-radius: 10px;
                    }
                    .status-header {
                        font-size: 0.9em;
                        color: #8b949e;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                        margin-bottom: 8px;
                    }
                    .status-value {
                        font-size: 1.6em;
                        font-weight: bold;
                        color: #ffffff;
                    }
                    .Building { color: #f59e0b; }
                    .Success { color: #10b981; }
                    .Failed { color: #ef4444; }
                    
                    /* Diagnostics Grid */
                    .diagnostics-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                        gap: 15px;
                        background: #10121a;
                        border: 1px solid #1f222e;
                        padding: 15px;
                        border-radius: 8px;
                        margin-bottom: 25px;
                    }
                    .diag-item {
                        font-size: 0.85em;
                    }
                    .diag-label {
                        color: #8b949e;
                        margin-bottom: 4px;
                    }
                    .diag-info {
                        font-family: monospace;
                        color: #58a6ff;
                        font-weight: bold;
                    }

                    /* Compile Failures HUD Quick Board */
                    .error-hud {
                        background: #221518;
                        border: 1px solid #442026;
                        color: #ff7b72;
                        border-radius: 8px;
                        padding: 15px;
                        margin-bottom: 25px;
                        display: none;
                    }
                    .error-hud h3 {
                        margin-top: 0;
                        margin-bottom: 10px;
                        font-size: 1.1em;
                        color: #ff7b72;
                        display: flex;
                        align-items: center;
                    }
                    .error-item {
                        border-left: 3px solid #f85149;
                        padding-left: 12px;
                        margin-bottom: 12px;
                        font-family: monospace;
                        font-size: 0.9em;
                    }
                    .error-item:last-child {
                        margin-bottom: 0;
                    }
                    .error-file {
                        color: #f85149;
                        font-weight: bold;
                    }
                    .error-pos {
                        color: #db6d28;
                    }
                    .error-msg {
                        color: #c9d1d9;
                        margin-top: 4px;
                    }

                    .download-area {
                        display: flex;
                        align-items: center;
                        gap: 25px;
                        background: #14211e;
                        border: 1px solid #1a3c30;
                        padding: 20px;
                        border-radius: 10px;
                        margin-bottom: 25px;
                    }
                    a.button {
                        display: inline-block;
                        padding: 12px 28px;
                        background: #059669;
                        color: white;
                        text-decoration: none;
                        border-radius: 6px;
                        font-weight: bold;
                        font-size: 1em;
                        transition: background 0.2s;
                        text-align: center;
                    }
                    a.button:hover { background: #047857; }
                    
                    .control-btn {
                        padding: 10px 20px;
                        cursor: pointer;
                        font-weight: bold;
                        border-radius: 6px;
                        border: none;
                        background: #38bdf8;
                        color: #1e293b;
                        transition: opacity 0.2s;
                    }
                    .control-btn:hover { opacity: 0.9; }
                    .control-btn:disabled { opacity: 0.5; cursor: not-allowed; }

                    #qrcode {
                        padding: 10px;
                        background: white;
                        border-radius: 6px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                    }
                    
                    /* Logs control headers */
                    .logs-tools {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        background: #161821;
                        border: 1px solid #232735;
                        padding: 12px 20px;
                        border-radius: 8px 8px 0 0;
                        border-bottom: none;
                    }
                    .filter-group {
                        display: flex;
                        gap: 10px;
                        align-items: center;
                        font-size: 0.9em;
                    }
                    .filter-btn {
                        background: #232735;
                        border: 1px solid #30363d;
                        color: #c9d1d9;
                        padding: 5px 12px;
                        border-radius: 4px;
                        cursor: pointer;
                        font-size: 0.85em;
                    }
                    .filter-btn.active {
                        background: #58a6ff;
                        color: #0d1117;
                        border-color: #58a6ff;
                    }

                    pre {
                        background: #090a0f;
                        padding: 24px;
                        border-radius: 0 0 8px 8px;
                        border: 1px solid #232735;
                        max-height: 550px;
                        overflow-y: auto;
                        overflow-x: auto;
                        margin: 0;
                        white-space: pre-wrap;
                        word-wrap: break-word;
                        font-family: "JetBrains Mono", "Courier New", Courier, monospace;
                        font-size: 12.5px;
                        color: #c9d1d9;
                    }
                    
                    /* Custom Scrollbars */
                    pre::-webkit-scrollbar { width: 8px; height: 8px; }
                    pre::-webkit-scrollbar-track { background: #090a0f; }
                    pre::-webkit-scrollbar-thumb { background: #21262d; border-radius: 4px; }
                    pre::-webkit-scrollbar-thumb:hover { background: #30363d; }

                    .log-line-err { color: #f85149; font-weight: bold; }
                    .log-line-warn { color: #e3b341; }
                    .log-line-info { color: #58a6ff; }
                    .log-line-success { color: #56d364; }
                </style>
            </head>
            <body>
                <div class="container">
                    <header>
                        <div>
                            <h1>Android Build & Diagnostics Core</h1>
                            <div style="color: #8b949e; font-size: 0.9em; margin-top: 3px;">Coolify Container Hot Rebuilder Console</div>
                        </div>
                        <button id="rebuild-btn" class="control-btn" onclick="triggerRebuild()">⚙️ Restart Assembly Build</button>
                    </header>

                    <!-- Resource Allocation & Configurations Inspector -->
                    <h2>📋 Node Hosting Environment Context</h2>
                    <div class="diagnostics-grid">
                        <div class="diag-item">
                            <div class="diag-label">JDK Environment (Java)</div>
                            <div class="diag-info">${sysDiagnostics.jdkVersion}</div>
                        </div>
                        <div class="diag-item">
                            <div class="diag-label">Gradle Compiler Engine</div>
                            <div class="diag-info">${sysDiagnostics.gradleVersion}</div>
                        </div>
                        <div class="diag-item">
                            <div class="diag-label">Android Home Dir</div>
                            <div class="diag-info">${sysDiagnostics.androidHome}</div>
                        </div>
                        <div class="diag-item">
                            <div class="diag-label">Mem Reservation / Disk Info</div>
                            <div class="diag-info" style="font-size: 0.93em;">Alloc: ${sysDiagnostics.memory} <br/>${sysDiagnostics.diskUsage}</div>
                        </div>
                    </div>

                    <!-- Build Status Dashboard -->
                    <div class="status-panel">
                        <div class="status-card">
                            <div class="status-header">Pipeline Status</div>
                            <div id="status-val" class="status-value ${buildStatus}">Status: ${buildStatus}</div>
                        </div>
                        <div class="status-card">
                            <div class="status-header">Assembly Triggered Time</div>
                            <div id="timestamp-val" class="status-value" style="font-size: 1.25em; padding-top: 5px; color: #c9d1d9;">${buildTimestamp ? buildTimestamp : 'Spawning ...'}</div>
                        </div>
                    </div>

                    <!-- HUD Quick board for unresolved references, parse errors, and lints -->
                    <div id="error-hud-container" class="error-hud">
                        <h3>❌ Compilation Diagnostics Panel</h3>
                        <div id="error-hud-items"></div>
                    </div>

                    <!-- Compilation Output Downloader wrapper -->
                    <div id="download-container" class="download-area" style="display: ${buildStatus === 'Success' ? 'flex' : 'none'};">
                        <div>
                            <h2 style="margin: 0 0 8px 0; color: #56d364;">✔️ App Compiled Successfully</h2>
                            <p style="margin: 0 0 15px 0; color: #8b949e; font-size: 0.9em;">Click the download trigger below or scan the generated QR frame using your mobile camera target directly.</p>
                            <a href="/app.apk" class="button">⬇️ Download Debug APK</a>
                        </div>
                        <div style="margin-left: auto;">
                            <div id="qrcode"></div>
                        </div>
                    </div>

                    <!-- Terminal Output Console -->
                    <div class="logs-tools">
                        <strong>🖥️ Compiler Stdout / Stderr streams</strong>
                        <div class="filter-group">
                            <span>Filter Display:</span>
                            <button id="filter-all" class="filter-btn active" onclick="setLogFilter('ALL')">All Logs</button>
                            <button id="filter-errors" class="filter-btn" onclick="setLogFilter('ERRORS')">Errors Only</button>
                        </div>
                    </div>
                    <pre id="logs-pre">${logContent}</pre>
                </div>

                <script>
                    let currentFilter = 'ALL';
                    let rawLogsCache = \`${logContent.replace(/`/g, '\\`').replace(/\$/g, '\\$')}\`;

                    function generateQRCode() {
                        const qrContainer = document.getElementById('qrcode');
                        if (qrContainer) {
                            qrContainer.innerHTML = ''; // clear old QR
                            let apkUrl = window.location.href.split('?')[0].split('#')[0];
                            if (!apkUrl.endsWith('/')) {
                                apkUrl = apkUrl.substring(0, apkUrl.lastIndexOf('/') + 1);
                            }
                            apkUrl += 'app.apk';

                            new QRCode(qrContainer, {
                                text: apkUrl,
                                width: 110,
                                height: 110,
                                colorDark : "#000000",
                                colorLight : "#ffffff",
                                correctLevel : QRCode.CorrectLevel.M
                            });
                        }
                    }

                    async function triggerRebuild() {
                        const btn = document.getElementById('rebuild-btn');
                        btn.disabled = true;
                        btn.textContent = '🔄 Preparing Workspace...';
                        
                        try {
                            const res = await fetch('/restart', { method: 'POST' });
                            if (res.ok) {
                                rawLogsCache = '--- Reloading build sequence from Gradle script ---';
                                document.getElementById('logs-pre').textContent = rawLogsCache;
                                document.getElementById('status-val').className = 'status-value Building';
                                document.getElementById('status-val').textContent = 'Status: Building';
                                document.getElementById('error-hud-container').style.display = 'none';
                                document.getElementById('download-container').style.display = 'none';
                                setTimeout(pollStatus, 1500);
                            }
                        } catch (e) {
                            console.error('Error contacting build server', e);
                        } finally {
                            setTimeout(() => {
                                btn.disabled = false;
                                btn.textContent = '⚙️ Restart Assembly Build';
                            }, 3000);
                        }
                    }

                    function setLogFilter(mode) {
                        currentFilter = mode;
                        document.getElementById('filter-all').className = 'filter-btn' + (mode === 'ALL' ? ' active' : '');
                        document.getElementById('filter-errors').className = 'filter-btn' + (mode === 'ERRORS' ? ' active' : '');
                        renderFormattedLogs(rawLogsCache);
                    }

                    function renderFormattedLogs(rawText) {
                        const logsPre = document.getElementById('logs-pre');
                        const lines = rawText.split('\\n');
                        let htmlBuffer = [];
                        
                        lines.forEach(line => {
                            const trimmed = line.trim();
                            let isError = trimmed.startsWith('e:') || trimmed.includes(': error:') || trimmed.includes('FAILED') || trimmed.includes('Build failed with an exception.');
                            let isWarning = trimmed.includes('w:') || trimmed.includes('warning:') || trimmed.includes('WARNING:');
                            let isSuccess = trimmed.includes('BUILD SUCCESSFUL') || trimmed.includes('compileDebugKotlin') || trimmed.includes('Task :app:assembleDebug');

                            if (currentFilter === 'ERRORS' && !isError && !isWarning) {
                                return;
                            }

                            if (isError) {
                                htmlBuffer.push('<span class="log-line-err">' + escapeHTML(line) + '</span>');
                            } else if (isWarning) {
                                htmlBuffer.push('<span class="log-line-warn">' + escapeHTML(line) + '</span>');
                            } else if (isSuccess) {
                                htmlBuffer.push('<span class="log-line-success">' + escapeHTML(line) + '</span>');
                            } else {
                                htmlBuffer.push(escapeHTML(line));
                            }
                        });

                        logsPre.innerHTML = htmlBuffer.join('\\n');
                    }

                    function escapeHTML(str) {
                        return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
                    }

                    async function pollStatus() {
                        try {
                            const res = await fetch('/status');
                            if (!res.ok) throw new Error('Network error');
                            const data = await res.json();
                            
                            rawLogsCache = data.logContent;
                            
                            // Status updater
                            const sVal = document.getElementById('status-val');
                            sVal.className = 'status-value ' + data.buildStatus;
                            sVal.textContent = 'Status: ' + data.buildStatus;
                            
                            const tsVal = document.getElementById('timestamp-val');
                            tsVal.textContent = data.buildTimestamp ? data.buildTimestamp : 'Compiling...';
                            
                            renderFormattedLogs(data.logContent);

                            // Compile Diagnostics Panel
                            const hud = document.getElementById('error-hud-container');
                            const hudItems = document.getElementById('error-hud-items');
                            if (data.brokenErrors && data.brokenErrors.length > 0) {
                                hud.style.display = 'block';
                                hudItems.innerHTML = data.brokenErrors.map(e => \`
                                    <div class="error-item">
                                        [<span class="error-file">\${e.file}</span>:\${e.line}] <span class="error-pos">Type: \${e.type}</span>
                                        <div class="error-msg">\${escapeHTML(e.message)}</div>
                                    </div>
                                \`).join('');
                            } else if (data.buildStatus === 'Failed') {
                                hud.style.display = 'block';
                                hudItems.innerHTML = '<div class="error-item"><span class="error-file">Assembly Error</span>: Gradle compilation failed. Check stdout for stack traces.</div>';
                            } else {
                                hud.style.display = 'none';
                            }

                            // Download view logic
                            const dCon = document.getElementById('download-container');
                            if (data.buildStatus === 'Success') {
                                if (dCon.style.display === 'none') {
                                    dCon.style.display = 'flex';
                                    generateQRCode();
                                }
                            } else {
                                dCon.style.display = 'none';
                            }

                            // If building, scroll layout down to inspect streams
                            if (data.buildStatus === 'Building') {
                                setTimeout(pollStatus, 2000);
                                const preElement = document.getElementById('logs-pre');
                                preElement.scrollTop = preElement.scrollHeight;
                            }
                        } catch (e) {
                            console.error('Error polling status', e);
                            setTimeout(pollStatus, 5000);
                        }
                    }

                    // Run initial display formatter and begin checks
                    renderFormattedLogs(rawLogsCache);
                    if ('\${buildStatus}' === 'Building') {
                        setTimeout(pollStatus, 2000);
                    } else if ('\${buildStatus}' === 'Success') {
                        generateQRCode();
                        pollStatus(); // fetch initial diagnostics as well
                    } else {
                        pollStatus();
                    }
                </script>
            </body>
            </html>
        `);
    } else if (req.url === '/status') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        const brokenErrors = parseCompileErrors(logContent);
        res.end(JSON.stringify({ buildStatus, logContent, buildTimestamp, brokenErrors }));
    } else if (req.url === '/restart' && req.method === 'POST') {
        if (buildProcess) {
            try {
                // Kill process tree
                buildProcess.kill('SIGINT');
            } catch (e) { console.error('Error shutting down background assembly', e); }
        }
        logContent = '--- Initialized Custom Rebuild Assembly Initiated ---\\n';
        buildTimestamp = new Date().toLocaleString() + ' (Manual Trigger)';
        startBuild();
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: true }));
    } else if (req.url === '/app.apk' && buildStatus === 'Success') {
        const apkPath = path.join(__dirname, 'app/build/outputs/apk/debug/app-debug.apk');
        if (fs.existsSync(apkPath)) {
            res.writeHead(200, {
                'Content-Type': 'application/vnd.android.package-archive',
                'Content-Disposition': 'attachment; filename="app.apk"'
            });
            fs.createReadStream(apkPath).pipe(res);
        } else {
            res.writeHead(404);
            res.end('APK output binaries not gathered on compilation thread');
        }
    } else {
        res.writeHead(404);
        res.end('Route Not Found');
    }
});

server.listen(port, () => {
    console.log(`Build & Diagnostics Server actively running on port ${port}`);
});
