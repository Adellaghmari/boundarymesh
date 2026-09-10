<script setup lang="ts">
import { Shield } from '@lucide/vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { ROUTE_NAMES } from '@/router'

const route = useRoute()

const links = [
  { name: ROUTE_NAMES.overview, to: '/', label: 'Overview' },
  { name: ROUTE_NAMES.boundaryEvents, to: '/boundary-events', label: 'Boundary Events' },
  { name: ROUTE_NAMES.connections, to: '/connections', label: 'Connections' },
  { name: ROUTE_NAMES.architecture, to: '/architecture', label: 'Architecture' },
  { name: ROUTE_NAMES.identity, to: '/identity', label: 'Identity Lab' },
  { name: ROUTE_NAMES.webhooks, to: '/webhooks', label: 'Webhook Lab' },
  { name: ROUTE_NAMES.contracts, to: '/contracts', label: 'Contract Lab' },
  { name: ROUTE_NAMES.dataBoundary, to: '/data-boundary', label: 'Data Boundary' },
  { name: ROUTE_NAMES.crossing, to: '/crossing', label: 'Full Crossing' },
  { name: ROUTE_NAMES.delivery, to: '/delivery', label: 'Delivery Lab' },
  { name: ROUTE_NAMES.credentials, to: '/credentials', label: 'Credentials' },
  { name: ROUTE_NAMES.quarantine, to: '/quarantine', label: 'Quarantine' },
  { name: ROUTE_NAMES.audit, to: '/audit', label: 'Audit Chain' },
  { name: ROUTE_NAMES.attestations, to: '/attestations', label: 'Attestations' },
  { name: ROUTE_NAMES.boundaryGraph, to: '/boundary-graph', label: 'Boundary Graph' },
  { name: ROUTE_NAMES.status, to: '/status', label: 'Status' },
] as const

function isActive(name: string): boolean {
  if (name === ROUTE_NAMES.boundaryEvents) {
    return (
      route.name === ROUTE_NAMES.boundaryEvents || route.name === ROUTE_NAMES.boundaryEventDetail
    )
  }
  if (name === ROUTE_NAMES.quarantine) {
    return route.name === ROUTE_NAMES.quarantine || route.name === ROUTE_NAMES.quarantineDetail
  }
  if (name === ROUTE_NAMES.audit) {
    return route.name === ROUTE_NAMES.audit || route.name === ROUTE_NAMES.auditDetail
  }
  if (name === ROUTE_NAMES.attestations) {
    return route.name === ROUTE_NAMES.attestations || route.name === ROUTE_NAMES.attestationDetail
  }
  return route.name === name
}
</script>

<template>
  <a class="skip-link" href="#main">Skip to content</a>
  <div class="shell">
    <header class="shell__header">
      <div class="shell__brand">
        <Shield :size="22" aria-hidden="true" />
        <div>
          <p class="shell__name">BoundaryMesh</p>
          <p class="shell__tagline">Secure every system crossing.</p>
        </div>
      </div>
      <nav class="shell__nav" aria-label="Primary">
        <RouterLink
          v-for="link in links"
          :key="link.name"
          :to="link.to"
          class="shell__link"
          :class="{ 'is-active': isActive(link.name) }"
          :aria-current="isActive(link.name) ? 'page' : undefined"
        >
          {{ link.label }}
        </RouterLink>
      </nav>
    </header>
    <main id="main" class="shell__main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.shell {
  min-height: 100vh;
}

.shell__header {
  background: $color-header;
  color: $color-header-ink;
  padding: $space-4 $space-5;
  display: grid;
  gap: $space-4;
}

.shell__brand {
  display: flex;
  gap: $space-3;
  align-items: center;
}

.shell__name {
  font-weight: 700;
  letter-spacing: 0.02em;
}

.shell__tagline {
  color: #c5e8e5;
  font-size: 0.9rem;
}

.shell__nav {
  display: flex;
  flex-wrap: wrap;
  gap: $space-2;
}

.shell__link {
  color: $color-header-ink;
  text-decoration: none;
  padding: $space-2 $space-3;
  border-radius: $radius-sm;
  border: 1px solid transparent;
}

.shell__link.is-active {
  background: rgb(255 255 255 / 12%);
  border-color: rgb(255 255 255 / 24%);
}

.shell__main {
  width: min(100%, $measure);
  margin: 0 auto;
  padding: $space-6 $space-4 $space-7;
}

@media (min-width: $bp-md) {
  .shell__header {
    grid-template-columns: auto 1fr;
    align-items: end;
  }

  .shell__nav {
    justify-content: flex-end;
  }
}
</style>
