<?php
// sambazen.net landing page
$title = "SambaZen | Find Your Rhythm, Find Your Peace";
$description = "Experience the perfect balance of energetic Samba dance and mindful Zen practices. Join our retreats and classes.";
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
                            50: '#f0fdfa',
                            100: '#ccfbf1',
                            200: '#99f6e4',
                            300: '#5eead4',
                            400: '#2dd4bf',
                            500: '#14b8a6',
                            600: '#0d9488',
                            700: '#0f766e',
                            800: '#115e59',
                            900: '#134e4a',
                            950: '#042f2e',
                        },
                        accent: {
                            500: '#f59e0b',
                            600: '#d97706',
                        }
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
    </style>
</head>
<body class="bg-brand-50 text-brand-950 smooth-scroll antialiased selection:bg-brand-300 selection:text-brand-950">

    <!-- Navigation -->
    <nav class="fixed w-full z-50 bg-brand-950/90 backdrop-blur-md border-b border-brand-800 transition-all duration-300">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div class="flex justify-between items-center h-20">
                <div class="flex-shrink-0 flex items-center gap-2">
                    <span class="text-3xl font-display font-bold text-white tracking-tight">Samba<span class="text-brand-400">Zen</span></span>
                </div>
                <div class="hidden md:flex items-center space-x-8">
                    <a href="#about" class="text-brand-100 hover:text-white transition-colors">About</a>
                    <a href="#programs" class="text-brand-100 hover:text-white transition-colors">Programs</a>
                    <a href="#join" class="bg-brand-500 hover:bg-brand-400 text-white px-6 py-2.5 rounded-full font-medium transition-all shadow-lg shadow-brand-500/20 hover:shadow-brand-500/40">Join a Session</a>
                </div>
            </div>
        </div>
    </nav>

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
                <a href="#about" class="px-8 py-4 rounded-full bg-transparent border border-brand-300 text-brand-100 font-semibold text-lg hover:bg-brand-900/50 hover:text-white transition-all">Our Philosophy</a>
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
                    <ul class="space-y-4">
                        <li class="flex items-center text-brand-900 font-medium">
                            <svg class="w-6 h-6 text-brand-500 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                            Release physical tension through dance
                        </li>
                        <li class="flex items-center text-brand-900 font-medium">
                            <svg class="w-6 h-6 text-brand-500 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                            Cultivate mental clarity through meditation
                        </li>
                    </ul>
                </div>
                <div class="relative">
                    <img src="/img/meditation.jpg" alt="Meditation and Movement" class="rounded-2xl shadow-2xl object-cover h-[600px] w-full" loading="lazy">
                    <div class="absolute -bottom-8 -left-8 bg-brand-900 p-8 rounded-2xl shadow-xl max-w-xs hidden md:block">
                        <p class="text-brand-100 italic text-lg line-clamp-3">"SambaZen entirely changed how I relate to my body and my stress."</p>
                        <p class="text-brand-400 font-semibold mt-4">— Maria S.</p>
                    </div>
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
                <p class="text-lg text-gray-600">Choose the path that best aligns with your current rhythm and wellness goals.</p>
            </div>
            
            <div class="grid grid-cols-1 md:grid-cols-2 gap-8 max-w-5xl mx-auto border-t border-brand-200 pt-8">
                <!-- Card 1 -->
                <div class="bg-white rounded-3xl overflow-hidden shadow-sm hover:shadow-xl transition-shadow duration-300 border border-brand-100 group">
                    <div class="h-48 overflow-hidden">
                        <img src="/img/class.jpg" alt="Weekly Classes" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" loading="lazy">
                    </div>
                    <div class="p-8">
                        <div class="text-brand-500 font-medium text-sm mb-2">Ongoing</div>
                        <h4 class="text-2xl font-display font-bold text-brand-950 mb-3">Weekly Flow</h4>
                        <p class="text-gray-600 mb-6 line-clamp-3">Join our twice-weekly 90-minute sessions. 45 minutes of instructional samba cardio followed by 45 minutes of guided meditation.</p>
                        <a href="#join" class="text-brand-600 font-semibold flex items-center hover:text-brand-800 transition-colors">
                            View Schedule <svg class="w-4 h-4 ml-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"></path></svg>
                        </a>
                    </div>
                </div>
                
                <!-- Card 2 -->
                <div class="bg-brand-950 rounded-3xl overflow-hidden shadow-xl transform border border-brand-800 group relative">
                    <div class="absolute top-0 right-0 bg-accent-500 text-white text-xs font-bold px-3 py-1 rounded-bl-lg z-10">POPULAR</div>
                    <div class="h-48 overflow-hidden opacity-80">
                        <img src="/img/retreat.jpg" alt="Weekend Retreat" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" loading="lazy">
                    </div>
                    <div class="p-8">
                        <div class="text-brand-300 font-medium text-sm mb-2">Immersive</div>
                        <h4 class="text-2xl font-display font-bold text-white mb-3">Weekend Reset</h4>
                        <p class="text-brand-100 mb-6 line-clamp-3">A 2-day immersive retreat in nature. Includes healthy meals, intensive workshops, drum circles, and deep silence practices.</p>
                        <a href="#join" class="text-brand-300 font-semibold flex items-center hover:text-white transition-colors">
                            Learn More <svg class="w-4 h-4 ml-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"></path></svg>
                        </a>
                    </div>
                </div>
            </div>
        </div>
    </section>

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
