<?php

class AdminRouter {
    public static function dispatch($contentPath) {
        $requestUri = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH);
        
        if (strpos($requestUri, '/admin/api/') === 0) {
            self::handleApi($contentPath, $requestUri);
            exit;
        }

        self::serveApp();
        exit;
    }

    private static function checkAuth() {
        $envPass = getenv('APP_ADMIN_PASSKEY');
        if ($envPass === false || $envPass === '') {
            $envPass = getenv('app_admin_passkey');
        }

        // If no passkey configured in env, for safety we deny access
        if ($envPass === false || $envPass === '') {
             return false;
        }

        $headers = function_exists('getallheaders') ? getallheaders() : [];
        if (empty($headers) && isset($_SERVER)) {
            foreach ($_SERVER as $name => $value) {
                if (strpos($name, 'HTTP_') === 0) {
                    $headers[str_replace(' ', '-', ucwords(strtolower(str_replace('_', ' ', substr($name, 5)))))] = $value;
                }
            }
        }

        $providedPass = '';
        if (isset($headers['X-Admin-Passkey'])) {
            $providedPass = $headers['X-Admin-Passkey'];
        } elseif (isset($headers['x-admin-passkey'])) {
            $providedPass = $headers['x-admin-passkey'];
        } elseif (isset($_COOKIE['admin_passkey'])) {
            $providedPass = $_COOKIE['admin_passkey'];
        }
        
        return $providedPass === $envPass;
    }

    private static function handleApi($contentPath, $uri) {
        header('Content-Type: application/json');

        if (!self::checkAuth()) {
            http_response_code(403);
            echo json_encode(['error' => 'Unauthorized']);
            exit;
        }

        if ($uri === '/admin/api/sites') {
            $sites = [];
            if (is_dir($contentPath)) {
                $items = scandir($contentPath);
                foreach ($items as $item) {
                    if ($item !== '.' && $item !== '..' && is_dir($contentPath . '/' . $item)) {
                        $sites[] = $item;
                    }
                }
            }
            echo json_encode(['status' => 'success', 'sites' => array_values($sites)]);
            return;
        }

        if ($uri === '/admin/api/ai/tasks') {
            $tasks = AITaskManager::getTasks($contentPath);
            echo json_encode(['status' => 'success', 'tasks' => array_values($tasks)]);
            return;
        }

        if ($uri === '/admin/api/ai/tasks/add' && $_SERVER['REQUEST_METHOD'] === 'POST') {
            $data = json_decode(file_get_contents('php://input'), true);
            $site = $data['site'] ?? 'site1.com';
            $type = $data['type'] ?? 'improve_text';
            $payload = $data['payload'] ?? ['text' => 'Hello World', 'context' => 'Home page'];
            
            $task = AITaskManager::addTask($contentPath, $site, $type, $payload);
            echo json_encode(['status' => 'success', 'task' => $task]);
            return;
        }

        if ($uri === '/admin/api/ai/heartbeat') {
            // Forward call to the public heartbeat (or implement same logic)
            // For simplicity, we just trigger it
            $ch = curl_init('http://localhost:' . (getenv('PORT') ?: '3000') . '/api/heartbeat');
            curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
            $response = curl_exec($ch);
            curl_close($ch);
            echo $response;
            return;
        }

        if (preg_match('#^/admin/api/sites/([^/]+)/download$#', $uri, $matches)) {
            $site = $matches[1];
            self::downloadSite($contentPath, $site);
            exit;
        }

        if (preg_match('#^/admin/api/sites/([^/]+)/upload$#', $uri, $matches)) {
            $site = $matches[1];
            self::uploadSite($contentPath, $site);
            exit;
        }

        http_response_code(404);
        echo json_encode(['error' => 'Endpoint not found']);
    }

    private static function downloadSite($contentPath, $site) {
        // Prevent path traversal
        if (strpos($site, '..') !== false || strpos($site, '/') !== false) {
            http_response_code(400);
            echo json_encode(['error' => 'Invalid site name']);
            return;
        }

        $sitePath = $contentPath . '/' . $site;
        if (!is_dir($sitePath)) {
            http_response_code(404);
            echo json_encode(['error' => 'Site not found']);
            return;
        }

        $zipPath = sys_get_temp_dir() . '/' . $site . '_' . time() . '.zip';
        $zip = new ZipArchive();
        if ($zip->open($zipPath, ZipArchive::CREATE | ZipArchive::OVERWRITE) === true) {
            $files = new RecursiveIteratorIterator(
                new RecursiveDirectoryIterator($sitePath),
                RecursiveIteratorIterator::LEAVES_ONLY
            );

            foreach ($files as $name => $file) {
                if (!$file->isDir()) {
                    $filePath = $file->getRealPath();
                    $relativePath = substr($filePath, strlen($sitePath) + 1);
                    // Add under a folder named as the site
                    $zip->addFile($filePath, $site . '/' . $relativePath);
                }
            }
            $zip->close();

            header('Content-Type: application/zip');
            header('Content-Disposition: attachment; filename="' . $site . '.zip"');
            header('Content-Length: ' . filesize($zipPath));
            readfile($zipPath);
            unlink($zipPath);
        } else {
            http_response_code(500);
            echo json_encode(['error' => 'Could not create zip']);
        }
    }

    private static function uploadSite($contentPath, $site) {
        if (strpos($site, '..') !== false || strpos($site, '/') !== false) {
            http_response_code(400);
            echo json_encode(['error' => 'Invalid site name']);
            return;
        }

        if (!isset($_FILES['file']) || $_FILES['file']['error'] !== UPLOAD_ERR_OK) {
            http_response_code(400);
            echo json_encode(['error' => 'No file uploaded or upload error']);
            return;
        }

        $sitePath = $contentPath . '/' . $site;
        if (!is_dir($sitePath)) {
            // Attempt to create it just in case
            if (!mkdir($sitePath, 0755, true)) {
                http_response_code(500);
                echo json_encode(['error' => 'Failed to create site directory']);
                return;
            }
        }

        $zipPath = $_FILES['file']['tmp_name'];
        $zip = new ZipArchive();
        if ($zip->open($zipPath) === true) {
            // Extract directly to contentPath because zip contains files in the `site/*` subset 
            $zip->extractTo($contentPath);
            $zip->close();
            echo json_encode(['status' => 'success']);
        } else {
            http_response_code(500);
            echo json_encode(['error' => 'Failed to open zip']);
        }
    }

    private static function serveApp() {
        ?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>PHP CMS Admin</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script src="https://unpkg.com/vue@3/dist/vue.global.js"></script>
    <script src="https://unpkg.com/lucide@latest"></script>
</head>
<body class="bg-[#0e0e0e] text-[#f4f4f4] font-sans antialiased">
    <div id="app" class="min-h-screen">
        <main class="max-w-4xl mx-auto px-4 py-12">
            <div v-if="!isAuthenticated" class="max-w-md mx-auto bg-[#181818] p-8 mt-20 border border-[#2A2A2A]">
                <h1 class="text-2xl font-serif italic mb-6">Admin Login</h1>
                <div v-if="errorMsg" class="mb-4 text-red-400 text-sm border-l-2 border-red-500 pl-2">
                    {{ errorMsg }}
                </div>
                <form @submit.prevent="login">
                    <div class="mb-4">
                        <label class="block text-xs font-mono uppercase tracking-widest opacity-50 mb-2">Passkey</label>
                        <input type="password" v-model="passkey" class="w-full bg-[#0e0e0e] border border-[#2A2A2A] p-3 text-white focus:outline-none focus:border-[#F27D26]" placeholder="APP_ADMIN_PASSKEY..." required>
                    </div>
                    <button type="submit" class="w-full bg-[#F27D26] text-black font-bold uppercase tracking-widest text-xs p-3 hover:bg-transparent hover:text-[#F27D26] hover:border-[#F27D26] border border-transparent transition-all">Login</button>
                </form>
            </div>

            <div v-else>
                <header class="flex justify-between items-center mb-12">
                    <div>
                        <h1 class="text-3xl font-serif italic mb-2">PHP CMS Admin</h1>
                        <p class="text-sm opacity-50 font-mono">Manage your content securely</p>
                    </div>
                    <button @click="logout" class="px-4 py-2 border border-[#2A2A2A] text-sm hover:border-red-500 hover:text-red-500 transition-colors">Logout</button>
                </header>

                <!-- Sites List -->
                <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div v-for="site in sites" :key="site" class="group border border-[#2A2A2A] p-6 bg-[#181818] hover:border-[#F27D26] transition-all cursor-pointer relative overflow-hidden">
                        <h3 class="text-xl font-serif italic mb-2">{{ site }}</h3>
                        <p class="text-[10px] font-mono opacity-40 uppercase tracking-tighter">/content/{{ site }}</p>
                        
                        <div class="flex items-center gap-2 mt-4 z-10 relative">
                            <button @click.stop="downloadSite(site)" class="bg-[#2A2A2A] text-white px-3 py-1.5 text-xs font-bold uppercase tracking-wider rounded hover:bg-[#F27D26] hover:text-black transition-all flex items-center gap-1" title="Download ZIP">
                                <i data-lucide="download" class="w-4 h-4"></i> Download
                            </button>
                            <button @click.stop="triggerUpload(site)" class="bg-[#2A2A2A] text-white px-3 py-1.5 text-xs font-bold uppercase tracking-wider rounded hover:bg-[#F27D26] hover:text-black transition-all flex items-center gap-1" title="Upload ZIP to Overwrite">
                                <i data-lucide="upload" class="w-4 h-4"></i> Upload
                            </button>
                        </div>
                        
                        <div class="absolute right-[-20px] bottom-[-20px] opacity-[0.03] scale-[4] group-hover:opacity-[0.07] transition-all pointer-events-none">
                            <i data-lucide="layout-grid" class="w-8 h-8"></i>
                        </div>
                    </div>
                </div>

                <!-- AI Tasks Section -->
                <div class="mt-16">
                    <div class="flex justify-between items-end mb-6">
                        <div>
                            <h2 class="text-2xl font-serif italic mb-1">AI Media Queue</h2>
                            <p class="text-[10px] font-mono opacity-40 uppercase tracking-tighter">Automated tasks via Heartbeat</p>
                        </div>
                        <div class="flex gap-2">
                            <button @click="triggerHeartbeat" class="px-3 py-1 bg-white text-black text-[10px] font-bold uppercase tracking-widest hover:bg-[#F27D26] transition-all" :disabled="isBusy">
                                {{ isBusy ? 'Processing...' : 'Run Heartbeat' }}
                            </button>
                            <button @click="addSampleTask" class="px-3 py-1 border border-[#2A2A2A] text-[10px] font-bold uppercase tracking-widest hover:border-white transition-all">
                                Add Sample Task
                            </button>
                        </div>
                    </div>

                    <div class="border border-[#2A2A2A] bg-[#181818] overflow-hidden">
                        <table class="w-full text-left text-xs">
                            <thead class="bg-[#222] border-b border-[#2A2A2A]">
                                <tr>
                                    <th class="p-3 font-mono opacity-40 uppercase">Task ID</th>
                                    <th class="p-3 font-mono opacity-40 uppercase">Site</th>
                                    <th class="p-3 font-mono opacity-40 uppercase">Type</th>
                                    <th class="p-3 font-mono opacity-40 uppercase">Status</th>
                                    <th class="p-3 font-mono opacity-40 uppercase text-right">Updated</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="task in aiTasks" :key="task.id" class="border-b border-[#2A2A2A] last:border-0 hover:bg-[#222] transition-colors">
                                    <td class="p-3 font-mono opacity-60">{{ task.id }}</td>
                                    <td class="p-3">{{ task.site }}</td>
                                    <td class="p-3">
                                        <span class="px-1.5 py-0.5 bg-[#2A2A2A] rounded text-[9px] uppercase font-bold">{{ task.type }}</span>
                                    </td>
                                    <td class="p-3">
                                        <span :class="{
                                            'text-yellow-400': task.status === 'pending',
                                            'text-blue-400': task.status === 'running',
                                            'text-green-400': task.status === 'completed',
                                            'text-red-400': task.status === 'failed'
                                        }" class="font-bold lowercase italic">{{ task.status }}</span>
                                    </td>
                                    <td class="p-3 text-right font-mono opacity-30 text-[10px] italic">{{ task.updated_at }}</td>
                                </tr>
                                <tr v-if="aiTasks.length === 0">
                                    <td colspan="5" class="p-8 text-center opacity-30 italic">No AI tasks in queue.</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>

                <input type="file" accept=".zip" ref="uploadInput" class="hidden" @change="onFileSelected" />

                <div v-if="sites.length === 0" class="border border-dashed border-[#2A2A2A] py-20 text-center opacity-30 mt-8">
                    <p class="text-sm italic">No sites found.</p>
                </div>
            </div>
        </main>
    </div>

    <script>
        const { createApp, ref, onMounted, nextTick } = Vue;

        createApp({
            setup() {
                const isAuthenticated = ref(false);
                const passkey = ref('');
                const errorMsg = ref('');
                const sites = ref([]);
                const aiTasks = ref([]);
                const isBusy = ref(false);
                const uploadInput = ref(null);
                const siteToUpload = ref('');

                const loadLucide = async () => {
                    await nextTick();
                    if(window.lucide) {
                        window.lucide.createIcons();
                    }
                };

                const fetchTasks = async () => {
                    const storedKey = localStorage.getItem('adminPasskey');
                    try {
                        const res = await fetch('/admin/api/ai/tasks', {
                            headers: { 'X-Admin-Passkey': storedKey }
                        });
                        if (res.ok) {
                            const data = await res.json();
                            aiTasks.value = data.tasks.reverse();
                        }
                    } catch (e) {
                        console.error('Error fetching tasks', e);
                    }
                };

                const triggerHeartbeat = async () => {
                    isBusy.value = true;
                    const storedKey = localStorage.getItem('adminPasskey');
                    try {
                        const res = await fetch('/admin/api/ai/heartbeat', {
                            headers: { 'X-Admin-Passkey': storedKey }
                        });
                        const data = await res.json();
                        if (data.status === 'success') {
                            console.log('Heartbeat run:', data.message);
                        } else {
                            console.warn('Heartbeat error:', data.message);
                        }
                        await fetchTasks();
                    } catch (e) {
                        console.error('Heartbeat failed', e);
                    } finally {
                        isBusy.value = false;
                    }
                };

                const addSampleTask = async () => {
                    const storedKey = localStorage.getItem('adminPasskey');
                    try {
                        await fetch('/admin/api/ai/tasks/add', {
                            method: 'POST',
                            headers: { 
                                'X-Admin-Passkey': storedKey,
                                'Content-Type': 'application/json'
                            },
                            body: JSON.stringify({
                                site: sites.value[0] || 'site1.com',
                                type: 'improve_text',
                                payload: { text: "Welcome to our website. We provide quality services.", context: "Landing Page Hero" }
                            })
                        });
                        await fetchTasks();
                    } catch (e) {
                        console.error('Add task failed', e);
                    }
                };

                const checkAuth = async () => {
                    const storedKey = localStorage.getItem('adminPasskey');
                    if (storedKey) {
                        try {
                            const res = await fetch('/admin/api/sites', {
                                headers: { 'X-Admin-Passkey': storedKey }
                            });
                            if (res.ok) {
                                isAuthenticated.value = true;
                                passkey.value = storedKey;
                                const data = await res.json();
                                sites.value = data.sites;
                                await fetchTasks();
                                loadLucide();
                            } else {
                                localStorage.removeItem('adminPasskey');
                                errorMsg.value = 'Session expired or passkey changed.';
                            }
                        } catch (e) {
                            console.error(e);
                        }
                    }
                };

                const login = async () => {
                    errorMsg.value = '';
                    try {
                        const res = await fetch('/admin/api/sites', {
                            headers: { 'X-Admin-Passkey': passkey.value }
                        });
                        if (res.ok) {
                            localStorage.setItem('adminPasskey', passkey.value);
                            isAuthenticated.value = true;
                            const data = await res.json();
                            sites.value = data.sites;
                            loadLucide();
                        } else {
                            errorMsg.value = 'Invalid passkey';
                        }
                    } catch (e) {
                        errorMsg.value = 'Error verifying passkey';
                    }
                };

                const logout = () => {
                    localStorage.removeItem('adminPasskey');
                    isAuthenticated.value = false;
                    passkey.value = '';
                };

                const downloadSite = (site) => {
                    const storedKey = localStorage.getItem('adminPasskey');
                    document.cookie = `admin_passkey=${storedKey}; path=/; max-age=60`;
                    window.open(`/admin/api/sites/${site}/download`, '_blank');
                };

                const triggerUpload = (site) => {
                    siteToUpload.value = site;
                    uploadInput.value.click();
                };

                const onFileSelected = async (event) => {
                    const target = event.target;
                    const file = target.files?.[0];
                    if (!file || !siteToUpload.value) return;

                    const formData = new FormData();
                    formData.append('file', file);
                    const storedKey = localStorage.getItem('adminPasskey');

                    try {
                        const res = await fetch(`/admin/api/sites/${siteToUpload.value}/upload`, {
                            method: 'POST',
                            headers: { 'X-Admin-Passkey': storedKey },
                            body: formData
                        });
                        if (res.ok) {
                            alert('Site updated successfully from zip!');
                        } else {
                            const e = await res.json();
                            alert('Failed to upload site: ' + e.error);
                        }
                    } catch (err) {
                        alert('Upload error');
                    }
                    target.value = '';
                };

                onMounted(() => {
                    checkAuth();
                });

                return {
                    isAuthenticated,
                    passkey,
                    errorMsg,
                    sites,
                    aiTasks,
                    isBusy,
                    login,
                    logout,
                    downloadSite,
                    triggerUpload,
                    onFileSelected,
                    triggerHeartbeat,
                    addSampleTask,
                    uploadInput
                };
            }
        }).mount('#app');
    </script>
</body>
</html>
        <?php
    }
}
