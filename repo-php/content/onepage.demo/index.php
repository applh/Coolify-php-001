<?php
// onepage.demo main entry
$title = "Solo - Minimalist One Page Portfolio";
$description = "A clean, responsive one-page portfolio for creatives and professionals built with our PHP CMS.";
?>
<!DOCTYPE html>
<html lang="en" class="scroll-smooth">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?php echo htmlspecialchars($title, ENT_QUOTES, 'UTF-8'); ?></title>
    <meta name="description" content="<?php echo htmlspecialchars($description, ENT_QUOTES, 'UTF-8'); ?>">
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;600;800&display=swap" rel="stylesheet">
    <style>
        body { font-family: 'Plus Jakarta Sans', sans-serif; }
    </style>
</head>
<body class="bg-slate-50 text-slate-900 selection:bg-indigo-100 selection:text-indigo-600">
    <nav class="fixed top-0 w-full z-50 bg-white/70 backdrop-blur-xl border-b border-slate-200/50">
        <div class="max-w-7xl mx-auto px-6 h-20 flex items-center justify-between">
            <a href="#" class="text-xl font-extrabold tracking-tight hover:opacity-80 transition-opacity">SOLO.</a>
            <div class="hidden md:flex items-center gap-8 text-sm font-semibold text-slate-600">
                <a href="#work" class="hover:text-indigo-600 transition-colors">Work</a>
                <a href="#about" class="hover:text-indigo-600 transition-colors">About</a>
                <a href="#contact" class="px-5 py-2.5 bg-slate-900 text-white rounded-full hover:bg-slate-800 transition-all">Let's Talk</a>
            </div>
        </div>
    </nav>

    <main>
        <section class="relative pt-40 pb-20 px-6">
            <div class="max-w-7xl mx-auto">
                <span class="inline-block px-3 py-1 rounded-full bg-indigo-50 text-indigo-600 text-xs font-bold tracking-wider uppercase mb-6">Available for work</span>
                <h1 class="text-5xl md:text-8xl font-extrabold tracking-tight leading-[1.1] mb-8">
                    Crafting digital <br/>
                    <span class="text-indigo-600">experiences</span> that matter.
                </h1>
                <p class="text-xl text-slate-500 max-w-2xl leading-relaxed mb-12">
                    Independent designer & developer focusing on minimalist aesthetics and functional excellence for progressive brands.
                </p>
                <div class="flex gap-4">
                    <a href="#work" class="px-8 py-4 bg-indigo-600 text-white font-bold rounded-2xl hover:bg-indigo-700 hover:scale-[1.02] active:scale-[0.98] transition-all shadow-lg shadow-indigo-200">View Projects</a>
                    <a href="#about" class="px-8 py-4 bg-white border border-slate-200 font-bold rounded-2xl hover:bg-slate-50 transition-all">My Story</a>
                </div>
            </div>
        </section>

        <section id="work" class="py-24 px-6 bg-white">
            <div class="max-w-7xl mx-auto">
                <h2 class="text-3xl font-bold mb-12">Selected Projects</h2>
                <div class="grid grid-cols-1 md:grid-cols-2 gap-12">
                    <?php for($i=1; $i<=4; $i++): ?>
                    <div class="group cursor-pointer">
                        <div class="aspect-[4/3] bg-slate-100 rounded-3xl overflow-hidden mb-6 relative">
                            <div class="absolute inset-0 bg-indigo-600/0 group-hover:bg-indigo-600/5 transition-colors duration-500"></div>
                            <img src="/img/project-<?php echo $i; ?>.jpg" alt="Project <?php echo $i; ?>" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700" loading="lazy">
                        </div>
                        <div class="flex justify-between items-start">
                            <div>
                                <h3 class="text-xl font-bold mb-1">Project Name <?php echo $i; ?></h3>
                                <p class="text-slate-500">Visual Identity, Web Design</p>
                            </div>
                            <span class="p-2 border border-slate-200 rounded-full group-hover:bg-slate-900 group-hover:text-white transition-all">
                                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="7" y1="17" x2="17" y2="7"></line><polyline points="7 7 17 7 17 17"></polyline></svg>
                            </span>
                        </div>
                    </div>
                    <?php endfor; ?>
                </div>
            </div>
        </section>

        <section id="about" class="py-24 px-6">
            <div class="max-w-3xl mx-auto text-center">
                <h2 class="text-4xl font-bold mb-8">Less but better.</h2>
                <p class="text-xl text-slate-600 leading-relaxed">
                    I believe that great design is mostly about subtraction. Removing the noise to let the core message shine. With 10+ years of experience, I help companies simplify their digital presence.
                </p>
            </div>
        </section>
    </main>

    <footer class="py-12 px-6 border-t border-slate-200">
        <div class="max-w-7xl mx-auto flex flex-col md:flex-row justify-between items-center gap-8">
            <p class="text-slate-500 text-sm">&copy; <?php echo date('Y'); ?> Solo Studio. Built with PHP CMS.</p>
            <div class="flex gap-6 text-sm font-semibold">
                <a href="#" class="hover:text-indigo-600 transition-colors">Twitter</a>
                <a href="#" class="hover:text-indigo-600 transition-colors">Dribbble</a>
                <a href="#" class="hover:text-indigo-600 transition-colors">Instagram</a>
            </div>
        </div>
    </footer>
</body>
</html>
