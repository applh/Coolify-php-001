<?php
// CMS.demo main entry
$request_uri = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH);
$path = trim($request_uri, '/');

// Handle API Routes first
if (strpos($path, 'app/api') === 0) {
    header('Content-Type: application/json');
    $endpoint = str_replace('app/api/', '', $path);
    $data = [
        'status' => 'success',
        'timestamp' => time(),
        'endpoint' => $endpoint,
        'payload' => []
    ];
    
    switch ($endpoint) {
        case 'site-info':
            $data['payload'] = [
                'name' => 'CMS Explorer Demo',
                'version' => '2.4.0',
                'engine' => 'PHP 8.2+ Hierarchical CMS'
            ];
            break;
        case 'stats':
            $data['payload'] = [
                'requests_today' => rand(1000, 5000),
                'active_users' => rand(50, 200),
                'uptime' => '99.998%'
            ];
            break;
        default:
            $data['status'] = 'error';
            $data['message'] = 'Endpoint not found';
            http_response_code(404);
            break;
    }
    echo json_encode($data);
    exit;
}

$pages = [
    '' => ['title' => 'CoreCMS - The Developer First Platform'],
    'architecture' => ['title' => 'System Architecture - CoreCMS'],
    'integration' => ['title' => 'Integration Guide - CoreCMS'],
    'components' => ['title' => 'Custom Components - CoreCMS'],
    'plugins' => ['title' => 'Plugin System - CoreCMS'],
    'security' => ['title' => 'Hardened Security - CoreCMS'],
    'hosting' => ['title' => 'Cloud Deployment - CoreCMS'],
    'about' => ['title' => 'Our Identity - CoreCMS'],
    'contact' => ['title' => 'Speak with Experts - CoreCMS'],
    'faq' => ['title' => 'Help & FAQ - CoreCMS'],
    'app' => ['title' => 'API Explorer - CoreCMS Dashboard'],
];

if (!isset($pages[$path])) $path = '';
$currentPage = $pages[$path];

?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?php echo htmlspecialchars($currentPage['title'], ENT_QUOTES, 'UTF-8'); ?></title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;700&family=Space+Grotesk:wght@300;500;700&display=swap" rel="stylesheet">
    <style>
        body { font-family: 'Space Grotesk', sans-serif; }
        .mono { font-family: 'JetBrains Mono', monospace; }
    </style>
</head>
<body class="bg-[#050505] text-white antialiased selection:bg-emerald-500 selection:text-black">

    <?php if ($path !== 'app'): ?>
        <nav class="p-8 flex justify-between items-center border-b border-white/10 sticky top-0 bg-black/50 backdrop-blur-xl z-50">
            <a href="/" class="text-2xl font-bold tracking-tighter flex items-center gap-3">
                 <div class="w-2 h-2 rounded-full bg-emerald-500 shadow-[0_0_15px_rgba(16,185,129,0.5)]"></div>
                 CORECMS
            </a>
            <div class="hidden lg:flex items-center gap-10 text-xs font-bold uppercase tracking-widest text-zinc-500">
                <a href="/architecture" class="hover:text-emerald-500 transition-colors">Architecture</a>
                <a href="/plugins" class="hover:text-emerald-500 transition-colors">Plugins</a>
                <a href="/security" class="hover:text-emerald-500 transition-colors">Security</a>
                <a href="/contact" class="hover:text-emerald-500 transition-colors">Contact</a>
                <a href="/app" class="px-5 py-2 border border-emerald-500 text-emerald-500 rounded-full hover:bg-emerald-500 hover:text-black transition-all">API Explorer</a>
            </div>
        </nav>
    <?php endif; ?>

    <main>
        <?php if ($path === 'app'): ?>
            <div class="flex h-screen bg-[#0a0a0a]">
                <!-- Dash Sidebar -->
                <aside class="w-80 border-r border-white/5 flex flex-col pt-10">
                    <div class="px-10 mb-20">
                         <a href="/" class="text-xl font-bold tracking-tighter hover:opacity-70">&larr; CORECMS</a>
                    </div>
                    <nav class="flex-grow px-4 space-y-2">
                        <div class="px-6 py-2 text-[10px] uppercase tracking-widest font-bold text-zinc-600">Dynamic Endpoints</div>
                        <button onclick="fetchApi('site-info')" class="w-full flex items-center gap-4 px-6 py-4 hover:bg-white/5 rounded-2xl transition-all text-sm font-bold text-emerald-500/80 hover:text-emerald-500 text-left">
                            <span class="mono">GET</span> /app/api/site-info
                        </button>
                        <button onclick="fetchApi('stats')" class="w-full flex items-center gap-4 px-6 py-4 hover:bg-white/5 rounded-2xl transition-all text-sm font-bold text-emerald-500/80 hover:text-emerald-500 text-left">
                            <span class="mono">GET</span> /app/api/stats
                        </button>
                    </nav>
                </aside>

                <!-- Dash Content -->
                <div class="flex-grow flex flex-col p-10 overflow-hidden">
                    <div class="mb-10">
                        <h1 class="text-3xl font-bold mb-2">API Console</h1>
                        <p class="text-zinc-500 text-sm">Interact with real-time dynamic routes handled by the CMS engine.</p>
                    </div>

                    <div class="flex-grow bg-black rounded-3xl border border-white/5 p-8 flex flex-col gap-6 overflow-hidden">
                        <div class="flex items-center justify-between border-b border-white/5 pb-4">
                            <div class="flex items-center gap-2">
                                <div class="w-3 h-3 rounded-full bg-zinc-800"></div>
                                <div class="w-3 h-3 rounded-full bg-zinc-800"></div>
                                <div class="w-3 h-3 rounded-full bg-zinc-800"></div>
                            </div>
                            <span class="mono text-xs text-zinc-600">Response Viewer</span>
                        </div>
                        <pre id="output" class="flex-grow mono text-sm text-emerald-400 overflow-auto scrollbar-hide py-4">{ "message": "Select an endpoint to begin testing" }</pre>
                    </div>
                </div>
            </div>

            <script>
                async function fetchApi(endpoint) {
                    const output = document.getElementById('output');
                    output.innerText = 'Fetching ' + endpoint + '...';
                    output.classList.add('animate-pulse');
                    
                    try {
                        const res = await fetch('/app/api/' + endpoint);
                        const data = await res.json();
                        output.innerText = JSON.stringify(data, null, 2);
                    } catch (e) {
                        output.innerText = 'Error: ' + e.message;
                    } finally {
                        output.classList.remove('animate-pulse');
                    }
                }
            </script>

        <?php else: ?>
            <!-- Sales Site -->
            <section class="py-32 px-10 max-w-6xl mx-auto">
                <div class="text-center mb-32">
                    <h1 class="text-7xl md:text-9xl font-bold tracking-tighter mb-10 leading-[0.9]">
                        CMS for <br/> <span class="text-zinc-500 font-light underline decoration-emerald-500/50">Architects</span>.
                    </h1>
                     <p class="text-xl text-zinc-400 max-w-2xl mx-auto mb-12">
                        Build complex, high-performance web applications with a hierarchical PHP backend and a flexible API layer. 
                    </p>
                    <div class="flex flex-col sm:flex-row justify-center gap-6">
                        <a href="/app" class="px-10 py-5 bg-white text-black font-bold rounded-full hover:scale-105 transition-transform">Explore API Console</a>
                        <a href="/architecture" class="px-10 py-5 border border-white/20 text-white font-bold rounded-full hover:border-white transition-colors">Documentation</a>
                    </div>
                </div>

                <div class="grid grid-cols-1 md:grid-cols-3 gap-10">
                    <div class="p-10 bg-white/5 rounded-[2.5rem] border border-white/10 group hover:border-emerald-500/30 transition-all">
                        <div class="text-3xl mb-6">⚙️</div>
                        <h3 class="text-xl font-bold mb-4 italic">Dynamic Routing</h3>
                        <p class="text-zinc-500 text-sm leading-relaxed">Instantly map any incoming request to dynamic controllers or static views with zero configuration overhead.</p>
                    </div>
                    <div class="p-10 bg-white/5 rounded-[2.5rem] border border-white/10 group hover:border-emerald-500/30 transition-all">
                        <div class="text-3xl mb-6">🔒</div>
                        <h3 class="text-xl font-bold mb-4 italic">Hardened Security</h3>
                        <p class="text-zinc-500 text-sm leading-relaxed">Integrated XSS protection, CSRF guards, and secure environment orchestration from the ground up.</p>
                    </div>
                    <div class="p-10 bg-white/5 rounded-[2.5rem] border border-white/10 group hover:border-emerald-500/30 transition-all">
                        <div class="text-3xl mb-6">🚀</div>
                        <h3 class="text-xl font-bold mb-4 italic">Plugin Engine</h3>
                        <p class="text-zinc-500 text-sm leading-relaxed">Extend the core capabilities using a standardized plugin architecture that keeps your codebase clean.</p>
                    </div>
                </div>
            </section>
        <?php endif; ?>
    </main>

    <?php if ($path !== 'app'): ?>
        <footer class="p-10 border-t border-white/10 mt-32">
             <div class="max-w-6xl mx-auto flex flex-col md:flex-row justify-between items-center gap-10">
                <div class="text-xl font-bold tracking-tighter">CORECMS</div>
                <div class="flex gap-10 text-[10px] font-bold uppercase tracking-[0.2em] text-zinc-600">
                    <a href="/terms" class="hover:text-white transition-colors">Terms</a>
                    <a href="/privacy" class="hover:text-white transition-colors">Privacy</a>
                    <a href="/faq" class="hover:text-white transition-colors">Support</a>
                </div>
                <p class="text-zinc-700 text-xs mono">&copy; <?php echo date('Y'); ?> // SYSTEM_ROOT // CORE_BUILD_BETA</p>
             </div>
        </footer>
    <?php endif; ?>

</body>
</html>
