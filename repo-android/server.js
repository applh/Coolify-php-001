const http = require('http');
const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

const port = 3000;
let buildStatus = 'Building'; // 'Building', 'Success', 'Failed'
let logContent = '';
let buildTimestamp = null;

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
        buildTimestamp = new Date().toLocaleString();
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
                                <script src="https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js"></script>
                <style>
                    body { font-family: monospace; background: #1e1e1e; color: #fff; padding: 20px; line-height: 1.5; }
                    .status { padding: 15px; margin-bottom: 20px; border-radius: 5px; font-weight: bold; font-size: 1.2em; }
                    .Building { background: #d97706; }
                    .Success { background: #059669; }
                    .Failed { background: #dc2626; }
                    pre { background: #000; padding: 20px; border-radius: 5px; overflow-x: auto; white-space: pre-wrap; word-wrap: break-word; }
                    .download-area { display: flex; align-items: center; gap: 20px; margin-bottom: 20px; }
                    a.button { display: inline-block; padding: 12px 24px; background: #2563eb; color: white; text-decoration: none; border-radius: 5px; font-weight: bold; }
                    a.button:hover { background: #1d4ed8; }
                    #qrcode { padding: 10px; background: white; border-radius: 5px; }
                    .timestamp { color: #9ca3af; font-size: 0.9em; margin-bottom: 20px; }
                </style>
            </head>
            <body>
                <h1>Android Build Panel</h1>
                <div id="status-div" class="status ${buildStatus}">Status: ${buildStatus}</div>
                <div id="timestamp-container" class="timestamp">${buildTimestamp ? 'Build Timestamp: ' + buildTimestamp : ''}</div>
                <div id="download-container" class="download-area">
                    ${buildStatus === 'Success' ? '<a href="/app.apk" class="button">Download APK</a><div id="qrcode"></div>' : ''}
                </div>
                <h2>Build Logs:</h2>
                <p><i>Logs are automatically updated via AJAX.</i></p>
                <pre id="logs-pre">${logContent.replace(/</g, '&lt;').replace(/>/g, '&gt;')}</pre>
                
                <script>
                    function generateQRCode() {
                        const qrContainer = document.getElementById('qrcode');
                        if (qrContainer && !qrContainer.hasChildNodes()) {
                            let apkUrl = window.location.href.split('?')[0].split('#')[0];
                            if (!apkUrl.endsWith('/')) {
                                apkUrl = apkUrl.substring(0, apkUrl.lastIndexOf('/') + 1);
                            }
                            apkUrl += 'app.apk';

                            new QRCode(qrContainer, {
                                text: apkUrl,
                                width: 100,
                                height: 100,
                                colorDark : "#000000",
                                colorLight : "#ffffff",
                                correctLevel : QRCode.CorrectLevel.H
                            });
                        }
                    }

                    // Attempt generation immediately in case it's already successful
                    if ('${buildStatus}' === 'Success') {
                        generateQRCode();
                    }

                    async function fetchStatus() {
                        try {
                            const res = await fetch('/status');
                            if (!res.ok) throw new Error('Network response was not ok');
                            const data = await res.json();
                            
                            const statusDiv = document.getElementById('status-div');
                            statusDiv.textContent = 'Status: ' + data.buildStatus;
                            statusDiv.className = 'status ' + data.buildStatus;
                            
                            const timestampDiv = document.getElementById('timestamp-container');
                            if (data.buildTimestamp) {
                                timestampDiv.textContent = 'Build Timestamp: ' + data.buildTimestamp;
                            }

                            const logsPre = document.getElementById('logs-pre');
                            logsPre.textContent = data.logContent;
                            
                            // Scroll to bottom if it's currently building
                            if (data.buildStatus === 'Building') {
                                window.scrollTo(0, document.body.scrollHeight);
                            }
                            
                            const downloadContainer = document.getElementById('download-container');
                            if (data.buildStatus === 'Success') {
                                if (downloadContainer.innerHTML.trim() === '') {
                                    downloadContainer.innerHTML = '<a href="/app.apk" class="button">Download APK</a><div id="qrcode"></div>';
                                    generateQRCode();
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
        res.end(JSON.stringify({ buildStatus, logContent, buildTimestamp }));
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
