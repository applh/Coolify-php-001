<?php

require_once __DIR__ . '/../plugins/forms/FormsManager.php';

class AdminRouter {
    public static function dispatch($contentPath) {
        // Initialize FormsManager context
        FormsManager::setContext($contentPath);
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

        // Forms API
        if ($uri === '/admin/api/forms' && $_SERVER['REQUEST_METHOD'] === 'GET') {
            $site = $_GET['site'] ?? '';
            $forms = FormsManager::getForms($site);
            echo json_encode(['status' => 'success', 'forms' => $forms]);
            return;
        }

        if ($uri === '/admin/api/forms/save' && $_SERVER['REQUEST_METHOD'] === 'POST') {
            $data = json_decode(file_get_contents('php://input'), true);
            $site = $data['site'] ?? '';
            $form = $data['form'] ?? null;
            if (!$site || !$form) {
                http_response_code(400);
                echo json_encode(['error' => 'Missing site or form data']);
                return;
            }
            $savedForm = FormsManager::saveForm($site, $form);
            echo json_encode(['status' => 'success', 'form' => $savedForm]);
            return;
        }

        if ($uri === '/admin/api/forms/delete' && $_SERVER['REQUEST_METHOD'] === 'POST') {
            $data = json_decode(file_get_contents('php://input'), true);
            $site = $data['site'] ?? '';
            $formId = $data['form_id'] ?? '';
            FormsManager::deleteForm($site, $formId);
            echo json_encode(['status' => 'success']);
            return;
        }

        if ($uri === '/admin/api/forms/submissions' && $_SERVER['REQUEST_METHOD'] === 'GET') {
            $site = $_GET['site'] ?? '';
            $formId = $_GET['form_id'] ?? '';
            $submissions = FormsManager::getSubmissions($site, $formId);
            echo json_encode(['status' => 'success', 'submissions' => $submissions]);
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

                <!-- Navigation -->
                <div v-if="currentView !== 'sites'" class="mb-8">
                    <button @click="setView('sites')" class="text-sm border border-[#2A2A2A] px-3 py-1 bg-[#181818] hover:border-white transition-all">← Back to Sites</button>
                    <span class="mx-3 opacity-30">/</span>
                    <span class="text-sm font-serif italic">{{ activeSite }}</span>
                    <span v-if="currentView === 'submissions'" class="mx-3 opacity-30">/</span>
                    <span v-if="currentView === 'submissions'" class="text-sm font-serif italic">{{ activeForm?.title }} Submissions</span>
                </div>

                <!-- Sites List -->
                <div v-if="currentView === 'sites'">
                    <admin-sites-list 
                        :sites="sites" 
                        @download="downloadSite" 
                        @upload="triggerUpload" 
                        @manage-forms="manageForms"
                    ></admin-sites-list>
                </div>

                <!-- Forms Manager View -->
                <div v-if="currentView === 'forms'">
                    <admin-forms-manager 
                        :site="activeSite" 
                        :forms="siteForms"
                        @create="createNewForm"
                        @edit="editForm"
                        @delete="deleteForm"
                        @view-submissions="viewSubmissions"
                    ></admin-forms-manager>
                </div>

                <!-- Form Editor View -->
                <div v-if="currentView === 'editor'">
                    <admin-form-editor 
                        :form="editingForm"
                        @save="saveForm"
                        @cancel="setView('forms')"
                    ></admin-form-editor>
                </div>

                <!-- Submissions View -->
                <div v-if="currentView === 'submissions'">
                    <admin-submissions 
                        :form="activeForm" 
                        :submissions="submissions"
                        @export="exportSubmissions"
                    ></admin-submissions>
                </div>

                <!-- AI Tasks Section (Only on sites view) -->
                <admin-ai-tasks v-if="currentView === 'sites'" :passkey="passkey"></admin-ai-tasks>
            </div>

                <input type="file" accept=".zip" ref="uploadInput" class="hidden" @change="onFileSelected" />

                <div v-if="sites.length === 0" class="border border-dashed border-[#2A2A2A] py-20 text-center opacity-30 mt-8">
                    <p class="text-sm italic">No sites found.</p>
                </div>
            </div>
        </main>
    </div>

    <script type="module">
        import { createApp, ref, onMounted, nextTick, defineAsyncComponent } from 'https://unpkg.com/vue@3/dist/vue.esm-browser.js';

        const app = createApp({
            components: {
                AdminSitesList: defineAsyncComponent(() => import('/js/components/AdminSitesList.js')),
                AdminAiTasks: defineAsyncComponent(() => import('/js/components/AdminAiTasks.js')),
                AdminFormsManager: defineAsyncComponent(() => import('/js/components/AdminFormsManager.js')),
                AdminFormEditor: defineAsyncComponent(() => import('/js/components/AdminFormEditor.js')),
                AdminSubmissions: defineAsyncComponent(() => import('/js/components/AdminSubmissions.js'))
            },
            setup() {
                const isAuthenticated = ref(false);
                const passkey = ref('');
                const errorMsg = ref('');
                const sites = ref([]);
                const uploadInput = ref(null);
                const siteToUpload = ref('');

                // Forms State
                const currentView = ref('sites');
                const activeSite = ref('');
                const activeForm = ref(null);
                const siteForms = ref([]);
                const submissions = ref([]);
                const editingForm = ref({ title: '', slug: '', submit_label: 'Submit', fields: [] });

                const loadLucide = async () => {
                    await nextTick();
                    if(window.lucide) {
                        window.lucide.createIcons();
                    }
                };

                const setView = async (view) => {
                    currentView.value = view;
                    await loadLucide();
                };

                const manageForms = async (site) => {
                    activeSite.value = site;
                    await fetchSiteForms();
                    setView('forms');
                };

                const fetchSiteForms = async () => {
                    const storedKey = localStorage.getItem('adminPasskey');
                    try {
                        const res = await fetch(`/admin/api/forms?site=${activeSite.value}`, {
                            headers: { 'X-Admin-Passkey': storedKey }
                        });
                        const data = await res.json();
                        siteForms.value = data.forms || [];
                    } catch (e) {
                        console.error('Error fetching forms', e);
                    }
                };

                const createNewForm = () => {
                    editingForm.value = {
                        title: '',
                        slug: '',
                        submit_label: 'Submit',
                        fields: [
                            { label: 'Name', name: 'name', type: 'text', required: true },
                            { label: 'Email', name: 'email', type: 'email', required: true },
                            { label: 'Message', name: 'message', type: 'textarea', required: true }
                        ]
                    };
                    setView('editor');
                };

                const editForm = (form) => {
                    editingForm.value = JSON.parse(JSON.stringify(form));
                    // Rehydrate options_string for select fields
                    editingForm.value.fields.forEach(f => {
                        if (f.type === 'select' && f.options) {
                            f.options_string = f.options.join(', ');
                        }
                    });
                    setView('editor');
                };

                const addField = () => {
                    editingForm.value.fields.push({ label: 'New Field', name: 'field_' + Date.now(), type: 'text', required: false });
                };

                const removeField = (index) => {
                    editingForm.value.fields.splice(index, 1);
                };

                const updateOptions = (field) => {
                    field.options = field.options_string.split(',').map(s => s.trim()).filter(s => s !== '');
                };

                const saveForm = async (formData) => {
                    const storedKey = localStorage.getItem('adminPasskey');
                    try {
                        const res = await fetch('/admin/api/forms/save', {
                            method: 'POST',
                            headers: { 
                                'X-Admin-Passkey': storedKey,
                                'Content-Type': 'application/json'
                            },
                            body: JSON.stringify({
                                site: activeSite.value,
                                form: formData
                            })
                        });
                        const data = await res.json();
                        if (data.status === 'success') {
                            await fetchSiteForms();
                            setView('forms');
                        }
                    } catch (e) {
                        alert('Save failed');
                    }
                };

                const deleteForm = async (formId) => {
                    if (!confirm('Are you sure you want to delete this form and all its submissions?')) return;
                    const storedKey = localStorage.getItem('adminPasskey');
                    try {
                        await fetch('/admin/api/forms/delete', {
                            method: 'POST',
                            headers: { 
                                'X-Admin-Passkey': storedKey,
                                'Content-Type': 'application/json'
                            },
                            body: JSON.stringify({
                                site: activeSite.value,
                                form_id: formId
                            })
                        });
                        await fetchSiteForms();
                    } catch (e) {
                        alert('Delete failed');
                    }
                };

                const viewSubmissions = async (form) => {
                    activeForm.value = form;
                    const storedKey = localStorage.getItem('adminPasskey');
                    try {
                        const res = await fetch(`/admin/api/forms/submissions?site=${activeSite.value}&form_id=${form.id}`, {
                            headers: { 'X-Admin-Passkey': storedKey }
                        });
                        const data = await res.json();
                        submissions.value = (data.submissions || []).reverse();
                        setView('submissions');
                    } catch (e) {
                        console.error('Error fetching submissions');
                    }
                };

                const exportSubmissions = () => {
                    const json = JSON.stringify(submissions.value, null, 2);
                    const blob = new Blob([json], { type: 'application/json' });
                    const url = URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.href = url;
                    a.download = `submissions_${activeForm.value.id}.json`;
                    a.click();
                    URL.revokeObjectURL(url);
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
                        currentView,
                        activeSite,
                        activeForm,
                        siteForms,
                        submissions,
                        editingForm,
                        setView,
                        manageForms,
                        createNewForm,
                        editForm,
                        addField,
                        removeField,
                        updateOptions,
                        saveForm,
                        deleteForm,
                        viewSubmissions,
                        exportSubmissions,
                        login,
                        logout,
                        downloadSite,
                        triggerUpload,
                        onFileSelected,
                        uploadInput
                    };
                }
            });
            
            app.mount('#app');
        </script>
</body>
</html>
        <?php
    }
}
