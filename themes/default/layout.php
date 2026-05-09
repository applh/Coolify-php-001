<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?php echo htmlspecialchars($title ?? 'CMS'); ?></title>
    <script src="https://cdn.tailwindcss.com"></script>
    <style>
        body { background: #09090b; color: #e4e4e7; }
        .prose h1 { font-size: 2.25rem; font-weight: 700; color: white; margin-bottom: 1.5rem; }
        .prose h2 { font-size: 1.5rem; font-weight: 600; color: #f4f4f5; margin-top: 2rem; margin-bottom: 1rem; }
        .prose p { margin-bottom: 1.25rem; line-height: 1.75; }
        .prose ul { list-style-type: disc; padding-left: 1.5rem; margin-bottom: 1.25rem; }
        .prose strong { color: white; }
    </style>
</head>
<body class="min-h-screen flex flex-col">
    <nav class="border-b border-zinc-800 bg-zinc-950/50 backdrop-blur-md sticky top-0 z-50">
        <div class="max-w-4xl mx-auto px-6 py-4 flex justify-between items-center">
            <a href="/" class="text-white font-bold text-lg tracking-tight">
                <?php echo htmlspecialchars($siteName); ?>
            </a>
            <div class="flex gap-6 text-sm font-medium text-zinc-400">
                <a href="/" class="hover:text-white transition-colors">Home</a>
                <a href="/about" class="hover:text-white transition-colors">About</a>
            </div>
        </div>
    </nav>

    <main class="flex-grow max-w-4xl mx-auto px-6 py-12 w-full">
        <article class="prose prose-invert max-w-none">
            <?php echo $content; ?>
        </article>
    </main>

    <footer class="border-t border-zinc-800 py-12 bg-zinc-950">
        <div class="max-w-4xl mx-auto px-6 text-center">
            <p class="text-zinc-500 text-xs mb-2 uppercase tracking-widest">
                <?php echo htmlspecialchars($siteDescription); ?>
            </p>
            <p class="text-zinc-700 text-[10px]">
                Powered by Flat-File CMS | Hosted on Coolify
            </p>
        </div>
    </footer>
</body>
</html>
