const http = require('http');
const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

const port = 3000;
let buildStatus = 'Building'; // 'Building', 'Success', 'Failed'
let logContent = '';

// Spawn the gradle build in the background
const buildProcess = spawn('gradle', ['assembleDebug', '--no-daemon', '--max-workers', '1', '--stacktrace', '--console=plain'], { cwd: '/app' });

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
    } else {
        buildStatus = 'Failed';
    }
    console.log(`Build process exited with code ${code}`);
});

const server = http.createServer((req, res) => {
    if (req.url === '/') {
        res.writeHead(200, { 'Content-Type': 'text/html' });
        res.end(`
            <!DOCTYPE html>
            <html>
            <head>
                <title>Android Build Server</title>
                <style>
                    body { font-family: monospace; background: #1e1e1e; color: #fff; padding: 20px; line-height: 1.5; }
                    .status { padding: 15px; margin-bottom: 20px; border-radius: 5px; font-weight: bold; font-size: 1.2em; }
                    .Building { background: #d97706; }
                    .Success { background: #059669; }
                    .Failed { background: #dc2626; }
                    pre { background: #000; padding: 20px; border-radius: 5px; overflow-x: auto; white-space: pre-wrap; word-wrap: break-word; }
                    a.button { display: inline-block; padding: 12px 24px; background: #2563eb; color: white; text-decoration: none; border-radius: 5px; font-weight: bold; margin-bottom: 20px; }
                    a.button:hover { background: #1d4ed8; }
                </style>
            </head>
            <body>
                <h1>Android Build Panel</h1>
                <div id="status-div" class="status ${buildStatus}">Status: ${buildStatus}</div>
                <div id="download-container">
                    ${buildStatus === 'Success' ? '<a href="/app.apk" class="button">Download APK</a>' : ''}
                </div>
                <h2>Build Logs:</h2>
                <p><i>Logs are automatically updated via AJAX.</i></p>
                <pre id="logs-pre">${logContent.replace(/</g, '&lt;').replace(/>/g, '&gt;')}</pre>
                
                <script>
                    async function fetchStatus() {
                        try {
                            const res = await fetch('/status');
                            if (!res.ok) throw new Error('Network response was not ok');
                            const data = await res.json();
                            
                            const statusDiv = document.getElementById('status-div');
                            statusDiv.textContent = 'Status: ' + data.buildStatus;
                            statusDiv.className = 'status ' + data.buildStatus;
                            
                            const logsPre = document.getElementById('logs-pre');
                            logsPre.textContent = data.logContent;
                            
                            // Scroll to bottom if it's currently building
                            if (data.buildStatus === 'Building') {
                                window.scrollTo(0, document.body.scrollHeight);
                            }
                            
                            const downloadContainer = document.getElementById('download-container');
                            if (data.buildStatus === 'Success') {
                                if (downloadContainer.innerHTML.trim() === '') {
                                    downloadContainer.innerHTML = '<a href="/app.apk" class="button">Download APK</a>';
                                }
                            } else {
                                downloadContainer.innerHTML = '';
                            }
                            
                            if (data.buildStatus === 'Building') {
                                setTimeout(fetchStatus, 2000); // Poll every 2 seconds
                            }
                        } catch (e) {
                            console.error('Error fetching status', e);
                            setTimeout(fetchStatus, 5000); // Retry longer on error
                        }
                    }
                    
                    // Start polling if still building
                    if ('${buildStatus}' === 'Building') {
                        setTimeout(fetchStatus, 2000);
                    }
                </script>
            </body>
            </html>
        `);
    } else if (req.url === '/status') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ buildStatus, logContent }));
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
            res.end('APK not found');
        }
    } else {
        res.writeHead(404);
        res.end('Not found');
    }
});

server.listen(port, () => {
    console.log(`Build server listening on port ${port}`);
});
