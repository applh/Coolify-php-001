<?php
// babiblog.fr main entry
$title = "BabiBlog | Lifestyle, Famille & Quotidien";
$description = "Bienvenue sur BabiBlog. Découvrez nos articles, astuces et réflexions sur le quotidien, la parentalité et le lifestyle.";
?>
<!DOCTYPE html>
<html lang="fr" class="scroll-smooth">
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
                        babi: {
                            50: '#fdf8f6',
                            100: '#f2e8e5',
                            200: '#eaddd7',
                            300: '#e0aba0',
                            400: '#df9282',
                            500: '#d17e6c',
                            600: '#b86656',
                            700: '#995244',
                            800: '#7a4237',
                            900: '#5a332b',
                        }
                    },
                    fontFamily: {
                        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
                        serif: ['Lora', 'ui-serif', 'Georgia', 'serif'],
                    }
                }
            }
        }
    </script>
    
    <!-- Google Fonts -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600&family=Lora:ital,wght@0,400;0,500;0,600;1,400&display=swap" rel="stylesheet">
    
    <style>
        body { font-family: 'Inter', sans-serif; }
        h1, h2, h3, .font-serif { font-family: 'Lora', serif; }
    </style>
</head>
<body class="bg-babi-50 text-babi-900 antialiased flex flex-col min-h-screen">

    <!-- Header Navigation -->
    <header class="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b border-babi-200">
        <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
            <div class="flex justify-between items-center h-16">
                <!-- Logo -->
                <div class="flex-shrink-0 flex items-center">
                    <a href="/" class="text-2xl font-serif font-bold text-babi-600 tracking-tight">BabiBlog.</a>
                </div>
                <!-- Navigation -->
                <nav class="hidden md:flex space-x-8">
                    <a href="#" class="text-babi-800 hover:text-babi-500 font-medium transition-colors">Accueil</a>
                    <a href="#" class="text-babi-800 hover:text-babi-500 font-medium transition-colors">Lifestyle</a>
                    <a href="#" class="text-babi-800 hover:text-babi-500 font-medium transition-colors">Famille</a>
                    <a href="#" class="text-babi-800 hover:text-babi-500 font-medium transition-colors">À Propos</a>
                </nav>
                <!-- Mobile button (visual only) -->
                <div class="md:hidden flex items-center">
                    <button class="text-babi-800 hover:text-babi-500 focus:outline-none">
                        <svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
                        </svg>
                    </button>
                </div>
            </div>
        </div>
    </header>

    <!-- Main Content -->
    <main class="flex-grow">
        <!-- Hero Section -->
        <section class="py-16 md:py-24 bg-babi-100/50">
            <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
                <h1 class="text-4xl md:text-6xl font-serif font-bold text-babi-900 mb-6 leading-tight">
                    Inspirations douces pour<br/>
                    <span class="text-babi-500 italic">un quotidien serein</span>
                </h1>
                <p class="mt-4 max-w-2xl mx-auto text-xl text-babi-800/80 mb-10 font-light leading-relaxed">
                    Des astuces, des partages d'expériences et de jolies découvertes pour embellir la vie de tous les jours.
                </p>
                <a href="#articles" class="inline-flex items-center px-8 py-4 border border-transparent text-lg font-medium rounded-full shadow-sm text-white bg-babi-500 hover:bg-babi-600 transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-babi-500">
                    Découvrir les articles
                </a>
            </div>
        </section>

        <!-- Articles Grid -->
        <section id="articles" class="py-16 md:py-24 max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
            <div class="flex items-baseline justify-between mb-10">
                <h2 class="text-3xl font-serif font-bold text-babi-900">Derniers articles</h2>
                <a href="#" class="text-sm font-medium text-babi-500 hover:text-babi-700 transition-colors">Voir tout &rarr;</a>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-8">
                <!-- Article 1 -->
                <article class="bg-white rounded-2xl shadow-sm border border-babi-100 overflow-hidden hover:shadow-md transition-shadow group flex flex-col">
                    <div class="aspect-w-16 aspect-h-10 w-full overflow-hidden bg-gray-200">
                        <img src="https://images.unsplash.com/photo-1511895426328-dc8714191300?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80" alt="Chambre bébé" class="w-full h-48 object-cover group-hover:scale-105 transition-transform duration-500" loading="lazy">
                    </div>
                    <div class="p-6 flex-grow flex flex-col">
                        <div class="text-xs font-semibold tracking-wider text-babi-500 uppercase mb-2">Décoration</div>
                        <h3 class="text-xl font-serif font-bold mb-3 text-babi-900 group-hover:text-babi-600 transition-colors">Aménager une chambre douce et apaisante</h3>
                        <p class="text-babi-700/80 text-sm mb-4 line-clamp-3 mb-auto">
                            Découvrez nos conseils pour créer un véritable cocon pour votre enfant, avec des matières naturelles et des couleurs tendres.
                        </p>
                        <span class="text-xs text-babi-400 mt-4 font-medium">Il y a 2 jours &middot; 4 min de lecture</span>
                    </div>
                </article>

                <!-- Article 2 -->
                <article class="bg-white rounded-2xl shadow-sm border border-babi-100 overflow-hidden hover:shadow-md transition-shadow group flex flex-col">
                    <div class="aspect-w-16 aspect-h-10 w-full overflow-hidden bg-gray-200">
                        <img src="https://images.unsplash.com/photo-1490645935967-10de6ba17061?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80" alt="Recette saine" class="w-full h-48 object-cover group-hover:scale-105 transition-transform duration-500" loading="lazy">
                    </div>
                    <div class="p-6 flex-grow flex flex-col">
                        <div class="text-xs font-semibold tracking-wider text-babi-500 uppercase mb-2">Recettes</div>
                        <h3 class="text-xl font-serif font-bold mb-3 text-babi-900 group-hover:text-babi-600 transition-colors">Idées de repas simples pour la semaine</h3>
                        <p class="text-babi-700/80 text-sm mb-4 line-clamp-3 mb-auto">
                            Comment s'organiser pour manger sainement sans passer des heures en cuisine ? Voici mon batch-cooking de la semaine.
                        </p>
                        <span class="text-xs text-babi-400 mt-4 font-medium">Il y a 5 jours &middot; 6 min de lecture</span>
                    </div>
                </article>

                <!-- Article 3 -->
                <article class="bg-white rounded-2xl shadow-sm border border-babi-100 overflow-hidden hover:shadow-md transition-shadow group flex flex-col">
                    <div class="aspect-w-16 aspect-h-10 w-full overflow-hidden bg-gray-200">
                        <img src="https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80" alt="Organisation" class="w-full h-48 object-cover group-hover:scale-105 transition-transform duration-500" loading="lazy">
                    </div>
                    <div class="p-6 flex-grow flex flex-col">
                        <div class="text-xs font-semibold tracking-wider text-babi-500 uppercase mb-2">Lifestyle</div>
                        <h3 class="text-xl font-serif font-bold mb-3 text-babi-900 group-hover:text-babi-600 transition-colors">Trouver son équilibre pro/perso</h3>
                        <p class="text-babi-700/80 text-sm mb-4 line-clamp-3 mb-auto">
                            Entre le télétravail et la vie de famille, la frontière est parfois floue. Voici quelques pistes pour mieux cloisonner.
                        </p>
                        <span class="text-xs text-babi-400 mt-4 font-medium">Il y a 1 semaine &middot; 5 min de lecture</span>
                    </div>
                </article>
            </div>
        </section>
        
        <!-- Newsletter Minimal -->
        <section class="bg-white border-y border-babi-200 py-16">
            <div class="max-w-xl mx-auto px-4 text-center">
                <h3 class="text-2xl font-serif font-bold text-babi-900 mb-3">Rejoignez la newsletter</h3>
                <p class="text-babi-700/80 mb-6">Recevez un condensé de douceur chaque dimanche matin pour bien commencer la semaine.</p>
                <form class="flex flex-col sm:flex-row gap-2" onsubmit="event.preventDefault(); alert('Merci pour votre inscription !');">
                    <input type="email" placeholder="Votre adresse email" required class="flex-grow px-4 py-3 rounded-full border border-babi-200 focus:outline-none focus:ring-2 focus:ring-babi-400 focus:border-transparent text-babi-900">
                    <button type="submit" class="px-6 py-3 rounded-full bg-babi-900 text-white font-medium hover:bg-babi-800 transition-colors">S'inscrire</button>
                </form>
            </div>
        </section>
    </main>

    <!-- Footer -->
    <footer class="bg-babi-50 py-12 mt-auto">
        <div class="max-w-5xl mx-auto px-4 flex flex-col md:flex-row justify-between items-center text-sm text-babi-600">
            <div class="mb-4 md:mb-0">
                &copy; <?php echo date('Y'); ?> BabiBlog. Tous droits réservés.
            </div>
            <div class="flex space-x-6">
                <a href="#" class="hover:text-babi-900 transition-colors">Mentions légales</a>
                <a href="#" class="hover:text-babi-900 transition-colors">Contact</a>
                <a href="#" class="hover:text-babi-900 transition-colors">Instagram</a>
            </div>
        </div>
    </footer>

</body>
</html>
