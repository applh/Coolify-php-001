/**
 * CmsButton - Styled button component
 */
export default {
    name: 'CmsButton',
    props: {
        variant: {
            type: String,
            default: 'primary' // primary, secondary, outline, danger
        },
        size: {
            type: String,
            default: 'md' // sm, md, lg
        },
        disabled: Boolean,
        loading: Boolean,
        icon: String
    },
    setup(props) {
        const variants = {
            primary: 'bg-[#F27D26] text-black hover:bg-white',
            secondary: 'bg-[#2A2A2A] text-white hover:bg-[#F27D26] hover:text-black',
            outline: 'border border-[#2A2A2A] text-white hover:border-white',
            danger: 'border border-[#2A2A2A] text-red-500 hover:border-red-500 hover:bg-red-500/10'
        };

        const sizes = {
            sm: 'px-3 py-1.5 text-[10px]',
            md: 'px-4 py-2 text-xs',
            lg: 'px-6 py-4 text-xs'
        };

        return { variants, sizes };
    },
    template: `
        <button 
            :disabled="disabled || loading"
            class="font-bold uppercase tracking-widest transition-all rounded-sm flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
            :class="[variants[variant], sizes[size]]"
        >
            <span v-if="loading" class="animate-spin">
                <svg class="w-4 h-4" viewBox="00 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
            </span>
            <span v-else-if="icon" class="lucide-icon" :data-lucide="icon"></span>
            
            <slot></slot>
        </button>
    `
};
