<?php

class Layout {
    /**
     * Renders a basic header with optional custom CSS/Head content
     */
    public static function header($title, $extraHead = '') {
        $siteName = htmlspecialchars($title);
        ?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?php echo $siteName; ?></title>
    <script src="https://cdn.tailwindcss.com"></script>
    <style>
        @import url('https://fonts.googleapis.com/css2?family=Instrument+Serif:italic&family=Inter:wght@400;700&display=swap');
        body { font-family: 'Inter', sans-serif; background-color: #fcfaf7; color: #1a1a1a; }
        .serif { font-family: 'Instrument Serif', serif; }
        .h-px-fade { height: 1px; background: linear-gradient(to right, rgba(26,26,26,0.1), rgba(26,26,26,0.05), transparent); }
    </style>
    <?php echo $extraHead; ?>
</head>
<body>
    <div class="max-w-4xl mx-auto px-6 py-12 md:py-24">
        <header class="mb-16">
            <div class="flex justify-between items-baseline mb-4">
                <h1 class="text-4xl md:text-6xl serif italic tracking-tight"><?php echo $siteName; ?></h1>
                <nav class="hidden md:flex gap-6 text-[10px] uppercase tracking-widest opacity-40 font-mono">
                    <a href="/" class="hover:opacity-100 transition-opacity underline decoration-[1px] underline-offset-4">Home</a>
                    <a href="/about" class="hover:opacity-100 transition-opacity">About</a>
                    <a href="/work" class="hover:opacity-100 transition-opacity">Work</a>
                </nav>
            </div>
            <div class="h-px-fade w-full mb-4"></div>
            <div class="flex justify-between text-[10px] uppercase tracking-widest opacity-30 font-mono">
                <span>Edition 01</span>
                <span>Published <?php echo date('Y'); ?></span>
            </div>
        </header>
        <?php
    }

    /**
     * Renders the footer and closing body/html tags
     */
    public static function footer() {
        ?>
        <footer class="mt-32 pt-16 border-t border-black/5">
            <div class="grid grid-cols-1 md:grid-cols-3 gap-12 mb-16">
                <div>
                    <p class="text-[10px] uppercase tracking-widest opacity-30 font-mono mb-4">Contact</p>
                    <p class="text-sm italic font-serif">hello@project.com</p>
                </div>
                <div>
                    <p class="text-[10px] uppercase tracking-widest opacity-30 font-mono mb-4">Studio</p>
                    <p class="text-sm italic font-serif">Digital Multi-Site</p>
                </div>
                <div class="text-right">
                    <p class="text-[10px] uppercase tracking-widest opacity-30 font-mono mb-4">&copy; <?php echo date('Y'); ?></p>
                    <p class="text-[10px] uppercase tracking-widest opacity-30 font-mono">Powered by NodeCMS</p>
                </div>
            </div>
        </footer>
    </div>
</body>
</html>
        <?php
    }
}
