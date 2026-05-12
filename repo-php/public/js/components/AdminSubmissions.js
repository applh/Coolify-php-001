/**
 * AdminSubmissions - Component to view form submissions
 */
import CmsButton from './CmsButton.js';
import CmsCard from './CmsCard.js';

export default {
    name: 'AdminSubmissions',
    components: {
        CmsButton,
        CmsCard
    },
    props: {
        form: {
            type: Object,
            required: true
        },
        submissions: {
            type: Array,
            default: () => []
        }
    },
    emits: ['export'],
    template: `
        <div>
            <div class="flex justify-between items-end mb-8">
                <div>
                    <h2 class="text-3xl font-serif italic mb-2">Submissions: {{ form.title }}</h2>
                    <p class="text-[10px] font-mono opacity-40 uppercase tracking-tighter">Recent activity</p>
                </div>
                <cms-button variant="outline" size="sm" @click="$emit('export')">Export JSON</cms-button>
            </div>

            <div v-if="submissions.length === 0" class="border border-dashed border-[#2A2A2A] py-20 text-center opacity-30 rounded-sm">
                <p class="italic">No submissions found for this form yet.</p>
            </div>

            <div class="space-y-6">
                <div v-for="sub in submissions" :key="sub.id" class="border border-[#2A2A2A] bg-[#181818] p-6 rounded-sm shadow-sm">
                    <div class="flex justify-between items-center mb-6 border-b border-[#2A2A2A] pb-4">
                        <span class="text-[10px] font-mono opacity-40 uppercase tracking-widest">{{ sub.submitted_at }}</span>
                        <span class="text-[10px] font-mono opacity-20 uppercase tracking-widest">ID: {{ sub.id }}</span>
                    </div>
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-4">
                        <div v-for="(val, key) in sub.data" :key="key">
                            <label class="block text-[9px] font-mono uppercase opacity-30 mb-1">{{ key }}</label>
                            <div class="text-sm border-l border-[#F27D26]/30 pl-3 leading-relaxed text-[#f4f4f4]">{{ val }}</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `
};
