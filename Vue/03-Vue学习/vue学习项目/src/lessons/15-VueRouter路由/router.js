import { createRouter, createWebHistory } from 'vue-router'
import HomePage from './HomePage.vue'
import StudentPage from './StudentPage.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: HomePage,
    },
    {
      path: '/students',
      component: StudentPage,
    },
  ],
})

export default router
