<?php
// startup-blog.demo main entry
$request_uri = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH);
$path = trim($request_uri, '/');

// 10 Pages Definition
$pages = [
    '' => ['title' => 'Vantage - Intelligence for Modern Business', 'view' => 'home'],
    'features' => ['title' => 'Features - Vantage', 'view' => 'features'],
    'pricing' => ['title' => 'Pricing - Vantage', 'view' => 'pricing'],
    'about' => ['title' => 'About Vantage', 'view' => 'about'],
    'contact' => ['title' => 'Get in Touch - Vantage', 'view' => 'contact'],
    'blog' => ['title' => 'Engineering & Design Blog - Vantage', 'view' => 'blog'],
    'login' => ['title' => 'Sign In - Vantage', 'view' => 'login'],
    'signup' => ['title' => 'Create Account - Vantage', 'view' => 'signup'],
    'resources' => ['title' => 'Help Center - Vantage', 'view' => 'resources'],
    'community' => ['title' => 'Our Community - Vantage', 'view' => 'community'],
];

// Generate 30 Blog Articles
$articles = [];
for ($i = 1; $i <= 30; $i++) {
    $articles["blog/post-$i"] = [
        'id' => $i,
        'slug' => "post-$i",
        'title' => "How we scaled our architecture to " . ($i * 100) . "k users",
        'date' => date('M j, Y', strtotime("-$i days")),
        'author' => 'Engineering Team',
        'category' => $i % 2 === 0 ? 'Scaling' : 'Infrastructure',
        'excerpt' => "In this post, we dive deep into the challenges we faced during the phase $i of our infrastructure migration.",
        'content' => "This is the full content for article #$i. It contains deep insights, technical charts, and code snippets demonstrating our progress. Scaling isn't just about adding more servers; it's about optimizing the ones you have."
    ];
}

$isBlogPost = strpos($path, 'blog/post-') === 0 && isset($articles[$path]);
$currentPage = $isBlogPost ? $articles[$path] : ($pages[$path] ?? $pages['']);

?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?php echo htmlspecialchars($currentPage['title'], ENT_QUOTES, 'UTF-8'); ?></title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;700&display=swap" rel="stylesheet">
    <style>
        body { font-family: 'Outfit', sans-serif; }
    </style>
</head>
<body class="bg-[#fafafa] text-slate-900 antialiased">
    <!-- Navbar -->
    <nav class="bg-white border-b border-slate-200 py-4 px-6 sticky top-0 z-50">
        <div class="max-w-7xl mx-auto flex items-center justify-between">
            <div class="flex items-center gap-12">
                <a href="/" class="text-2xl font-bold flex items-center gap-2">
                    <div class="w-8 h-8 bg-black rounded-lg flex items-center justify-center">
                        <div class="w-3 h-3 bg-white rounded-full"></div>
                    </div>
                    VANTAGE
                </a>
                <div class="hidden md:flex gap-8 text-sm font-medium text-slate-500">
                    <a href="/blog" class="hover:text-black <?php echo $path === 'blog' ? 'text-black' : ''; ?>">Blog</a>
                    <a href="/features" class="hover:text-black <?php echo $path === 'features' ? 'text-black' : ''; ?>">Features</a>
                    <a href="/pricing" class="hover:text-black <?php echo $path === 'pricing' ? 'text-black' : ''; ?>">Pricing</a>
                    <a href="/about" class="hover:text-black <?php echo $path === 'about' ? 'text-black' : ''; ?>">About</a>
                </div>
            </div>
            <div class="flex items-center gap-6 text-sm font-bold">
                <a href="/login" class="text-slate-500 hover:text-black">Log in</a>
                <a href="/signup" class="bg-black text-white px-6 py-3 rounded-full hover:bg-slate-800 transition-colors">Start Trial</a>
            </div>
        </div>
    </nav>

    <main class="min-h-screen">
        <?php if ($path === ''): ?>
            <!-- Hero with subtle animation -->
            <section class="py-32 px-6 overflow-hidden">
                <div class="max-w-7xl mx-auto flex flex-col md:flex-row items-center gap-16">
                    <div class="flex-1 text-center md:text-left">
                        <span class="inline-block px-4 py-1 rounded-full bg-slate-100 text-slate-600 text-xs font-bold mb-6">Series B Announced &middot; New features live</span>
                        <h1 class="text-6xl md:text-8xl font-bold tracking-tight mb-8 leading-none">
                            Insight on <br/> <span class="italic text-slate-400 font-light">autopilot</span>.
                        </h1>
                        <p class="text-xl text-slate-500 max-w-xl mb-10 leading-relaxed">
                            Stop guessing and start knowing. Vantage centralizes your entire company's data into a single, beautiful dashboard.
                        </p>
                        <div class="flex gap-4 justify-center md:justify-start">
                            <button class="bg-black text-white px-8 py-4 rounded-full font-bold shadow-xl shadow-slate-200">Get Started</button>
                            <button class="bg-white border border-slate-200 px-8 py-4 rounded-full font-bold hover:bg-slate-50">View Demo</button>
                        </div>
                    </div>
                    <div class="flex-1 w-full max-w-xl">
                        <div class="aspect-video bg-white rounded-3xl border border-slate-200 shadow-2xl p-4 flex flex-col gap-4">
                            <div class="h-8 bg-slate-100 rounded-lg w-1/3"></div>
                            <div class="flex-grow grid grid-cols-3 gap-4">
                                <div class="bg-slate-50 rounded-xl"></div>
                                <div class="col-span-2 bg-slate-50 rounded-xl relative overflow-hidden">
                                    <div class="absolute inset-0 bg-gradient-to-tr from-slate-100 to-transparent"></div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </section>
        <?php elseif ($path === 'blog'): ?>
            <!-- Blog Listing - 30 articles -->
            <section class="py-24 px-6 max-w-7xl mx-auto">
                <div class="mb-16 text-center max-w-2xl mx-auto">
                    <h1 class="text-4xl font-bold mb-4 italic">The Vantage Journal</h1>
                    <p class="text-slate-500">Thoughts, updates, and engineering practices from the team behind Vantage.</p>
                </div>
                <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-x-8 gap-y-16">
                    <?php foreach ($articles as $art): ?>
                    <a href="/<?php echo $art['slug']; ?>" class="group">
                        <div class="aspect-[16/10] bg-slate-200 rounded-2xl mb-6 overflow-hidden relative">
                             <img src="https://picsum.photos/seed/<?php echo $art['id']; ?>/800/500" alt="Cover" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500">
                             <div class="absolute top-4 left-4">
                                <span class="bg-white/90 backdrop-blur px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-widest"><?php echo $art['category']; ?></span>
                             </div>
                        </div>
                        <div class="text-xs text-slate-400 font-bold mb-2"><?php echo strtoupper($art['date']); ?></div>
                        <h2 class="text-xl font-bold mb-3 group-hover:text-slate-500 transition-colors"><?php echo $art['title']; ?></h2>
                        <p class="text-slate-500 text-sm line-clamp-2 leading-relaxed">
                            <?php echo $art['excerpt']; ?>
                        </p>
                    </a>
                    <?php endforeach; ?>
                </div>
            </section>
        <?php elseif ($isBlogPost): ?>
            <!-- Blog Post Detail -->
            <article class="py-24 px-6 max-w-3xl mx-auto">
                <a href="/blog" class="text-sm font-bold text-slate-400 hover:text-black mb-12 inline-block">&larr; Back to blog</a>
                <div class="flex items-center gap-3 text-slate-400 text-sm font-bold mb-6">
                    <span><?php echo strtoupper($currentPage['category']); ?></span>
                    <span>&bull;</span>
                    <span><?php echo strtoupper($currentPage['date']); ?></span>
                </div>
                <h1 class="text-5xl font-bold mb-8 leading-tight italic"><?php echo $currentPage['title']; ?></h1>
                <div class="flex items-center gap-4 mb-16 pb-16 border-b border-slate-100">
                    <div class="w-12 h-12 bg-slate-200 rounded-full"></div>
                    <div>
                        <div class="font-bold">Team Vantage</div>
                        <div class="text-xs text-slate-400 uppercase font-bold">The Engineering Collective</div>
                    </div>
                </div>
                <div class="prose prose-slate prose-xl max-w-none">
                    <p class="mb-8"><?php echo $currentPage['content']; ?></p>
                    <p class="mb-8">Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam at porttitor sem.  Aliquam erat volutpat. Donec placerat nisl magna, et faucibus arcu condimentum sed.</p>
                    <h3 class="text-2xl font-bold mb-4">Key takeaways</h3>
                    <ul class="list-disc pl-6 mb-8 space-y-4 text-slate-600">
                        <li>Architecture simplicity over complexity.</li>
                        <li>Automated testing at every layer.</li>
                        <li>Observability is not optional.</li>
                    </ul>
                </div>
            </article>
        <?php else: ?>
            <section class="py-24 px-6 max-w-3xl mx-auto">
                <h1 class="text-5xl font-bold mb-12"><?php echo $currentPage['title']; ?></h1>
                <div class="text-lg text-slate-600 leading-relaxed space-y-8">
                    <p>This is one of the <strong>10 pages</strong> created for the Vantage startup demo. Each page is designed to test the limits of our content rendering engine.</p>
                    <p>We believe that speed is a feature. That's why we built our platform on top of a lightning-fast PHP core with modern Tailwind CSS utility styling. No bloat, just performance.</p>
                    <p>Join over 2,000+ companies who have switched to Vantage for their internal intelligence needs. Scale faster, stay smarter.</p>
                </div>
            </section>
        <?php endif; ?>
    </main>

    <footer class="bg-black text-white py-24 px-6 mt-32">
        <div class="max-w-7xl mx-auto grid grid-cols-1 md:grid-cols-4 gap-16">
            <div class="col-span-1 md:col-span-2">
                <div class="text-2xl font-bold mb-8">VANTAGE</div>
                <p class="text-slate-400 max-w-xs leading-relaxed">Better data, better decisions. The intelligent platform for the next generation of SaaS companies.</p>
            </div>
            <div>
                <h4 class="text-xs font-bold uppercase tracking-widest text-slate-500 mb-8">Platform</h4>
                <ul class="space-y-4 text-sm font-medium">
                    <li><a href="/features" class="hover:text-slate-400">Features</a></li>
                    <li><a href="/pricing" class="hover:text-slate-400">Pricing</a></li>
                    <li><a href="/about" class="hover:text-slate-400">About</a></li>
                </ul>
            </div>
             <div>
                <h4 class="text-xs font-bold uppercase tracking-widest text-slate-500 mb-8">Journal</h4>
                <ul class="space-y-4 text-sm font-medium">
                    <li><a href="/blog" class="hover:text-slate-400">Latest Posts</a></li>
                    <li><a href="/community" class="hover:text-slate-400">Community</a></li>
                    <li><a href="/resources" class="hover:text-slate-400">Help Center</a></li>
                </ul>
            </div>
        </div>
        <div class="max-w-7xl mx-auto mt-24 pt-8 border-t border-slate-800 flex justify-between items-center text-xs text-slate-500">
            <p>&copy; <?php echo date('Y'); ?> Vantage Intelligence Group. All rights reserved.</p>
            <div class="flex gap-8">
                <a href="#">Privacy</a>
                <a href="#">Terms</a>
            </div>
        </div>
    </footer>
</body>
</html>
