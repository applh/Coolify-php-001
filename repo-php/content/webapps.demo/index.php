<?php
// webapps.demo main entry
$request_uri = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH);
$path = trim($request_uri, '/');

$pages = [
    '' => ['title' => 'LogiFlow - Intelligent Supply Chain Management'],
    'features' => ['title' => 'Features - LogiFlow'],
    'solution' => ['title' => 'The Solution - LogiFlow'],
    'customers' => ['title' => 'Our Customers - LogiFlow'],
    'pricing' => ['title' => 'Pricing Plans - LogiFlow'],
    'api' => ['title' => 'Developer API - LogiFlow'],
    'blog' => ['title' => 'Insights - LogiFlow'],
    'about' => ['title' => 'About Us - LogiFlow'],
    'contact' => ['title' => 'Contact Sales - LogiFlow'],
    'careers' => ['title' => 'Join the Team - LogiFlow'],
    'app' => ['title' => 'Control Center - LogiFlow Dashboard'],
];

if (!isset($pages[$path])) $path = '';
$currentPage = $pages[$path];

// For the map, we'll use a public placeholder or a real API if provided
$mapsApiKey = getenv('GOOGLE_MAPS_PLATFORM_KEY') ?: '';

?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?php echo htmlspecialchars($currentPage['title'], ENT_QUOTES, 'UTF-8'); ?></title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://fonts.googleapis.com/css2?family=IBM+Plex+Sans:wght@400;500;600;700&family=IBM+Plex+Mono&display=swap" rel="stylesheet">
    <style>
        body { font-family: 'IBM Plex Sans', sans-serif; }
        .mono { font-family: 'IBM Plex Mono', monospace; }
    </style>
</head>
<body class="bg-slate-50 text-slate-900 antialiased">

    <?php if ($path !== 'app'): ?>
        <!-- Public Website Header -->
        <nav class="bg-white border-b border-slate-200 py-6 px-8 flex justify-between items-center sticky top-0 z-50">
            <div class="flex items-center gap-12">
                <a href="/" class="text-2xl font-bold tracking-tighter text-blue-600">LOGIFLOW</a>
                <div class="hidden lg:flex gap-8 text-sm font-semibold text-slate-500">
                    <a href="/features" class="hover:text-blue-600">Features</a>
                    <a href="/solution" class="hover:text-blue-600">Solution</a>
                    <a href="/pricing" class="hover:text-blue-600">Pricing</a>
                    <a href="/api" class="hover:text-blue-600">API</a>
                </div>
            </div>
            <div class="flex items-center gap-4">
                <a href="/app" class="px-6 py-3 bg-blue-600 text-white text-sm font-bold rounded-xl hover:bg-blue-700 shadow-lg shadow-blue-100 transition-all">Launch Dashboard</a>
            </div>
        </nav>
    <?php endif; ?>

    <main>
        <?php if ($path === 'app'): ?>
            <!-- Interactive Dashboard Application -->
            <div class="flex h-screen overflow-hidden bg-white">
                <!-- App Sidebar -->
                <aside class="w-72 border-r border-slate-200 flex flex-col pt-6 bg-slate-50/50">
                    <div class="px-6 mb-10 flex items-center gap-3">
                        <div class="w-8 h-8 bg-blue-600 rounded-lg"></div>
                        <span class="font-bold tracking-tighter text-slate-900">CONTROL CENTER</span>
                    </div>
                    <nav class="flex-grow px-3 space-y-1">
                        <a href="#" class="flex items-center gap-3 px-3 py-2.5 bg-blue-50 text-blue-700 rounded-lg font-semibold text-sm">
                            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path><polyline points="9 22 9 12 15 12 15 22"></polyline></svg>
                            Live Fleet
                        </a>
                        <a href="#" class="flex items-center gap-3 px-3 py-2.5 text-slate-500 hover:bg-slate-100 rounded-lg font-medium text-sm transition-colors">
                            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
                            History
                        </a>
                        <a href="#" class="flex items-center gap-3 px-3 py-2.5 text-slate-500 hover:bg-slate-100 rounded-lg font-medium text-sm transition-colors">
                            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path><polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline><line x1="12" y1="22.08" x2="12" y2="12"></line></svg>
                            Inventory
                        </a>
                        <a href="#" class="flex items-center gap-3 px-3 py-2.5 text-slate-500 hover:bg-slate-100 rounded-lg font-medium text-sm transition-colors">
                            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1 5.07 5.07 0 0 0 5 4.77 5.44 5.44 0 0 0 3.5 8.55c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22"></path></svg>
                            Integrations
                        </a>
                    </nav>
                    <div class="p-6 border-t border-slate-200">
                        <a href="/" class="text-xs font-bold text-slate-400 hover:text-red-500 transition-colors uppercase tracking-widest">Logout</a>
                    </div>
                </aside>

                <!-- App Main Area -->
                <div class="flex-grow flex flex-col relative">
                    <!-- Dashboard Header -->
                    <header class="h-16 px-8 flex items-center justify-between border-b border-slate-100 bg-white shadow-sm z-10">
                        <h2 class="font-bold text-slate-800">Fleet Overview (Live)</h2>
                        <div class="flex items-center gap-6">
                            <div class="flex items-center gap-2">
                                <div class="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
                                <span class="text-xs font-bold text-slate-500 uppercase tracking-wide">Syncing</span>
                            </div>
                            <div class="w-8 h-8 bg-zinc-100 rounded-full"></div>
                        </div>
                    </header>

                    <!-- Map Container -->
                    <div id="map" class="flex-grow bg-slate-200 relative">
                        <?php if (!$mapsApiKey): ?>
                            <div class="absolute inset-0 flex items-center justify-center p-12 text-center bg-slate-100 italic text-slate-500">
                                <div>
                                    <p class="text-xl font-bold mb-2">Interactive Map Component</p>
                                    <p class="max-w-md mx-auto">Please set GOOGLE_MAPS_PLATFORM_KEY in your environment to enable real-time fleet tracking features.</p>
                                    <div class="mt-4 p-4 border border-dashed border-slate-300 rounded-lg bg-white/50 inline-block">
                                        <span class="mono text-xs text-blue-600">[MAP_VIEWPORT_PLACEHOLDER]</span>
                                    </div>
                                </div>
                            </div>
                        <?php endif; ?>
                    </div>

                    <!-- Overlay List (Optional) -->
                    <div class="absolute bottom-6 left-6 w-80 bg-white/90 backdrop-blur-md rounded-2xl shadow-2xl border border-slate-200/50 p-4 z-20">
                        <h3 class="text-xs font-extrabold text-slate-400 uppercase tracking-widest mb-4">Active Shipments</h3>
                        <div class="space-y-3">
                            <div class="p-3 bg-white rounded-xl border border-slate-100 flex justify-between items-center">
                                <div>
                                    <div class="text-sm font-bold text-slate-800">#NY-2093</div>
                                    <div class="text-[10px] text-slate-400 font-mono">EN ROUTE &bull; ETA 14:30</div>
                                </div>
                                <div class="px-2 py-1 bg-green-100 text-green-700 text-[10px] font-bold rounded uppercase">On Time</div>
                            </div>
                            <div class="p-3 bg-white rounded-xl border border-slate-100 flex justify-between items-center opacity-60">
                                <div>
                                    <div class="text-sm font-bold text-slate-800">#CH-9912</div>
                                    <div class="text-[10px] text-slate-400 font-mono">DELIVERED</div>
                                </div>
                                <div class="px-2 py-1 bg-slate-100 text-slate-700 text-[10px] font-bold rounded uppercase">Final</div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <script>
                // Map initialization logic
                <?php if ($mapsApiKey): ?>
                 (g=>{var h,a,k,p="The Google Maps JavaScript API",c="google",l="importLibrary",q="__ib__",m=document,b=window;b=b[c]||(b[c]={});var d=b.maps||(b.maps={}),r=new Set,e=new URLSearchParams,u=()=>h||(h=new Promise(async(f,n)=>{await (a=m.createElement("script"));e.set("libraries",[...r]+"");for(k in g)e.set(k.replace(/[A-Z]/g,t=>"_"+t.toLowerCase()),g[k]);e.set("callback",c+".maps."+q);a.src=`https://maps.${c}apis.com/maps/api/js?`+e;d[q]=f;a.onerror=()=>h=n(Error(p+" could not load."));a.nonce=m.querySelector("script[nonce]")?.nonce||"";m.head.append(a)}));d[l]?console.warn(p+" only loads once. Better to help"):d[l]=(f,...n)=>r.add(f)&&u().then(()=>d[l](f,...n))})({
                    key: "<?php echo $mapsApiKey; ?>",
                    v: "weekly",
                  });

                  async function initMap() {
                    const { Map } = await google.maps.importLibrary("maps");
                    const { AdvancedMarkerElement } = await google.maps.importLibrary("marker");

                    const map = new Map(document.getElementById("map"), {
                      center: { lat: 40.7128, lng: -74.0060 },
                      zoom: 12,
                      mapId: "DEMO_MAP_ID",
                      mapTypeControl: false,
                      streetViewControl: false,
                      fullscreenControl: false
                    });

                    // Add a few fleet markers
                    const fleetPositions = [
                      { lat: 40.7306, lng: -73.9352, id: 'NY-2093' },
                      { lat: 40.6782, lng: -73.9442, id: 'BK-1102' }
                    ];

                    fleetPositions.forEach(pos => {
                      new AdvancedMarkerElement({
                        map,
                        position: { lat: pos.lat, lng: pos.lng },
                        title: `Vehicle ${pos.id}`
                      });
                    });
                  }

                  initMap();
                <?php endif; ?>
            </script>
        <?php else: ?>
            <!-- Splash Website Content -->
            <section class="py-24 px-8 max-w-7xl mx-auto">
                <div class="grid grid-cols-1 md:grid-cols-2 gap-20 items-center">
                    <div>
                        <h1 class="text-6xl font-bold tracking-tighter leading-tight mb-8">
                             Next-gen <br/> <span class="text-blue-600 underline">Logistics</span> orchestration.
                        </h1>
                        <p class="text-xl text-slate-500 leading-relaxed max-w-lg mb-10">
                            LogiFlow helps the world's largest fleets visualize, analyze, and optimize every single mile in real-time.
                        </p>
                        <ul class="space-y-4 mb-10">
                            <li class="flex items-center gap-3 font-semibold text-slate-700">
                                <svg class="text-green-500" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
                                Real-time fleet visualization
                            </li>
                            <li class="flex items-center gap-3 font-semibold text-slate-700">
                                <svg class="text-green-500" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
                                AI-powered route optimization
                            </li>
                        </ul>
                        <div class="flex gap-4">
                             <a href="/app" class="px-8 py-4 bg-blue-600 text-white font-bold rounded-2xl">Try Interactive App</a>
                        </div>
                    </div>
                    <div class="bg-slate-200 aspect-square rounded-[3rem] overflow-hidden shadow-2xl relative group">
                         <div class="absolute inset-0 bg-gradient-to-br from-blue-600/20 to-transparent"></div>
                         <div class="absolute inset-10 bg-white/40 backdrop-blur rounded-2xl border border-white/50 p-6">
                            <div class="h-4 bg-white/80 rounded w-1/2 mb-4"></div>
                            <div class="h-2 bg-white/40 rounded w-full mb-2"></div>
                            <div class="h-2 bg-white/40 rounded w-5/6"></div>
                         </div>
                    </div>
                </div>
            </section>
        <?php endif; ?>
    </main>

    <?php if ($path !== 'app'): ?>
    <footer class="py-20 px-8 border-t border-slate-200 mt-20">
        <div class="max-w-7xl mx-auto flex flex-col md:flex-row justify-between items-start gap-12">
            <div>
                <div class="text-2xl font-bold tracking-tighter text-blue-600 mb-6">LOGIFLOW</div>
                <p class="text-slate-400 text-sm max-w-xs uppercase font-bold tracking-widest leading-loose font-mono">Moving bits to move atoms faster.</p>
            </div>
            <div class="grid grid-cols-2 gap-20">
                 <div>
                    <h4 class="text-xs font-bold text-slate-300 uppercase tracking-widest mb-6">Explore</h4>
                    <ul class="text-sm space-y-4 font-semibold text-slate-500">
                        <li><a href="/features" class="hover:text-blue-600">Features</a></li>
                        <li><a href="/pricing" class="hover:text-blue-600">Pricing</a></li>
                        <li><a href="/app" class="hover:text-blue-600">Dashboard</a></li>
                    </ul>
                 </div>
                 <div>
                    <h4 class="text-xs font-bold text-slate-300 uppercase tracking-widest mb-6">Legal</h4>
                    <ul class="text-sm space-y-4 font-semibold text-slate-500">
                        <li><a href="/privacy" class="hover:text-blue-600">Privacy</a></li>
                        <li><a href="/terms" class="hover:text-blue-600">Terms</a></li>
                        <li><a href="/contact" class="hover:text-blue-600">Contact</a></li>
                    </ul>
                 </div>
            </div>
        </div>
    </footer>
    <?php endif; ?>

</body>
</html>
