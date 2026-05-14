import { createApp } from 'vue';
import { createRouter, createWebHashHistory } from 'vue-router';
import App from './App.vue';
import SiteDashboard from './views/SiteDashboard.vue';
import SiteEditor from './views/SiteEditor.vue';
import SiteExplorer from './views/SiteExplorer.vue';
import AiMediaTasks from './views/AiMediaTasks.vue';
import SiteBenchmarker from './views/SiteBenchmarker.vue';
import SyncState from './views/SyncState.vue';
import TrainingCenter from './views/TrainingCenter.vue';
import './style.css';

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', component: SiteDashboard },
    { path: '/editor/:site', component: SiteEditor },
    { path: '/explorer', component: SiteExplorer },
    { path: '/ai-media', component: AiMediaTasks },
    { path: '/benchmark', component: SiteBenchmarker },
    { path: '/sync', component: SyncState },
    { path: '/training', component: TrainingCenter },
  ],
});

const app = createApp(App);
app.use(router);
app.mount('#app');
