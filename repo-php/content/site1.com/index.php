<?php
Layout::header("Site One - Welcome");
?>

<div class="space-y-12">
    <section class="grid grid-cols-1 md:grid-cols-2 gap-12 items-center">
        <div>
            <h2 class="text-4xl md:text-5xl serif italic mb-6 leading-tight">A new perspective on digital minimalism.</h2>
            <p class="text-lg opacity-60 leading-relaxed mb-8">
                Welcome to Site 1. This template has been refactored to use the global <code>Layout</code> engine, allowing for managed headers, footers, and SEO defaults across the multi-site network.
            </p>
            <div class="flex gap-4">
                <span class="px-3 py-1 bg-black/5 text-black/50 text-[10px] uppercase tracking-widest font-mono rounded">Status: Active</span>
                <span class="px-3 py-1 bg-black/5 text-black/50 text-[10px] uppercase tracking-widest font-mono rounded">Host: site1.com</span>
            </div>
        </div>
        <div class="aspect-square bg-gray-200 overflow-hidden rounded-sm">
            <img src="/img/site1-logo.png" alt="Site 1 Logo" class="w-full h-full object-cover grayscale hover:grayscale-0 transition-all duration-1000">
        </div>
    </section>

    <div class="h-px bg-black opacity-5 w-full"></div>

    <section class="max-w-2xl">
        <p class="text-sm opacity-50 mb-8 italic">
            This content is served directly from <code class="bg-[#1a1a1a]/5 px-1 rounded not-italic">/content/site1.com/index.php</code>.
        </p>
    </section>
</div>

<?php
Layout::footer();
?>

