<?php
/**
 * PHP Coolify Starter
 * A modern, minimal PHP boilerplate ready for Coolify deployment.
 */

// Simple routing or logic can go here
$projectName = "PHP Coolify Starter";
$version = "1.2.0";
$deploymentStatus = "Nginx + PHP 8.5 FPM Ready";

?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?php echo $projectName; ?></title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;600&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
    <style>
        body {
            font-family: 'Inter', sans-serif;
            background-color: #0a0a0a;
            color: #e0e0e0;
        }
        .mono { font-family: 'JetBrains Mono', monospace; }
        .grid-bg {
            background-image: radial-gradient(circle at 1px 1px, rgba(255,255,255,0.05) 1px, transparent 0);
            background-size: 40px 40px;
        }
    </style>
</head>
<body class="grid-bg min-h-screen flex flex-col items-center justify-center p-6">
    
    <div class="max-w-3xl w-full">
        <header class="mb-12 border-l-2 border-orange-500 pl-6 py-2">
            <h1 class="text-5xl font-light tracking-tight text-white mb-2">
                <?php echo $projectName; ?>
            </h1>
            <p class="text-gray-400 uppercase tracking-widest text-xs mono">
                v<?php echo $version; ?> • <?php echo $deploymentStatus; ?>
            </p>
        </header>

        <main class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <section class="bg-zinc-900/50 border border-zinc-800 p-8 rounded-2xl backdrop-blur-sm">
                <h2 class="text-sm font-semibold uppercase tracking-wider text-orange-500 mb-4 mono">Environment</h2>
                <ul class="space-y-4">
                    <li class="flex justify-between items-center border-b border-zinc-800 pb-2 leading-relaxed">
                        <span class="text-gray-500 text-sm italic">PHP Version</span>
                        <span class="mono text-white text-sm"><?php echo PHP_VERSION; ?></span>
                    </li>
                    <li class="flex justify-between items-center border-b border-zinc-800 pb-2 leading-relaxed">
                        <span class="text-gray-500 text-sm italic">Server IP</span>
                        <span class="mono text-white text-sm"><?php echo $_SERVER['SERVER_ADDR'] ?? '127.0.0.1'; ?></span>
                    </li>
                    <li class="flex justify-between items-center border-b border-zinc-800 pb-2 leading-relaxed">
                        <span class="text-gray-500 text-sm italic">Engine</span>
                        <span class="mono text-white text-sm">Nginx + FPM</span>
                    </li>
                </ul>
            </section>

            <section class="bg-zinc-900/50 border border-zinc-800 p-8 rounded-2xl backdrop-blur-sm">
                <h2 class="text-sm font-semibold uppercase tracking-wider text-orange-500 mb-4 mono">Architecture</h2>
                <p class="text-gray-400 text-sm mb-6 leading-relaxed">
                    Now using a modern <strong>/public</strong> document root with an <strong>Nginx + PHP-FPM</strong> stack for better performance and security.
                </p>
                <div class="flex items-center gap-4">
                    <span class="px-3 py-1 bg-zinc-800 rounded text-[10px] mono text-gray-400">NGINX_FPM</span>
                    <span class="px-3 py-1 bg-zinc-800 rounded text-[10px] mono text-gray-400">PUBLIC_ROOT</span>
                </div>
            </section>
        </main>

        <footer class="mt-12 text-center">
            <p class="text-zinc-600 text-[10px] uppercase tracking-[0.2em] mono">
                Rendered at <?php echo date('Y-m-d H:i:s'); ?> | Powered by AI Studio & Coolify
            </p>
        </footer>
    </div>

</body>
</html>
