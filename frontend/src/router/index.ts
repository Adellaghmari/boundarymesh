import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

export const ROUTE_NAMES = {
  overview: 'overview',
  boundaryEvents: 'boundary-events',
  boundaryEventDetail: 'boundary-event-detail',
  connections: 'connections',
  architecture: 'architecture',
  identity: 'identity',
  webhooks: 'webhooks',
  contracts: 'contracts',
  dataBoundary: 'data-boundary',
  crossing: 'crossing',
  delivery: 'delivery',
  credentials: 'credentials',
  quarantine: 'quarantine',
  quarantineDetail: 'quarantine-detail',
  audit: 'audit',
  auditDetail: 'audit-detail',
  attestations: 'attestations',
  attestationDetail: 'attestation-detail',
  boundaryGraph: 'boundary-graph',
  status: 'status',
  about: 'about',
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
    path: '/contracts',
    name: ROUTE_NAMES.contracts,
    component: () => import('@/views/ContractDriftLabView.vue'),
    meta: { title: 'Contract Drift Lab' },
  },
  {
    path: '/data-boundary',
    name: ROUTE_NAMES.dataBoundary,
    component: () => import('@/views/DataBoundaryLabView.vue'),
    meta: { title: 'Data Boundary Lab' },
  },
  {
    path: '/crossing',
    name: ROUTE_NAMES.crossing,
    component: () => import('@/views/CrossingLabView.vue'),
    meta: { title: 'Full Crossing' },
  },
  {
    path: '/delivery',
    name: ROUTE_NAMES.delivery,
    component: () => import('@/views/DeliveryLabView.vue'),
    meta: { title: 'Delivery Lab' },
  },
  {
    path: '/credentials',
    name: ROUTE_NAMES.credentials,
    component: () => import('@/views/CredentialLabView.vue'),
    meta: { title: 'Credential & Consent' },
  },
  {
    path: '/quarantine',
    name: ROUTE_NAMES.quarantine,
    component: () => import('@/views/QuarantineView.vue'),
    meta: { title: 'Quarantine' },
  },
  {
    path: '/quarantine/:id',
    name: ROUTE_NAMES.quarantineDetail,
    component: () => import('@/views/QuarantineDetailView.vue'),
    meta: { title: 'Quarantine item' },
    props: true,
  },
  {
    path: '/audit',
    name: ROUTE_NAMES.audit,
    component: () => import('@/views/AuditChainView.vue'),
    meta: { title: 'Audit Chain' },
  },
  {
    path: '/audit/:integrationId',
    name: ROUTE_NAMES.auditDetail,
    component: () => import('@/views/AuditChainDetailView.vue'),
    meta: { title: 'Audit Chain' },
    props: true,
  },
  {
    path: '/attestations',
    name: ROUTE_NAMES.attestations,
    component: () => import('@/views/AttestationsView.vue'),
    meta: { title: 'Exchange Attestations' },
  },
  {
    path: '/attestations/:id',
    name: ROUTE_NAMES.attestationDetail,
    component: () => import('@/views/AttestationDetailView.vue'),
    meta: { title: 'Exchange Attestation' },
    props: true,
  },
  {
    path: '/boundary-graph',
    name: ROUTE_NAMES.boundaryGraph,
    component: () => import('@/views/BoundaryGraphView.vue'),
    meta: { title: 'Boundary Graph' },
  },
  {
    path: '/status',
    name: ROUTE_NAMES.status,
    component: () => import('@/views/StatusView.vue'),
    meta: { title: 'Status' },
  },
  {
    path: '/about',
    name: ROUTE_NAMES.about,
    component: () => import('@/views/AboutProjectView.vue'),
    meta: { title: 'About the project' },
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
