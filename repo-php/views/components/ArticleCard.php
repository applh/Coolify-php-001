<article class="bg-white rounded-2xl shadow-sm border border-babi-100 overflow-hidden hover:shadow-md transition-shadow group flex flex-col">
    <div class="aspect-w-16 aspect-h-10 w-full overflow-hidden bg-gray-200">
        <img src="<?php echo htmlspecialchars($image); ?>" alt="<?php echo htmlspecialchars($title); ?>" class="w-full h-48 object-cover group-hover:scale-105 transition-transform duration-500" loading="lazy">
    </div>
    <div class="p-6 flex-grow flex flex-col">
        <div class="text-xs font-semibold tracking-wider text-babi-500 uppercase mb-2"><?php echo htmlspecialchars($category); ?></div>
        <h3 class="text-xl font-serif font-bold mb-3 text-babi-900 group-hover:text-babi-600 transition-colors">
            <?php echo htmlspecialchars($title); ?>
        </h3>
        <p class="text-babi-700/80 text-sm mb-4 line-clamp-3 mb-auto">
            <?php $slot(); ?>
        </p>
        <span class="text-xs text-babi-400 mt-4 font-medium"><?php echo htmlspecialchars($date); ?> &middot; <?php echo htmlspecialchars($readTime); ?> min de lecture</span>
    </div>
</article>
