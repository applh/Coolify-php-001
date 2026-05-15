/**
 * CmsCard - Visual container for items
 */
export default {
    name: 'CmsCard',
    props: {
        title: String,
        subtitle: String,
        icon: String
    },
    template: `
        <div class="group border border-[#2A2A2A] p-6 bg-[#181818] hover:border-[#F27D26] transition-all relative overflow-hidden rounded-sm">
            <h3 v-if="title" class="text-xl font-serif italic mb-1 text-[#f4f4f4]">{{ title }}</h3>
            <p v-if="subtitle" class="text-[10px] font-mono opacity-40 uppercase tracking-tighter">{{ subtitle }}</p>
            
            <div class="mt-4 z-10 relative">
                <slot></slot>
            </div>
            
            <!-- Footer actions if any -->
            <div v-if="$slots.actions" class="flex flex-wrap items-center gap-2 mt-6 z-10 relative">
                <slot name="actions"></slot>
            </div>

            <!-- Background decorative icon -->
            <div v-if="icon" class="absolute right-[-20px] bottom-[-20px] opacity-[0.03] scale-[4] group-hover:opacity-[0.07] transition-all pointer-events-none text-white">
                <span class="lucide-icon" :data-lucide="icon"></span>
            </div>
        </div>
    `
};
