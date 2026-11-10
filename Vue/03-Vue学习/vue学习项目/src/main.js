import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './lessons/15-VueRouter路由/router.js'

const app = createApp(App)
app.use(router)
app.use(createPinia())
app.mount('#app')
