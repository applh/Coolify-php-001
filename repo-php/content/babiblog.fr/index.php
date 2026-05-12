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
            <?php 
                // Initialize global state for the site
                Store::set('site_brand', 'BabiBlog');
                Store::set('show_read_time', true);

                $all_articles = [
                    ['image' => '/img/chambre-bebe.jpg', 'title' => 'Aménager une chambre douce et apaisante', 'category' => 'Décoration', 'date' => 'Il y a 2 jours', 'readTime' => 4, 'excerpt' => 'Découvrez nos conseils pour créer un véritable cocon pour votre enfant, avec des matières naturelles et des couleurs tendres.'],
                    ['image' => '/img/recette-saine.jpg', 'title' => 'Idées de repas simples pour la semaine', 'category' => 'Recettes', 'date' => 'Il y a 5 jours', 'readTime' => 6, 'excerpt' => "Comment s'organiser pour manger sainement sans passer des heures en cuisine ? Voici mon batch-cooking de la semaine."],
                    ['image' => '/img/organisation.jpg', 'title' => 'Trouver son équilibre pro/perso', 'category' => 'Lifestyle', 'date' => 'Il y a 1 semaine', 'readTime' => 5, 'excerpt' => "Entre le télétravail et la vie de famille, la frontière est parfois floue. Voici quelques pistes pour mieux cloisonner."],
                    ['image' => '/img/chambre-bebe.jpg', 'title' => 'Yoga matinal : 15 minutes pour réveiller son corps', 'category' => 'Bien-être', 'date' => 'Il y a 10 jours', 'readTime' => 3, 'excerpt' => 'Une routine simple et efficace pour commencer la journée avec énergie et sérénité.'],
                    ['image' => '/img/recette-saine.jpg', 'title' => 'Les bienfaits de la lecture avant de dormir', 'category' => 'Lifestyle', 'date' => 'Il y a 12 jours', 'readTime' => 5, 'excerpt' => "Pourquoi troquer son écran contre un livre peut transformer la qualité de votre sommeil."],
                    ['image' => '/img/organisation.jpg', 'title' => 'Organiser son bureau pour maximiser sa productivité', 'category' => 'Productivité', 'date' => 'Il y a 2 semaines', 'readTime' => 7, 'excerpt' => "Un espace de travail ordonné est la clé d'un esprit clair. Nos astuces pour un bureau minimaliste."],
                    ['image' => '/img/chambre-bebe.jpg', 'title' => "5 plantes d'intérieur faciles à entretenir", 'category' => 'Jardinage', 'date' => 'Il y a 15 jours', 'readTime' => 4, 'excerpt' => "Même si vous n'avez pas la main verte, ces plantes apporteront de la vie à votre intérieur."],
                    ['image' => '/img/recette-saine.jpg', 'title' => 'Ma routine de soin naturelle pour le visage', 'category' => 'Beauté', 'date' => 'Il y a 18 jours', 'readTime' => 6, 'excerpt' => "Des ingrédients simples de la cuisine pour une peau éclatante et en pleine santé."],
                    ['image' => '/img/organisation.jpg', 'title' => "Les secrets d'un thé parfait", 'category' => 'Cuisine', 'date' => 'Il y a 3 semaines', 'readTime' => 4, 'excerpt' => "Température de l'eau, temps d'infusion... tout ce qu'il faut savoir pour une dégustation optimale."],
                    ['image' => '/img/chambre-bebe.jpg', 'title' => 'Voyager léger : mes indispensables en sac à dos', 'category' => 'Voyage', 'date' => 'Il y a 1 mois', 'readTime' => 8, 'excerpt' => "Comment partir une semaine avec seulement un petit sac sans rien oublier d'essentiel."],
                    ['image' => '/img/recette-saine.jpg', 'title' => 'Comment débuter le potager sur son balcon', 'category' => 'Jardinage', 'date' => 'Il y a 1 mois', 'readTime' => 10, 'excerpt' => "Tomates cerises, herbes aromatiques : transformez votre petit extérieur en oasis gourmande."],
                    ['image' => '/img/organisation.jpg', 'title' => '3 recettes de smoothies énergisants', 'category' => 'Recettes', 'date' => 'Il y a 1 mois', 'readTime' => 3, 'excerpt' => "Faites le plein de vitamines dès le petit-déjeuner avec ces mélanges fruités."],
                    ['image' => '/img/chambre-bebe.jpg', 'title' => 'La magie du tri : désencombrer sa maison', 'category' => 'Maison', 'date' => 'Il y a 2 mois', 'readTime' => 12, 'excerpt' => "La méthode pas à pas pour se libérer du superflu et retrouver de l'espace vital."],
                    ['image' => '/img/recette-saine.jpg', 'title' => 'Apprendre à dire non sans culpabiliser', 'category' => 'Psychologie', 'date' => 'Il y a 2 mois', 'readTime' => 6, 'excerpt' => "Poser ses limites est essentiel pour préserver son énergie. Voici comment faire avec bienveillance."],
                    ['image' => '/img/organisation.jpg', 'title' => "Les meilleures applications pour s'organiser", 'category' => 'Productivité', 'date' => 'Il y a 2 mois', 'readTime' => 5, 'excerpt' => "De la gestion de tâches aux calendriers partagés, mes outils numériques préférés."],
                    ['image' => '/img/chambre-bebe.jpg', 'title' => 'Weekend à la campagne : déconnexion totale', 'category' => 'Voyage', 'date' => 'Il y a 3 mois', 'readTime' => 7, 'excerpt' => "Récit de mon escapade loin du tumulte urbain et mes adresses favorites."],
                    ['image' => '/img/recette-saine.jpg', 'title' => 'Créer son propre terrarium pas à pas', 'category' => 'Loisirs', 'date' => 'Il y a 3 mois', 'readTime' => 4, 'excerpt' => "Un petit monde végétal sous verre : un atelier DIY simple et gratifiant."],
                    ['image' => '/img/organisation.jpg', 'title' => 'Introduction à la méditation de pleine conscience', 'category' => 'Bien-être', 'date' => 'Il y a 3 mois', 'readTime' => 9, 'excerpt' => "Quelques minutes par jour suffisent pour réduire le stress et améliorer sa concentration."],
                    ['image' => '/img/chambre-bebe.jpg', 'title' => 'Pourquoi choisir des vêtements de seconde main ?', 'category' => 'Mode', 'date' => 'Il y a 4 mois', 'readTime' => 6, 'excerpt' => "Une alternative écologique et économique pour une garde-robe unique et responsable."],
                    ['image' => '/img/recette-saine.jpg', 'title' => 'Guide pour un pique-nique réussi en famille', 'category' => 'Famille', 'date' => 'Il y a 4 mois', 'readTime' => 5, 'excerpt' => "Recettes transportables et jeux en plein air pour des souvenirs inoubliables."],
                    ['image' => '/img/organisation.jpg', 'title' => 'Fabriquer ses propres bougies artisanales', 'category' => 'DIY', 'date' => 'Il y a 4 mois', 'readTime' => 8, 'excerpt' => "Cire de soja et huiles essentielles : la recette pour des bougies saines et parfumées."],
                    ['image' => '/img/chambre-bebe.jpg', 'title' => 'Les huiles essentielles indispensables à la maison', 'category' => 'Bien-être', 'date' => 'Il y a 5 mois', 'readTime' => 11, 'excerpt' => "Lavande, Ravintsara, Citron : le trio gagnant pour soigner les petits maux du quotidien."],
                    ['image' => '/img/recette-saine.jpg', 'title' => 'Recette de pain maison inratable', 'category' => 'Cuisine', 'date' => 'Il y a 5 mois', 'readTime' => 15, 'excerpt' => "Rien ne bat l'odeur du pain chaud qui sort du four. Une recette simple sans machine."],
                    ['image' => '/img/organisation.jpg', 'title' => 'Minimalisme : vivre mieux avec moins', 'category' => 'Lifestyle', 'date' => 'Il y a 5 mois', 'readTime' => 7, 'excerpt' => "Mon cheminement vers une consommation plus réfléchie et ce que cela a changé dans ma vie."],
                    ['image' => '/img/chambre-bebe.jpg', 'title' => 'Gérer son budget familial sereinement', 'category' => 'Famille', 'date' => 'Il y a 6 mois', 'readTime' => 10, 'excerpt' => "Mes méthodes pour épargner sans se priver et anticiper les dépenses importantes."],
                    ['image' => '/img/recette-saine.jpg', 'title' => 'Activités créatives pour les enfants le mercredi', 'category' => 'Famille', 'date' => 'Il y a 6 mois', 'readTime' => 4, 'excerpt' => "Peinture à doigts, collage, modelage : de quoi occuper les petits mains pendant des heures."],
                    ['image' => '/img/organisation.jpg', 'title' => 'Préparer sa valise de maternité sans stress', 'category' => 'Famille', 'date' => 'Il y a 6 mois', 'readTime' => 8, 'excerpt' => "La liste complète des essentiels pour maman et bébé pour partir l'esprit tranquille."],
                    ['image' => '/img/chambre-bebe.jpg', 'title' => 'Le pouvoir des rituels du soir', 'category' => 'Bien-être', 'date' => 'Il y a 7 mois', 'readTime' => 5, 'excerpt' => "Comment instaurer une routine apaisante pour faciliter l'endormissement des enfants."],
                    ['image' => '/img/recette-saine.jpg', 'title' => 'Découvrir la randonnée en basse montagne', 'category' => 'Sport', 'date' => 'Il y a 7 mois', 'readTime' => 9, 'excerpt' => "Conseils d'équipement et choix de parcours pour débuter en douceur et en sécurité."],
                    ['image' => '/img/organisation.jpg', 'title' => 'Astuces pour réduire ses déchets au quotidien', 'category' => 'Écologie', 'date' => 'Il y a 7 mois', 'readTime' => 12, 'excerpt' => "Passer au vrac, utiliser des bee-wraps : des petits gestes pour un grand impact."],
                    ['image' => '/img/chambre-bebe.jpg', 'title' => 'Mes livres coups de cœur du moment', 'category' => 'Culture', 'date' => 'Il y a 8 mois', 'readTime' => 6, 'excerpt' => "Une sélection de romans et d'essais qui m'ont marquée ces derniers mois."],
                    ['image' => '/img/recette-saine.jpg', 'title' => 'Aménager un coin lecture douillet', 'category' => 'Décoration', 'date' => 'Il y a 8 mois', 'readTime' => 4, 'excerpt' => "Fauteuil, lumière et plaids : créez votre refuge pour vos moments de lecture."],
                    ['image' => '/img/organisation.jpg', 'title' => 'Cuisiner les légumes de saison : les courges', 'category' => 'Recettes', 'date' => 'Il y a 8 mois', 'readTime' => 7, 'excerpt' => "Gratins, soupes et même gâteaux : découvrez comment sublimer les courges en automne."]
                ];

                // Pagination logic
                $items_per_page = 10;
                $total_articles = count($all_articles);
                $total_pages = ceil($total_articles / $items_per_page);
                
                // SEO-friendly Pagination: Parse URL for /page/X
                $request_uri = $_SERVER['REQUEST_URI'] ?? '/';
                $current_page = 1;
                if (preg_match('/\/page\/(\d+)/', $request_uri, $matches)) {
                    $current_page = (int)$matches[1];
                } elseif (isset($_GET['page'])) {
                    // Fallback to query param if needed, but we'll prefer paths
                    $current_page = (int)$_GET['page'];
                }
                
                $current_page = max(1, min($total_pages, $current_page));
                $offset = ($current_page - 1) * $items_per_page;
                $articles_to_show = array_slice($all_articles, $offset, $items_per_page);
            ?>
            <div class="flex items-baseline justify-between mb-10">
                <h2 class="text-3xl font-serif font-bold text-babi-900">Derniers articles</h2>
                <span class="text-sm font-medium text-babi-500 italic">Page <?php echo $current_page; ?> sur <?php echo $total_pages; ?></span>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-8">
                <?php 
                    foreach ($articles_to_show as $art) {
                        Component::render('ArticleCard', [
                            'image' => $art['image'],
                            'title' => $art['title'],
                            'category' => $art['category'],
                            'date' => $art['date'],
                            'readTime' => $art['readTime']
                        ], function() use ($art) {
                            echo htmlspecialchars($art['excerpt'], ENT_QUOTES, 'UTF-8');
                        });
                    }
                ?>
            </div>

            <!-- Pagination UI -->
            <?php if ($total_pages > 1): ?>
                <nav class="flex items-center justify-center space-x-2 mt-12">
                    <?php if ($current_page > 1): ?>
                        <a href="<?php echo ($current_page - 1 === 1) ? '/' : '/page/' . ($current_page - 1); ?>#articles" class="px-4 py-2 rounded-lg bg-white border border-babi-200 text-babi-600 hover:bg-babi-100 transition-colors">Précédent</a>
                    <?php endif; ?>
                    
                    <?php for ($i = 1; $i <= $total_pages; $i++): ?>
                        <a href="<?php echo ($i === 1) ? '/' : '/page/' . $i; ?>#articles" class="px-4 py-2 rounded-lg <?php echo $i === $current_page ? 'bg-babi-500 text-white shadow-sm' : 'bg-white border border-babi-200 text-babi-600 hover:bg-babi-100'; ?> transition-colors font-medium"><?php echo $i; ?></a>
                    <?php endfor; ?>

                    <?php if ($current_page < $total_pages): ?>
                        <a href="/page/<?php echo $current_page + 1; ?>#articles" class="px-4 py-2 rounded-lg bg-white border border-babi-200 text-babi-600 hover:bg-babi-100 transition-colors">Suivant</a>
                    <?php endif; ?>
                </nav>
            <?php endif; ?>
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
