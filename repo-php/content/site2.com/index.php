<?php
$title = "Site Two - Welcome";
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title><?php echo $title; ?></title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-zinc-950 text-zinc-100 min-h-screen flex items-center justify-center">
    <div class="max-w-xl w-full p-8 border border-zinc-800 rounded-2xl bg-zinc-900/50">
        <h1 class="text-3xl font-bold mb-4 text-emerald-400">Welcome to Site 2</h1>
        <p class="text-zinc-400 leading-relaxed mb-6">
            This instance is served from <code class="bg-zinc-800 px-1 rounded">/content/site2.com/index.php</code>.
        </p>
        <div class="flex gap-4">
            <span class="px-3 py-1 bg-emerald-500/10 text-emerald-400 text-xs font-medium rounded-full border border-emerald-500/20">Active Site: site2.com</span>
            <?php if (getenv('ACTIVE_SITE_OVERRIDE')): ?>
                <span class="px-3 py-1 bg-amber-500/10 text-amber-400 text-xs font-medium rounded-full border border-amber-500/20">Forced via ENV</span>
            <?php endif; ?>
        </div>
    </div>
</body>
</html>
