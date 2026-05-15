/**
 * AdminFormEditor - Component to create or edit a form
 */
import CmsButton from './CmsButton.js';

export default {
    name: 'AdminFormEditor',
    components: {
        CmsButton
    },
    props: {
        form: {
            type: Object,
            required: true
        }
    },
    emits: ['save', 'cancel'],
    setup(props, { emit }) {
        const { reactive } = Vue;
        
        // Local deep copy to avoid direct prop mutation
        const localForm = reactive(JSON.parse(JSON.stringify(props.form)));

        const addField = () => {
            localForm.fields.push({ label: 'New Field', name: 'field_' + Date.now(), type: 'text', required: false, options_string: '' });
        };

        const removeField = (index) => {
            localForm.fields.splice(index, 1);
        };

        const updateOptions = (field) => {
            if (field.options_string) {
                field.options = field.options_string.split(',').map(s => s.trim()).filter(s => s !== '');
            }
        };

        const onSave = () => {
            emit('save', localForm);
        };

        return { localForm, addField, removeField, updateOptions, onSave };
    },
    template: `
        <div class="max-w-2xl mx-auto border border-[#2A2A2A] bg-[#181818] p-8 rounded-sm shadow-xl">
            <h2 class="text-2xl font-serif italic mb-8">{{ localForm.id ? 'Edit Form' : 'New Form' }}</h2>
            
            <div class="space-y-6">
                <div>
                    <label class="block text-xs font-mono uppercase tracking-widest opacity-50 mb-2">Form Title</label>
                    <input type="text" v-model="localForm.title" class="w-full bg-[#0e0e0e] border border-[#2A2A2A] p-3 text-white focus:outline-none focus:border-[#F27D26]" placeholder="e.g. Contact Us">
                </div>
                <div>
                    <label class="block text-xs font-mono uppercase tracking-widest opacity-50 mb-2">Form Slug / HTML ID</label>
                    <input type="text" v-model="localForm.slug" class="w-full bg-[#0e0e0e] border border-[#2A2A2A] p-3 text-white focus:outline-none focus:border-[#F27D26]" placeholder="contact-form">
                </div>
                <div>
                    <label class="block text-xs font-mono uppercase tracking-widest opacity-50 mb-2">Submit Button Label</label>
                    <input type="text" v-model="localForm.submit_label" class="w-full bg-[#0e0e0e] border border-[#2A2A2A] p-3 text-white focus:outline-none focus:border-[#F27D26]" placeholder="Send Message">
                </div>

                <div class="pt-8 border-t border-[#2A2A2A]">
                    <div class="flex justify-between items-center mb-4">
                        <h4 class="text-sm font-mono uppercase tracking-widest flex items-center gap-2">
                            <span class="lucide-icon" data-lucide="list"></span> Fields
                        </h4>
                        <button @click="addField" class="text-[10px] uppercase font-bold tracking-widest text-[#F27D26] hover:text-white transition-all">+ Add Field</button>
                    </div>

                    <div class="space-y-4">
                        <div v-for="(field, index) in localForm.fields" :key="index" class="p-4 bg-[#222] border border-[#333] relative rounded-sm">
                            <button @click="removeField(index)" class="absolute top-2 right-2 text-red-500 opacity-50 hover:opacity-100 italic font-serif text-sm">Remove</button>
                            <div class="grid grid-cols-2 gap-4">
                                <div>
                                    <label class="block text-[9px] font-mono uppercase opacity-40 mb-1">Label</label>
                                    <input type="text" v-model="field.label" class="w-full bg-[#0e0e0e] border border-[#333] p-2 text-xs focus:outline-none focus:border-[#F27D26]" placeholder="Full Name">
                                </div>
                                <div>
                                    <label class="block text-[9px] font-mono uppercase opacity-40 mb-1">Field Name (Technical)</label>
                                    <input type="text" v-model="field.name" class="w-full bg-[#0e0e0e] border border-[#333] p-2 text-xs focus:outline-none focus:border-[#F27D26]" placeholder="name">
                                </div>
                                <div>
                                    <label class="block text-[9px] font-mono uppercase opacity-40 mb-1">Type</label>
                                    <select v-model="field.type" class="w-full bg-[#0e0e0e] border border-[#333] p-2 text-xs focus:outline-none focus:border-[#F27D26]">
                                        <option value="text">Text</option>
                                        <option value="email">Email</option>
                                        <option value="textarea">Textarea</option>
                                        <option value="select">Select</option>
                                        <option value="tel">Phone</option>
                                        <option value="date">Date</option>
                                    </select>
                                </div>
                                <div class="flex items-center gap-2 pt-4">
                                    <input type="checkbox" v-model="field.required" :id="'req_' + index">
                                    <label :for="'req_' + index" class="text-[10px] font-mono uppercase opacity-60">Required</label>
                                </div>
                            </div>
                            <div v-if="field.type === 'select'" class="mt-4">
                                <label class="block text-[9px] font-mono uppercase opacity-40 mb-1">Options (Comma separated)</label>
                                <textarea v-model="field.options_string" @input="updateOptions(field)" class="w-full bg-[#0e0e0e] border border-[#333] p-2 text-xs focus:outline-none focus:border-[#F27D26]" placeholder="Support, Sales, Feedback"></textarea>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="flex gap-4 pt-8">
                    <cms-button class="flex-1" @click="onSave">Save Form</cms-button>
                    <cms-button variant="outline" class="px-6" @click="$emit('cancel')">Cancel</cms-button>
                </div>
            </div>
        </div>
    `
};
