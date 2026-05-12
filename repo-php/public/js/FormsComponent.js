/**
 * Vue Async Plain JS Component for CMS Forms
 * This component handles fetching form metadata and submitting data via JSON API.
 */

/* global Vue */
const { createApp, ref, onMounted } = Vue;

const FormComponent = {
    props: {
        formId: {
            type: String,
            required: true
        },
        apiUrl: {
            type: String,
            default: '/?cms_api=get_form'
        },
        submitUrl: {
            type: String,
            default: '/?cms_api=submit_form'
        }
    },
    setup(props) {
        const form = ref(null);
        const loading = ref(true);
        const error = ref(null);
        const submitting = ref(false);
        const success = ref(false);
        const formData = ref({});

        const fetchForm = async () => {
            try {
                const response = await fetch(`${props.apiUrl}&id=${props.formId}`);
                if (!response.ok) throw new Error('Failed to load form');
                const data = await response.json();
                form.value = data;
                
                // Initialize form data
                data.fields.forEach(field => {
                    formData.value[field.name] = field.type === 'select' ? (field.options[0] || '') : '';
                });
            } catch (err) {
                error.value = err.message;
            } finally {
                loading.value = false;
            }
        };

        const handleSubmit = async () => {
            submitting.value = true;
            error.value = null;
            try {
                const response = await fetch(props.submitUrl, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        cms_form_id: form.value.id,
                        data: formData.value
                    })
                });
                const result = await response.json();
                if (result.success) {
                    success.value = true;
                } else {
                    throw new Error(result.error || 'Submission failed');
                }
            } catch (err) {
                error.value = err.message;
            } finally {
                submitting.value = false;
            }
        };

        onMounted(fetchForm);

        return {
            form,
            loading,
            error,
            submitting,
            success,
            formData,
            handleSubmit
        };
    },
    template: `
        <div class="vue-cms-form-container">
            <div v-if="loading" class="animate-pulse space-y-4">
                <div class="h-8 bg-black/5 w-1/3"></div>
                <div class="h-32 bg-black/5 w-full"></div>
            </div>

            <div v-else-if="error" class="p-4 bg-red-50 text-red-600 rounded-sm border border-red-100 text-sm">
                Error: {{ error }}
            </div>

            <div v-else-if="success" class="p-8 bg-green-50 text-center space-y-4 border border-green-100 rounded-sm">
                <div class="text-3xl">✓</div>
                <h3 class="text-xl serif italic">Thank you!</h3>
                <p class="text-sm opacity-60">Your submission has been received successfully.</p>
                <button @click="success = false" class="text-[10px] uppercase tracking-widest font-bold underline">Send another</button>
            </div>

            <form v-else @submit.prevent="handleSubmit" class="space-y-6 bg-white/50 p-8 border border-black/5 rounded-sm">
                <h3 class="text-2xl serif italic mb-4">{{ form.title }}</h3>
                <p v-if="form.description" class="text-sm opacity-60 mb-6">{{ form.description }}</p>

                <div v-for="field in form.fields" :key="field.name" class="space-y-2">
                    <label class="block text-[10px] uppercase tracking-widest opacity-40 font-mono">
                        {{ field.label }}
                        <span v-if="field.required" class="text-red-500">*</span>
                    </label>

                    <textarea 
                        v-if="field.type === 'textarea'"
                        v-model="formData[field.name]"
                        :required="field.required"
                        class="w-full bg-white border border-black/10 p-3 text-sm focus:outline-none focus:border-black/30 min-h-[120px]"
                    ></textarea>

                    <select 
                        v-else-if="field.type === 'select'"
                        v-model="formData[field.name]"
                        :required="field.required"
                        class="w-full bg-white border border-black/10 p-3 text-sm focus:outline-none focus:border-black/30"
                    >
                        <option v-for="opt in field.options" :key="opt" :value="opt">{{ opt }}</option>
                    </select>

                    <input 
                        v-else
                        :type="field.type"
                        v-model="formData[field.name]"
                        :required="field.required"
                        class="w-full bg-white border border-black/10 p-3 text-sm focus:outline-none focus:border-black/30"
                    >
                </div>

                <button 
                    type="submit" 
                    :disabled="submitting"
                    class="w-full bg-black text-white text-[10px] uppercase font-bold tracking-widest py-4 hover:bg-black/80 transition-all disabled:opacity-50"
                >
                    <span v-if="submitting">Sending...</span>
                    <span v-else>{{ form.submit_label || 'Submit' }}</span>
                </button>
            </form>
        </div>
    `
};

// Global initializer
window.initCmsForms = () => {
    document.querySelectorAll('[data-vue-form]').forEach(el => {
        const formId = el.getAttribute('data-vue-form');
        if (formId) {
            createApp(FormComponent, { formId }).mount(el);
        }
    });
};

// Auto-init if DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', window.initCmsForms);
} else {
    window.initCmsForms();
}
