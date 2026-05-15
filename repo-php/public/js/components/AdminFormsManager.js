/**
 * AdminFormsManager - Component to list and manage forms for a site
 */
import CmsButton from './CmsButton.js';
import CmsCard from './CmsCard.js';

export default {
    name: 'AdminFormsManager',
    components: {
        CmsButton,
        CmsCard
    },
    props: {
        site: String,
        forms: {
            type: Array,
            default: () => []
        }
    },
    emits: ['create', 'edit', 'delete', 'view-submissions'],
    template: `
        <div>
            <div class="flex justify-between items-end mb-8">
                <div>
                    <h2 class="text-3xl font-serif italic mb-2">Forms for {{ site }}</h2>
                    <p class="text-[10px] font-mono opacity-40 uppercase tracking-tighter">Create and manage custom site forms</p>
                </div>
                <cms-button @click="$emit('create')">Create New Form</cms-button>
            </div>

            <div v-if="forms.length === 0" class="border border-dashed border-[#2A2A2A] py-20 text-center opacity-30 rounded-sm">
                <p class="italic">No forms created yet for this site.</p>
            </div>

            <div class="space-y-4">
                <cms-card 
                    v-for="form in forms" 
                    :key="form.id"
                    :title="form.title"
                    :subtitle="'ID: ' + form.id + ' | Fields: ' + form.fields.length"
                >
                    <template #actions>
                        <cms-button variant="outline" size="sm" @click="$emit('view-submissions', form)">Submissions</cms-button>
                        <cms-button variant="outline" size="sm" @click="$emit('edit', form)">Edit</cms-button>
                        <cms-button variant="danger" size="sm" @click="$emit('delete', form.id)">Delete</cms-button>
                    </template>
                </cms-card>
            </div>
        </div>
    `
};
