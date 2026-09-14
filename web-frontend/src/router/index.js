import { createRouter, createWebHistory } from 'vue-router'
import { TOKEN_KEY } from '../api/request'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('../views/DashboardView.vue'), meta: { title: '数据概览' } },
      { path: 'buildings', name: 'buildings', component: () => import('../views/BuildingView.vue'), meta: { title: '楼栋管理' } },
      { path: 'rooms', name: 'rooms', component: () => import('../views/RoomView.vue'), meta: { title: '房间床位管理' } },
      { path: 'students', name: 'students', component: () => import('../views/StudentView.vue'), meta: { title: '学生管理' } },
      { path: 'stays', name: 'stays', component: () => import('../views/StayView.vue'), meta: { title: '入住退住办理' } },
      { path: 'stats', name: 'stats', component: () => import('../views/StatView.vue'), meta: { title: '查询统计' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：没有 token 一律回登录页
router.beforeEach((to) => {
  const hasToken = !!localStorage.getItem(TOKEN_KEY)
  if (!to.meta.public && !hasToken) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && hasToken) {
    return { path: '/dashboard' }
  }
  return true
})

router.afterEach((to) => {
  document.title = (to.meta.title ? to.meta.title + ' · ' : '') + '寓安 · 学生宿舍管理系统'
})

export default router
