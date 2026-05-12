<div class="border border-black/5 bg-white p-6 rounded-sm shadow-sm hover:shadow-md transition-shadow mb-6">
    <?php if (isset($title)): ?>
        <h3 class="text-xl serif italic mb-2"><?php echo htmlspecialchars($title); ?></h3>
    <?php endif; ?>

    <div class="text-sm opacity-80 leading-relaxed">
        <?php $slot(); ?>
    </div>

    <?php if (isset($footer)): ?>
        <div class="mt-4 pt-4 border-t border-black/5 text-[10px] uppercase tracking-widest opacity-40">
            <?php echo htmlspecialchars($footer); ?>
        </div>
    <?php endif; ?>
</div>
