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
import AgentTeams from './views/AgentTeams.vue';
import './style.css';

// Global Fetch Override for Admin Protection
const originalFetch = window.fetch;
window.fetch = async (url, options: RequestInit = {}) => {
  const passkey = localStorage.getItem('admin_passkey');
  if (passkey) {
    options.headers = {
      ...options.headers,
      'x-admin-passkey': passkey
    };
  }
  const response = await originalFetch(url, options);
  if (response.status === 401 && !url.toString().includes('/api/auth/verify')) {
    window.dispatchEvent(new CustomEvent('admin-auth-failed'));
  }
  return response;
};

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', component: SiteDashboard },
    { path: '/editor/:site', component: SiteEditor },
    { path: '/explorer', component: SiteExplorer },
    { path: '/ai-media', component: AiMediaTasks },
    { path: '/agent-teams', component: AgentTeams },
    { path: '/benchmark', component: SiteBenchmarker },
    { path: '/sync', component: SyncState },
    { path: '/training', component: TrainingCenter },
  ],
});

const app = createApp(App);
app.use(router);
app.mount('#app');
