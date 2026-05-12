/**
 * AdminSitesList - Component to list and manage sites
 */
import CmsCard from './CmsCard.js';
import CmsButton from './CmsButton.js';

export default {
    name: 'AdminSitesList',
    components: {
        CmsCard,
        CmsButton
    },
    props: {
        sites: {
            type: Array,
            default: () => []
        }
    },
    emits: ['download', 'upload', 'manage-forms'],
    setup(props, { emit }) {
        return {
            onDownload: (site) => emit('download', site),
            onUpload: (site) => emit('upload', site),
            onManageForms: (site) => emit('manage-forms', site)
        };
    },
    template: `
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <cms-card 
                v-for="site in sites" 
                :key="site"
                :title="site"
                :subtitle="'/content/' + site"
                icon="layout-grid"
            >
                <template #actions>
                    <cms-button variant="secondary" size="sm" icon="download" @click="onDownload(site)">
                        Download
                    </cms-button>
                    <cms-button variant="secondary" size="sm" icon="upload" @click="onUpload(site)">
                        Upload
                    </cms-button>
                    <cms-button variant="primary" size="sm" icon="form-input" @click="onManageForms(site)">
                        Forms
                    </cms-button>
                </template>
            </cms-card>
        </div>
    `
};
