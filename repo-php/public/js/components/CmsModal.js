/**
 * CmsModal - Reusable Modal Component
 */
export default {
    name: 'CmsModal',
    props: {
        show: {
            type: Boolean,
            default: false
        },
        title: {
            type: String,
            default: ''
        },
        size: {
            type: String,
            default: 'md' // sm, md, lg, xl
        }
    },
    emits: ['close'],
    setup(props, { emit }) {
        const close = () => emit('close');
        
        const sizeClasses = {
            sm: 'max-w-md',
            md: 'max-w-2xl',
            lg: 'max-w-4xl',
            xl: 'max-w-6xl'
        };

        return { close, sizeClasses };
    },
    template: `
        <div v-if="show" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm transition-opacity">
            <div 
                class="bg-[#181818] border border-[#2A2A2A] w-full shadow-2xl relative flex flex-col"
                :class="sizeClasses[size]"
                @click.stop
            >
                <!-- Header -->
                <div class="p-6 border-b border-[#2A2A2A] flex justify-between items-center">
                    <h3 class="text-xl font-serif italic text-white">{{ title }}</h3>
                    <button @click="close" class="opacity-40 hover:opacity-100 transition-opacity p-2">
                        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                    </button>
                </div>

                <!-- Body -->
                <div class="p-6 overflow-y-auto max-h-[80vh]">
                    <slot></slot>
                </div>

                <!-- Footer -->
                <div v-if="$slots.footer" class="p-6 border-t border-[#2A2A2A] flex justify-end gap-3">
                    <slot name="footer"></slot>
                </div>
            </div>
        </div>
    `
};
