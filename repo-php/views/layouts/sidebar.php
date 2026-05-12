<div class="flex min-h-screen">
    <aside class="w-64 bg-white border-r border-slate-200 p-6 hidden md:block">
        <h2 class="text-xs font-bold uppercase tracking-wider text-slate-400 mb-6">Navigation</h2>
        <nav class="space-y-4">
            <a href="/" class="block text-sm font-medium hover:text-indigo-600 transition-colors">Dashboard</a>
            <a href="/work" class="block text-sm font-medium hover:text-indigo-600 transition-colors">Projects</a>
            <a href="/settings" class="block text-sm font-medium hover:text-indigo-600 transition-colors">Settings</a>
        </nav>
    </aside>
    <main class="flex-1 p-8 md:p-12">
        <div class="max-w-4xl mx-auto">
            <?php echo $slot ?? ''; ?>
        </div>
    </main>
</div>
