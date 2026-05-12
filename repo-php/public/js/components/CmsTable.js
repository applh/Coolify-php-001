/**
 * CmsTable - Reusable Table Component
 * Supports custom columns, slots for actions, and dark-themed styling.
 */
export default {
    name: 'CmsTable',
    props: {
        items: {
            type: Array,
            default: () => []
        },
        columns: {
            type: Array,
            default: () => [] // [{ key: 'id', label: 'ID', sortable: true }]
        },
        loading: {
            type: Boolean,
            default: false
        },
        emptyMessage: {
            type: String,
            default: 'No items found.'
        }
    },
    setup() {
        return {};
    },
    template: `
        <div class="border border-[#2A2A2A] bg-[#181818] overflow-hidden rounded-sm">
            <table class="w-full text-left text-xs">
                <thead class="bg-[#222] border-b border-[#2A2A2A]">
                    <tr>
                        <th v-for="col in columns" :key="col.key" class="p-3 font-mono opacity-40 uppercase tracking-widest">
                            {{ col.label }}
                        </th>
                        <th v-if="$slots.actions" class="p-3 font-mono opacity-40 uppercase tracking-widest text-right">Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-if="loading" class="animate-pulse">
                        <td :colspan="columns.length + ($slots.actions ? 1 : 0)" class="p-8 text-center opacity-30 italic">
                            Loading...
                        </td>
                    </tr>
                    <tr v-else-if="items.length === 0">
                        <td :colspan="columns.length + ($slots.actions ? 1 : 0)" class="p-8 text-center opacity-30 italic">
                            {{ emptyMessage }}
                        </td>
                    </tr>
                    <tr v-for="(item, index) in items" :key="index" class="border-b border-[#2A2A2A] last:border-0 hover:bg-[#222] transition-colors">
                        <td v-for="col in columns" :key="col.key" class="p-3">
                            <slot :name="'col-' + col.key" :item="item" :value="item[col.key]">
                                {{ item[col.key] }}
                            </slot>
                        </td>
                        <td v-if="$slots.actions" class="p-3 text-right space-x-2">
                            <slot name="actions" :item="item"></slot>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    `
};
