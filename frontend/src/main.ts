import { createPinia } from 'pinia'
import { createApp } from 'vue'

import App from './App.vue'
import { initApiClient } from './api/client'
import { createAppRouter } from './router'
import './style.css'

const app = createApp(App)

const pinia = createPinia()
const router = createAppRouter(pinia)

initApiClient(pinia, router)

app.use(pinia)
app.use(router)

app.mount('#app')
