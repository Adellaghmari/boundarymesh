import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

export const ROUTE_NAMES = {
  overview: 'overview',
  boundaryEvents: 'boundary-events',
  boundaryEventDetail: 'boundary-event-detail',
  connections: 'connections',
  architecture: 'architecture',
  identity: 'identity',
  webhooks: 'webhooks',
  status: 'status',
  notFound: 'not-found',
} as const

export const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: ROUTE_NAMES.overview,
    component: () => import('@/views/OverviewView.vue'),
    meta: { title: 'Overview' },
  },
  {
    path: '/boundary-events',
    name: ROUTE_NAMES.boundaryEvents,
    component: () => import('@/views/BoundaryEventsView.vue'),
    meta: { title: 'Boundary Events' },
  },
  {
    path: '/boundary-events/:transactionId',
    name: ROUTE_NAMES.boundaryEventDetail,
    component: () => import('@/views/BoundaryEventDetailView.vue'),
    meta: { title: 'Boundary Event' },
    props: true,
  },
  {
    path: '/connections',
    name: ROUTE_NAMES.connections,
    component: () => import('@/views/ConnectionsView.vue'),
    meta: { title: 'Connections' },
  },
  {
    path: '/architecture',
    name: ROUTE_NAMES.architecture,
    component: () => import('@/views/ArchitectureView.vue'),
    meta: { title: 'Architecture' },
  },
  {
    path: '/identity',
    name: ROUTE_NAMES.identity,
    component: () => import('@/views/IdentityLabView.vue'),
    meta: { title: 'Identity Lab' },
  },
  {
    path: '/webhooks',
    name: ROUTE_NAMES.webhooks,
    component: () => import('@/views/WebhookLabView.vue'),
    meta: { title: 'Webhook Lab' },
  },
  {
    path: '/status',
    name: ROUTE_NAMES.status,
    component: () => import('@/views/StatusView.vue'),
    meta: { title: 'Status' },
  },
  {
    path: '/:pathMatch(.*)*',
    name: ROUTE_NAMES.notFound,
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: 'Not found' },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

router.afterEach((to) => {
  const page = typeof to.meta.title === 'string' ? to.meta.title : 'Console'
  document.title = `${page} · BoundaryMesh`
})

export default router
