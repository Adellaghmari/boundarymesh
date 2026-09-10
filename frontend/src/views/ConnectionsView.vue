<script setup lang="ts">
import { onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import TruthBanner from '@/components/TruthBanner.vue'
import { VELORA_SYSTEMS } from '@/domain/connections'
import { useMicrosoftStore } from '@/stores/microsoft'

const store = useMicrosoftStore()
const { connector, lastHarness, errorMessage, loading } = storeToRefs(store)

onMounted(() => {
  void store.loadConnector()
})

async function runHarness(): Promise<void> {
  await store.runHarness()
}
</script>

<template>
  <div class="stack">
    <header class="stack">
      <div class="cluster">
        <h1>Connections</h1>
        <TruthBanner />
      </div>
      <p>
        Velora Interchange remains the fictional demo environment. Microsoft 365 Calendar is a real
        connector type that still runs through BoundaryMesh contracts, egress, audit, and attestation.
        Helix 365 is not Microsoft 365.
      </p>
    </header>

    <article class="card stack" data-testid="m365-connector">
      <div class="cluster">
        <h2>Microsoft 365 Calendar</h2>
        <span class="truth-banner">{{ connector?.banner ?? 'CONTRACT HARNESS' }}</span>
      </div>
      <p>
        Outlook Calendar is read through Microsoft Graph, normalized to
        <code>m365-calendar/v1</code>, then evaluated by the Data Boundary Firewall
        (<code>m365-calendar-egress/v1</code>). Delegated permission:
        <strong>Calendars.Read</strong>. This page reads GET connector status and does
        not call Graph. <code>LIVE_VERIFIED</code> is returned only by the Operator
        calendar pull after Microsoft Graph HTTP 2xx. A personal Microsoft account is
        not an enterprise Exchange Online mailbox. Helix 365 remains SYNTHETIC DEMO.
        Graph change notifications remain planned.
      </p>
      <dl class="meta">
        <div>
          <dt>Mode</dt>
          <dd data-testid="m365-mode">{{ connector?.mode ?? 'CONTRACT_HARNESS' }}</dd>
        </div>
        <div>
          <dt>Permission</dt>
          <dd>{{ connector?.permissionKind }} {{ connector?.permission }}</dd>
        </div>
        <div>
          <dt>Live Graph</dt>
          <dd>{{ connector?.live ? 'yes' : 'no' }}</dd>
        </div>
      </dl>
      <p v-if="errorMessage" class="muted" role="alert">{{ errorMessage }}</p>
      <button type="button" class="action" :disabled="loading" @click="runHarness">
        Run contract-harness crossing
      </button>
      <p v-if="lastHarness" data-testid="m365-harness">
        Harness outcome recorded. Subjects and attendees are not displayed.
      </p>
    </article>

    <article v-for="system in VELORA_SYSTEMS" :key="system.id" class="card stack">
      <div class="cluster">
        <h2>{{ system.name }}</h2>
        <span class="truth-banner">{{ system.connectorStatus }}</span>
      </div>
      <p><strong>Role.</strong> {{ system.role }}</p>
      <p>{{ system.character }}</p>
    </article>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.meta {
  display: grid;
  gap: $space-2;
}

.meta div {
  display: grid;
  grid-template-columns: 8rem 1fr;
}

dt {
  color: $color-ink-muted;
}

.action {
  justify-self: start;
}
</style>
