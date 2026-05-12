<?php
// sambazen.net index.php
require_once 'countries-data.php';

// Helper function for slugs
if (!function_exists('slugify')) {
    function slugify($text) {
        $text = preg_replace('~[^\pL\d]+~u', '-', $text);
        $text = iconv('utf-8', 'us-ascii//TRANSLIT', $text);
        $text = preg_replace('~[^-\w]+~', '', $text);
        $text = trim($text, '-');
        $text = preg_replace('~-+~', '-', $text);
        $text = strtolower($text);
        if (empty($text)) return 'n-a';
        return $text;
    }
}

// Routing logic
$requestUri = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH);
// Clean up path if it includes the content folder (for dev environments)
$path = $requestUri;
$sitePrefix = '/repo-php/content/sambazen.net';
if (strpos($path, $sitePrefix) === 0) {
    $path = substr($path, strlen($sitePrefix));
}

$view = 'home';
$countryData = null;

if ($path === '/world-countries' || $path === '/world-countries/') {
    $view = 'countries_list';
    $title = "World Population Explorer | SambaZen Insights";
    $description = "Exhaustive list of world countries by population. Discover global demographics through the Zen lens of balance.";
} else if (preg_match('#^/world-countries/([^/]+)/?$#', $path, $matches)) {
    $slug = $matches[1];
    foreach ($countries as $c) {
        if (slugify($c['name']) === $slug) {
            $countryData = $c;
            $view = 'country_detail';
            $title = "{$c['name']} Population and Facts | SambaZen World";
            $description = "Detailed population data and facts about {$c['name']}. Explore global rhythms and mindful statistics.";
            break;
        }
    }
    if (!$countryData) {
        $view = 'home'; 
    }
} else {
    $view = 'home';
    $title = "SambaZen | Find Your Rhythm, Find Your Peace";
    $description = "Experience the perfect balance of energetic Samba dance and mindful Zen practices. Join our retreats and classes.";
}

?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?php echo htmlspecialchars($title, ENT_QUOTES, 'UTF-8'); ?></title>
    <meta name="description" content="<?php echo htmlspecialchars($description, ENT_QUOTES, 'UTF-8'); ?>">
    
    <!-- Tailwind CSS -->
    <script src="https://cdn.tailwindcss.com"></script>
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    colors: {
                        brand: {
                            50: '#f0fdfa', 100: '#ccfbf1', 200: '#99f6e4', 300: '#5eead4', 400: '#2dd4bf',
                            500: '#14b8a6', 600: '#0d9488', 700: '#0f766e', 800: '#115e59', 900: '#134e4a', 950: '#042f2e',
                        },
                        accent: { 500: '#f59e0b', 600: '#d97706', }
                    },
                    fontFamily: {
                        sans: ['Inter', 'sans-serif'],
                        display: ['Playfair Display', 'serif'],
                    }
                }
            }
        }
    </script>
    
    <!-- Google Fonts -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600&family=Playfair+Display:ital,wght@0,400;0,600;0,700;1,400&display=swap" rel="stylesheet">
    
    <style>
        body { font-family: 'Inter', sans-serif; }
        h1, h2, h3, .font-display { font-family: 'Playfair Display', serif; }
        .hero-bg {
            background-image: linear-gradient(rgba(4, 47, 46, 0.8), rgba(4, 47, 46, 0.9)), url('/img/hero.jpg');
            background-size: cover;
            background-position: center;
        }
        .smooth-scroll { scroll-behavior: smooth; }
        .country-card:hover .country-name { color: #14b8a6; }
    </style>
</head>
<body class="bg-brand-50 text-brand-950 smooth-scroll antialiased selection:bg-brand-300 selection:text-brand-950">

    <!-- Navigation -->
    <nav class="fixed w-full z-50 bg-brand-950/90 backdrop-blur-md border-b border-brand-800 transition-all duration-300">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div class="flex justify-between items-center h-20">
                <div class="flex-shrink-0 flex items-center gap-2">
                    <a href="/" class="text-3xl font-display font-bold text-white tracking-tight">Samba<span class="text-brand-400">Zen</span></a>
                </div>
                <div class="hidden md:flex items-center space-x-8">
                    <a href="/#about" class="text-brand-100 hover:text-white transition-colors">About</a>
                    <a href="/#programs" class="text-brand-100 hover:text-white transition-colors">Programs</a>
                    <a href="/world-countries" class="text-brand-100 hover:text-white transition-colors">World Countries</a>
                    <a href="/#join" class="bg-brand-500 hover:bg-brand-400 text-white px-6 py-2.5 rounded-full font-medium transition-all shadow-lg shadow-brand-500/20 hover:shadow-brand-500/40">Join a Session</a>
                </div>
            </div>
        </div>
    </nav>

    <?php if ($view === 'home'): ?>
        <!-- Hero Section -->
        <section class="relative pt-32 pb-20 lg:pt-48 lg:pb-32 hero-bg min-h-screen flex items-center">
            <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10 text-center">
                <h1 class="text-5xl md:text-7xl font-bold text-white mb-6 leading-tight">
                    Find Your Rhythm. <br/>
                    <span class="italic font-light text-brand-200">Find Your Peace.</span>
                </h1>
                <p class="mt-4 max-w-2xl mx-auto text-xl text-brand-100 mb-10 leading-relaxed font-light">
                    Discover the transformative power of blending energetic Brazilian Samba with mindful Zen practices. A unique journey for body, mind, and spirit.
                </p>
                <div class="flex flex-col sm:flex-row justify-center gap-4">
                    <a href="#programs" class="px-8 py-4 rounded-full bg-brand-500 text-white font-semibold text-lg hover:bg-brand-400 transition-all shadow-[0_0_20px_rgba(20,184,166,0.3)] hover:shadow-[0_0_30px_rgba(20,184,166,0.5)] transform hover:-translate-y-1">Explore Programs</a>
                    <a href="/world-countries" class="px-8 py-4 rounded-full bg-transparent border border-brand-300 text-brand-100 font-semibold text-lg hover:bg-brand-900/50 hover:text-white transition-all">World Insights</a>
                </div>
            </div>
        </section>

        <!-- About Section -->
        <section id="about" class="py-24 bg-white">
            <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div class="grid grid-cols-1 lg:grid-cols-2 gap-16 items-center">
                    <div>
                        <h2 class="text-sm font-bold tracking-widest text-brand-600 uppercase mb-3">The Synergy</h2>
                        <h3 class="text-4xl md:text-5xl font-bold mb-6 text-brand-950 leading-tight">Two Worlds, <span class="italic text-brand-500">One Harmony</span></h3>
                        <p class="text-lg text-gray-600 mb-6 leading-relaxed">
                            Samba brings fire, passion, and cardiovascular vitality. Zen brings focus, stillness, and emotional clarity. Together, they create a holistic wellness practice unlike any other.
                        </p>
                        <p class="text-lg text-gray-600 mb-8 leading-relaxed">
                            Our expert guides help you transition seamlessly from high-energy rhythmic expression to profound meditative calm, leaving you energized yet deeply centered.
                        </p>
                    </div>
                    <div class="relative">
                        <img src="/img/meditation.jpg" alt="Meditation and Movement" class="rounded-2xl shadow-2xl object-cover h-[600px] w-full" loading="lazy">
                    </div>
                </div>
            </div>
        </section>

        <!-- Programs Section -->
        <section id="programs" class="py-24 bg-brand-50">
            <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div class="text-center max-w-3xl mx-auto mb-16">
                    <h2 class="text-sm font-bold tracking-widest text-brand-600 uppercase mb-3">Our Offerings</h2>
                    <h3 class="text-4xl md:text-5xl font-bold mb-6 text-brand-950">Curated Experiences</h3>
                </div>
                <div class="grid grid-cols-1 md:grid-cols-2 gap-8 max-w-5xl mx-auto border-t border-brand-200 pt-8">
                    <div class="bg-white rounded-3xl overflow-hidden shadow-sm hover:shadow-xl transition-shadow duration-300 border border-brand-100 group">
                        <div class="h-48 overflow-hidden"><img src="/img/class.jpg" alt="Weekly Classes" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" loading="lazy"></div>
                        <div class="p-8">
                            <h4 class="text-2xl font-display font-bold text-brand-950 mb-3">Weekly Flow</h4>
                            <p class="text-gray-600 mb-6">Twice-weekly 90-minute sessions blending samba and meditation.</p>
                            <a href="#join" class="text-brand-600 font-semibold flex items-center">View Schedule</a>
                        </div>
                    </div>
                    <div class="bg-brand-950 rounded-3xl overflow-hidden shadow-xl border border-brand-800 group relative">
                        <div class="h-48 overflow-hidden opacity-80"><img src="/img/retreat.jpg" alt="Weekend Retreat" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" loading="lazy"></div>
                        <div class="p-8 text-white">
                            <h4 class="text-2xl font-display font-bold text-white mb-3">Weekend Reset</h4>
                            <p class="text-brand-100 mb-6">A 2-day immersive retreat in nature.</p>
                            <a href="#join" class="text-brand-300 font-semibold flex items-center">Learn More</a>
                        </div>
                    </div>
                </div>
            </div>
        </section>

    <?php elseif ($view === 'countries_list'): ?>
        <section class="pt-32 pb-24 min-h-screen">
            <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div class="text-center mb-16">
                    <h1 class="text-4xl md:text-6xl font-display font-bold text-brand-950 mb-6">World Countries</h1>
                    <p class="text-xl text-gray-600 max-w-2xl mx-auto">Exploring the balance of global populations through a Zen perspective. Every number tells a story of rhythm and growth.</p>
                </div>

                <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                    <?php foreach ($countries as $index => $c): ?>
                        <a href="/world-countries/<?php echo slugify($c['name']); ?>" class="bg-white p-6 rounded-2xl shadow-sm border border-brand-100 hover:shadow-md hover:border-brand-300 transition-all group country-card <?php echo $index < 20 ? 'ring-2 ring-brand-100' : ''; ?>">
                            <div class="flex justify-between items-start mb-4">
                                <span class="text-xs font-bold text-brand-400 tracking-widest uppercase">Rank #<?php echo $index + 1; ?></span>
                                <?php if ($index < 20): ?>
                                    <span class="bg-brand-100 text-brand-700 text-[10px] font-bold px-2 py-0.5 rounded">DETAILED</span>
                                <?php endif; ?>
                            </div>
                            <h2 class="text-xl font-display font-bold text-brand-950 mb-2 country-name transition-colors"><?php echo htmlspecialchars($c['name']); ?></h2>
                            <p class="text-2xl font-semibold text-brand-700 mb-1"><?php echo number_format($c['population']); ?></p>
                            <p class="text-sm text-gray-500">Total Population</p>
                            <div class="mt-4 pt-4 border-t border-brand-50 flex items-center text-brand-600 font-medium text-sm gap-2">
                                <span><?php echo htmlspecialchars($c['region']); ?></span>
                                <span class="w-1 h-1 bg-brand-200 rounded-full"></span>
                                <span><?php echo htmlspecialchars($c['capital']); ?></span>
                            </div>
                        </a>
                    <?php endforeach; ?>
                </div>
            </div>
        </section>

    <?php elseif ($view === 'country_detail'): ?>
        <section class="pt-32 pb-24 min-h-screen">
            <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
                <nav class="mb-12">
                    <a href="/world-countries" class="inline-flex items-center text-brand-600 hover:text-brand-800 font-medium transition-colors">
                        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"></path></svg>
                        Back to Countries
                    </a>
                </nav>

                <div class="bg-white rounded-3xl p-8 md:p-12 shadow-xl border border-brand-100 relative overflow-hidden">
                    <div class="absolute top-0 right-0 p-8 opacity-10">
                         <svg class="w-32 h-32 text-brand-900" fill="currentColor" viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"/></svg>
                    </div>

                    <div class="relative z-10">
                        <h1 class="text-4xl md:text-6xl font-display font-bold text-brand-950 mb-4"><?php echo htmlspecialchars($countryData['name']); ?></h1>
                        <div class="flex flex-wrap gap-4 mb-8">
                            <span class="bg-brand-50 text-brand-700 px-4 py-1.5 rounded-full text-sm font-medium border border-brand-100"><?php echo htmlspecialchars($countryData['region']); ?></span>
                            <span class="bg-brand-50 text-brand-700 px-4 py-1.5 rounded-full text-sm font-medium border border-brand-100">Capital: <?php echo htmlspecialchars($countryData['capital']); ?></span>
                        </div>

                        <div class="grid grid-cols-1 md:grid-cols-2 gap-12 items-center">
                            <div>
                                <h2 class="text-sm font-bold tracking-widest text-brand-400 uppercase mb-2">Demographic Balance</h2>
                                <p class="text-5xl md:text-6xl font-bold text-brand-600 mb-6"><?php echo number_format($countryData['population']); ?></p>
                                <p class="text-gray-600 leading-relaxed mb-6">
                                    In the vast rhythm of our planet, <?php echo htmlspecialchars($countryData['name']); ?> represents a significant pulse of life and energy. With a population of <?php echo number_format($countryData['population']); ?>, it contributes uniquely to the global tapestry.
                                </p>
                                <p class="text-gray-600 leading-relaxed">
                                    Understanding the scale of this nation through a Zen lens allows us to appreciate the collective focus and individual stillness required to maintain harmony within such a dynamic environment.
                                </p>
                            </div>
                            <div class="space-y-6">
                                <div class="bg-brand-50 p-6 rounded-2xl border border-brand-100">
                                    <h3 class="text-brand-900 font-bold mb-2">Did you know?</h3>
                                    <p class="text-sm text-gray-600">The population density and distribution of <?php echo htmlspecialchars($countryData['name']); ?> creates a unique social rhythm that residents navigate daily.</p>
                                </div>
                                <div class="bg-brand-900 text-white p-6 rounded-2xl">
                                    <h3 class="font-bold mb-2 text-brand-400">Zen Practice Focus</h3>
                                    <p class="text-sm text-brand-100 italic">"In a world of billions, finding one's internal rhythm is the ultimate achievement."</p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                
                <div class="mt-12 text-center">
                    <a href="/#join" class="inline-block px-10 py-4 bg-brand-500 text-white rounded-full font-bold shadow-lg hover:bg-brand-400 transition-all">Join a Session for Global Peace</a>
                </div>
            </div>
        </section>
    <?php endif; ?>

    <!-- Footer / CTA -->
    <footer id="join" class="bg-brand-950 text-brand-100 py-16 border-t border-brand-900">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
            <h2 class="text-3xl md:text-5xl font-display font-bold text-white mb-6">Ready to find your rhythm?</h2>
            <p class="text-xl text-brand-200 mb-10 max-w-2xl mx-auto font-light">Join our newsletter to get schedule updates, wellness tips, and a free guided moving meditation.</p>
            
            <form class="max-w-md mx-auto relative flex items-center" onsubmit="event.preventDefault(); alert('Thanks for subscribing!');">
                <input type="email" placeholder="Enter your email" required class="w-full bg-brand-900/50 border border-brand-700 text-white px-6 py-4 rounded-full focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-transparent placeholder-brand-400">
                <button type="submit" class="absolute right-2 bg-brand-500 hover:bg-brand-400 text-white px-6 py-2 rounded-full font-medium transition-colors">Subscribe</button>
            </form>
            
            <div class="mt-16 pt-8 border-t border-brand-800/50 flex flex-col md:flex-row justify-between items-center text-sm text-brand-400">
                <p>&copy; <?php echo date('Y'); ?> SambaZen Retreats. All rights reserved.</p>
                <div class="flex space-x-6 mt-4 md:mt-0">
                    <a href="#" class="hover:text-white transition-colors">Privacy</a>
                    <a href="#" class="hover:text-white transition-colors">Terms</a>
                    <a href="#" class="hover:text-white transition-colors">Contact</a>
                </div>
            </div>
        </div>
    </footer>

</body>
</html>
