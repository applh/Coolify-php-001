<?php
// startup.demo main entry
$request_uri = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH);
$path = trim($request_uri, '/');

// 10 Pages Definition
$pages = [
    '' => ['title' => 'Nexus - Scale Your Engineering Output', 'view' => 'home'],
    'features' => ['title' => 'Powerful Features - Nexus', 'view' => 'features'],
    'pricing' => ['title' => 'Flexible Pricing - Nexus', 'view' => 'pricing'],
    'about' => ['title' => 'Our Mission - Nexus', 'view' => 'about'],
    'contact' => ['title' => 'Contact Us - Nexus', 'view' => 'contact'],
    'login' => ['title' => 'Log In - Nexus', 'view' => 'login'],
    'signup' => ['title' => 'Start Your Free Trial - Nexus', 'view' => 'signup'],
    'docs' => ['title' => 'Documentation - Nexus', 'view' => 'docs'],
    'terms' => ['title' => 'Terms of Service - Nexus', 'view' => 'terms'],
    'privacy' => ['title' => 'Privacy Policy - Nexus', 'view' => 'privacy'],
];

// Simple Dispatcher
if (!isset($pages[$path])) {
    // Fallback or 404
    if ($path !== '' && !headers_sent()) {
        // Simple regex fallback for deep paths if needed
    }
    $path = ''; // Default to home
}

$currentPage = $pages[$path];

?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?php echo htmlspecialchars($currentPage['title'], ENT_QUOTES, 'UTF-8'); ?></title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://fonts.googleapis.com/css2?family=Uncut+Sans:wght@400;500;700&display=swap" rel="stylesheet">
    <style>
        body { font-family: 'Uncut Sans', sans-serif; }
    </style>
</head>
<body class="bg-white text-zinc-900 antialiased">
    <!-- Navbar -->
    <nav class="border-b border-zinc-100 py-4 px-6 sticky top-0 bg-white/80 backdrop-blur-md z-50">
        <div class="max-w-7xl mx-auto flex items-center justify-between">
            <div class="flex items-center gap-10">
                <a href="/" class="text-2xl font-bold tracking-tight">NEXUS</a>
                <div class="hidden md:flex gap-6 text-sm font-medium text-zinc-500">
                    <a href="/features" class="hover:text-zinc-900 <?php echo $path === 'features' ? 'text-zinc-900' : ''; ?>">Features</a>
                    <a href="/pricing" class="hover:text-zinc-900 <?php echo $path === 'pricing' ? 'text-zinc-900' : ''; ?>">Pricing</a>
                    <a href="/about" class="hover:text-zinc-900 <?php echo $path === 'about' ? 'text-zinc-900' : ''; ?>">About</a>
                    <a href="/docs" class="hover:text-zinc-900 <?php echo $path === 'docs' ? 'text-zinc-900' : ''; ?>">Docs</a>
                </div>
            </div>
            <div class="flex items-center gap-4">
                <a href="/login" class="text-sm font-medium px-4 py-2 hover:bg-zinc-50 rounded-lg">Log in</a>
                <a href="/signup" class="text-sm font-bold bg-zinc-900 text-white px-5 py-2.5 rounded-lg hover:bg-zinc-800 transition-colors">Get Started</a>
            </div>
        </div>
    </nav>

    <main class="min-h-[70vh]">
        <?php if ($path === ''): ?>
            <!-- Hero -->
            <section class="pt-24 pb-20 px-6 text-center">
                <div class="max-w-4xl mx-auto">
                    <h1 class="text-6xl md:text-8xl font-bold tracking-tighter mb-8 leading-[1.05]">
                        The engineering <br class="hidden md:block"/> platform for <span class="bg-gradient-to-r from-blue-600 to-indigo-600 bg-clip-text text-transparent">fast teams</span>.
                    </h1>
                    <p class="text-xl text-zinc-500 mb-12 max-w-2xl mx-auto leading-relaxed">
                        Nexus automates the heavy lifting of infrastructure, so your team can focus on shipping features that customers love.
                    </p>
                    <div class="flex flex-col sm:flex-row justify-center gap-4">
                        <input type="email" placeholder="Work email" class="px-6 py-4 rounded-xl border border-zinc-200 focus:ring-2 focus:ring-blue-500 outline-none w-full sm:w-80">
                        <button class="px-8 py-4 bg-zinc-900 text-white font-bold rounded-xl hover:bg-zinc-800">Start for free</button>
                    </div>
                    <p class="mt-4 text-sm text-zinc-400">No credit card required. Cancel anytime.</p>
                </div>
            </section>
        <?php elseif ($path === 'features'): ?>
            <section class="py-20 px-6 max-w-7xl mx-auto">
                <h1 class="text-4xl font-bold mb-12">Product Features</h1>
                <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
                    <?php for($i=1; $i<=6; $i++): ?>
                    <div class="p-8 border border-zinc-100 rounded-3xl bg-zinc-50/50">
                        <div class="w-12 h-12 bg-blue-100 rounded-xl mb-6 flex items-center justify-center text-blue-600">
                             <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m21 16-4 4-4-4"></path><path d="M17 20V4"></path><path d="m3 8 4-4 4 4"></path><path d="M7 4v16"></path></svg>
                        </div>
                        <h3 class="text-xl font-bold mb-3">Feature #<?php echo $i; ?></h3>
                        <p class="text-zinc-500 leading-relaxed">Highly optimized workflow for teams looking to scale their deployments without the overhead.</p>
                    </div>
                    <?php endfor; ?>
                </div>
            </section>
        <?php elseif ($path === 'pricing'): ?>
             <section class="py-20 px-6 max-w-7xl mx-auto text-center">
                <h1 class="text-4xl font-bold mb-4">Simple, transparent pricing</h1>
                <p class="text-zinc-500 mb-16">Choose the plan that's right for your stage of growth.</p>
                <div class="grid grid-cols-1 md:grid-cols-3 gap-8 text-left">
                    <div class="p-10 border border-zinc-100 rounded-3xl">
                        <h3 class="font-bold text-lg mb-2">Starter</h3>
                        <div class="text-4xl font-bold mb-6">$0 <span class="text-lg font-normal text-zinc-400">/mo</span></div>
                        <ul class="space-y-4 text-zinc-500 mb-10">
                            <li>Up to 3 users</li>
                            <li>10 projects</li>
                            <li>Community support</li>
                        </ul>
                        <button class="w-full py-3 border border-zinc-200 rounded-xl font-bold">Get Started</button>
                    </div>
                    <div class="p-10 border-2 border-zinc-900 rounded-3xl relative">
                        <span class="absolute top-0 right-10 -translate-y-1/2 bg-zinc-900 text-white text-xs font-bold px-3 py-1 rounded-full uppercase">Most Popular</span>
                        <h3 class="font-bold text-lg mb-2">Pro</h3>
                        <div class="text-4xl font-bold mb-6">$49 <span class="text-lg font-normal text-zinc-400">/mo</span></div>
                        <ul class="space-y-4 text-zinc-500 mb-10">
                            <li>Unlimited users</li>
                            <li>Everything in Starter</li>
                            <li>Priority support</li>
                        </ul>
                        <button class="w-full py-3 bg-zinc-900 text-white rounded-xl font-bold">Try Pro</button>
                    </div>
                    <div class="p-10 border border-zinc-100 rounded-3xl">
                        <h3 class="font-bold text-lg mb-2">Enterprise</h3>
                        <div class="text-4xl font-bold mb-6">Custom</div>
                        <ul class="space-y-4 text-zinc-500 mb-10">
                            <li>Advanced security</li>
                            <li>SSO / SAML</li>
                            <li>Dedicated manager</li>
                        </ul>
                        <button class="w-full py-3 border border-zinc-200 rounded-xl font-bold">Contact Sales</button>
                    </div>
                </div>
             </section>
        <?php else: ?>
            <section class="py-20 px-6 max-w-3xl mx-auto">
                 <h1 class="text-4xl font-bold mb-8"><?php echo $currentPage['title']; ?></h1>
                 <div class="prose prose-zinc max-w-none text-zinc-600 leading-relaxed space-y-6">
                    <p>This is the content for the <strong><?php echo $path; ?></strong> page of the Startup demo.</p>
                    <p>Nexus is designed to showcase how a multi-page setup works seamlessly with our hierarchical PHP CMS routing strategy. Every page here is dynamically served from the same entry point while maintaining clean SEO-friendly URLs.</p>
                    <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.</p>
                 </div>
            </section>
        <?php endif; ?>
    </main>

    <footer class="bg-zinc-50 border-t border-zinc-100 py-20 px-6 mt-20">
        <div class="max-w-7xl mx-auto">
            <div class="grid grid-cols-2 md:grid-cols-4 gap-12 mb-16">
                <div class="col-span-2 md:col-span-1">
                    <div class="text-xl font-bold mb-6">NEXUS</div>
                    <p class="text-sm text-zinc-500 leading-relaxed">Making software engineering accessible and automated for everyone.</p>
                </div>
                <div>
                    <h4 class="font-bold text-sm mb-6 uppercase tracking-wider">Product</h4>
                    <ul class="space-y-4 text-sm text-zinc-500">
                        <li><a href="/features" class="hover:text-zinc-900">Features</a></li>
                        <li><a href="/pricing" class="hover:text-zinc-900">Pricing</a></li>
                        <li><a href="/docs" class="hover:text-zinc-900">Documentation</a></li>
                    </ul>
                </div>
                <div>
                     <h4 class="font-bold text-sm mb-6 uppercase tracking-wider">Company</h4>
                    <ul class="space-y-4 text-sm text-zinc-500">
                        <li><a href="/about" class="hover:text-zinc-900">About</a></li>
                        <li><a href="/contact" class="hover:text-zinc-900">Contact</a></li>
                    </ul>
                </div>
                <div>
                     <h4 class="font-bold text-sm mb-6 uppercase tracking-wider">Legal</h4>
                    <ul class="space-y-4 text-sm text-zinc-500">
                        <li><a href="/terms" class="hover:text-zinc-900">Terms</a></li>
                        <li><a href="/privacy" class="hover:text-zinc-900">Privacy</a></li>
                    </ul>
                </div>
            </div>
            <div class="border-t border-zinc-200 pt-8 flex justify-between items-center text-xs text-zinc-400">
                <p>&copy; <?php echo date('Y'); ?> Nexus Technologies Inc.</p>
                <div class="flex gap-4">
                    <span>English (US)</span>
                </div>
            </div>
        </div>
    </footer>
</body>
</html>
