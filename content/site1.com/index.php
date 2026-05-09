<?php
$title = "Site One - Welcome";
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
        <h1 class="text-3xl font-bold mb-4">Welcome to Site 1</h1>
        <p class="text-zinc-400 leading-relaxed mb-6">
            This is a simple PHP template served from <code class="bg-zinc-800 px-1 rounded">/content/site1.com/index.php</code>.
        </p>
        <div class="flex gap-4">
            <span class="px-3 py-1 bg-blue-500/10 text-blue-400 text-xs font-medium rounded-full border border-blue-500/20">Active Site: site1.com</span>
            <?php if (getenv('ACTIVE_SITE_OVERRIDE')): ?>
                <span class="px-3 py-1 bg-amber-500/10 text-amber-400 text-xs font-medium rounded-full border border-amber-500/20">Forced via ENV</span>
            <?php endif; ?>
        </div>
    </div>
</body>
</html>
