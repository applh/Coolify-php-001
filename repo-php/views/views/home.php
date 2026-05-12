<header class="mb-12">
    <h1 class="text-4xl font-bold tracking-tight mb-4"><?php echo $heading ?? 'Welcome'; ?></h1>
    <p class="text-lg text-slate-500">This page was built using the hierarchy template system.</p>
</header>

<section class="grid grid-cols-1 md:grid-cols-2 gap-8">
    <div class="bg-white p-6 rounded-xl border border-slate-200 shadow-sm">
        <h3 class="font-bold mb-2">Build Level</h3>
        <p class="text-sm text-slate-600">The hierarchy starts by rendering the innermost content (this view).</p>
    </div>
    <div class="bg-white p-6 rounded-xl border border-slate-200 shadow-sm">
        <h3 class="font-bold mb-2">Insert Level</h3>
        <p class="text-sm text-slate-600">The result is then 'inserted' as a slot into the parent level (the layout).</p>
    </div>
</section>
