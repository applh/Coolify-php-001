import { createApp } from 'vue';
import { createRouter, createWebHashHistory } from 'vue-router';
import App from './App.vue';
import SiteDashboard from './views/SiteDashboard.vue';
import SiteEditor from './views/SiteEditor.vue';
import './style.css';

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', component: SiteDashboard },
    { path: '/editor/:site', component: SiteEditor },
  ],
});

const app = createApp(App);
app.use(router);
app.mount('#app');
