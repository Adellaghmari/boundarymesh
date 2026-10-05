<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
import { onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { RouterLink } from 'vue-router'
import TruthBanner from '@/components/TruthBanner.vue'
import { VELORA_SYSTEMS } from '@/domain/connections'
import { useMicrosoftStore } from '@/stores/microsoft'

const store = useMicrosoftStore()
const { connector, lastHarness, errorMessage, loading } = storeToRefs(store)
const openIds = ref<string[]>([])

function toggle(id: string): void {
  openIds.value = openIds.value.includes(id)
    ? openIds.value.filter((item) => item !== id)
    : [...openIds.value, id]
}

onMounted(() => {
  void store.loadConnector()
})

async function runHarness(): Promise<void> {
  await store.runHarness()
}
</script>

<template>
  <div class="stack" data-inspect="connections">
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
        <span class="truth-banner">{{ $show(connector?.banner ?? 'CONTRACT HARNESS') }}</span>
      </div>
      <p>
        Outlook Calendar is read through Microsoft Graph, normalized to the calendar contract,
        then evaluated by the Data Boundary Firewall under the calendar egress policy. Delegated permission:
        <strong>Calendars.Read</strong>. This page reads GET connector status and does
        not call Graph. <code>LIVE_VERIFIED</code> is returned only by the Operator
        calendar pull after Microsoft Graph HTTP 2xx. A personal Microsoft account is
        not an enterprise Exchange Online mailbox. Helix 365 remains SYNTHETIC DEMO.
        Graph change notifications remain planned.
      </p>
      <dl class="meta">
        <div>
          <dt>Mode</dt>
          <dd data-testid="m365-mode">{{ $show(connector?.mode ?? 'CONTRACT_HARNESS') }}</dd>
        </div>
        <div>
          <dt>Permission</dt>
          <dd>{{ $show(connector?.permissionKind) }} {{ $show(connector?.permission) }}</dd>
        </div>
        <div>
          <dt>Live Graph</dt>
          <dd>{{ $show(connector?.live ? 'yes' : 'no') }}</dd>
        </div>
      </dl>
      <p v-if="errorMessage" class="muted" role="alert">{{ $show(errorMessage) }}</p>
      <button type="button" class="action" :disabled="loading" @click="runHarness">
        Run contract harness crossing
      </button>
      <p v-if="lastHarness" data-testid="m365-harness">
        Harness outcome recorded. Subjects and attendees are not displayed.
      </p>
    </article>

    <article v-for="system in VELORA_SYSTEMS" :key="system.id" class="card stack" :data-testid="`connection-${system.id}`">
      <div class="cluster">
        <h2>{{ $show(system.name) }}</h2>
        <span class="truth-banner">{{ $show(system.connectorStatus) }}</span>
      </div>
      <p><strong>Role.</strong> {{ $show(system.role) }}</p>
      <p>{{ $show(system.character) }}</p>
      <div class="actions">
        <button
          type="button"
          class="action"
          :aria-expanded="openIds.includes(system.id)"
          :aria-controls="`connection-details-${system.id}`"
          @click="toggle(system.id)"
        >
          View connection details
        </button>
        <RouterLink v-for="action in system.actions" :key="action.label" :to="action.to">
          {{ action.label }}
        </RouterLink>
      </div>
      <dl v-if="openIds.includes(system.id)" :id="`connection-details-${system.id}`" class="meta">
        <div>
          <dt>Mode</dt>
          <dd>{{ $show(system.mode) }}</dd>
        </div>
        <div>
          <dt>Source</dt>
          <dd>{{ $show(system.source) }}</dd>
        </div>
        <div>
          <dt>Destination</dt>
          <dd>{{ $show(system.destination) }}</dd>
        </div>
        <div>
          <dt>Contract</dt>
          <dd>{{ $show(system.contract) }}</dd>
        </div>
        <div>
          <dt>Permissions</dt>
          <dd>{{ $show(system.permissions) }}</dd>
        </div>
        <div>
          <dt>Policy path</dt>
          <dd>{{ $show(system.policyPath) }}</dd>
        </div>
        <div>
          <dt>Crossing</dt>
          <dd>{{ $show(system.crossing) }}</dd>
        </div>
        <div>
          <dt>Synthetic status</dt>
          <dd>{{ $show(system.syntheticStatus) }}</dd>
        </div>
        <div>
          <dt>Recent synthetic outcome</dt>
          <dd>{{ $show(system.recentOutcome) }}</dd>
        </div>
      </dl>
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

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: $space-3;
  align-items: center;
}
</style>
