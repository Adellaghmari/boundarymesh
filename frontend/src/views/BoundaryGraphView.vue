<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { storeToRefs } from 'pinia'
import TruthBanner from '@/components/TruthBanner.vue'
import type { BoundaryGraphEdge, BoundaryGraphNode } from '@/domain/types'
import { CONTROL_CATALOG } from '@/domain/labels'
import { useEvidenceStore } from '@/stores/evidence'

const LAYOUT: Record<string, { x: number; y: number }> = {
  QUAYFORGE: { x: 90, y: 70 },
  HELIX365: { x: 90, y: 230 },
  BOUNDARYMESH: { x: 360, y: 150 },
  COVECRM: { x: 640, y: 50 },
  LUMENWAY: { x: 640, y: 150 },
  RIVERTIDE: { x: 640, y: 250 },
}

const store = useEvidenceStore()
const { graph, errorMessage, loading, lastDelivery, attestation } = storeToRefs(store)
const selectedEdgeId = ref<string | null>(null)
const selectedNodeId = ref<string | null>(null)
const filter = ref<'ALL' | 'QUARANTINED' | 'REJECTED' | 'ACTIVE'>('ALL')

onMounted(() => {
  void store.loadGraph()
})

const visibleEdges = computed(() => {
  const edges = graph.value?.edges ?? []
  if (filter.value === 'QUARANTINED') {
    return edges.filter((edge) => edge.status === 'QUARANTINED')
  }
  if (filter.value === 'REJECTED') {
    return edges.filter((edge) => edge.status === 'BLOCKED' || edge.recentOutcome === 'REJECT')
  }
  if (filter.value === 'ACTIVE') {
    return edges.filter((edge) => edge.status === 'ACTIVE')
  }
  return edges
})

const selectedEdge = computed(() =>
  graph.value?.edges.find((edge) => edge.id === selectedEdgeId.value) ?? null,
)

function point(id: string): { x: number; y: number } {
  return LAYOUT[id] ?? { x: 360, y: 150 }
}

function statusClass(status: string): string {
  return `status-${status.toLowerCase()}`
}

function selectEdge(edge: BoundaryGraphEdge): void {
  selectedEdgeId.value = edge.id
  selectedNodeId.value = null
  if (edge.attestationId) {
    void store.loadAttestation(edge.attestationId)
  }
}

function selectNode(node: BoundaryGraphNode): void {
  selectedNodeId.value = node.id
  selectedEdgeId.value = null
}

async function runDemo(scenario: string): Promise<void> {
  await store.runCrossing(scenario)
  const destination =
    scenario === 'transformed-delivery'
      ? 'LUMENWAY'
      : scenario === 'replay-reject'
        ? 'COVECRM'
        : scenario === 'contract-drift'
          ? 'RIVERTIDE'
          : 'LUMENWAY'
  const edge = store.graph?.edges.find((item) => item.destination === destination && item.id !== 'helix365-calendar')
  if (edge) {
    selectEdge(edge)
  }
}
</script>

<template>
  <div class="stack">
    <header class="stack">
      <div class="cluster">
        <h1>Integration Boundary Graph</h1>
        <TruthBanner />
      </div>
      <p>
        Security boundaries between external systems. This is not a service blast-radius graph and
        not a static architecture poster. Nodes and edges come from the local Java graph API.
      </p>
    </header>
    <p v-if="graph" class="muted">{{ graph.banner }}</p>
    <p v-if="errorMessage" class="card fail" role="alert">{{ errorMessage }}</p>

    <section class="card stack">
      <h2>One-minute crossing</h2>
      <div class="cluster">
        <button type="button" class="action" :disabled="loading" @click="runDemo('transformed-delivery')">
          QuayForge → Lumenway delivery
        </button>
        <button type="button" class="action" :disabled="loading" @click="runDemo('contract-drift')">
          Contract drift
        </button>
        <button type="button" class="action" :disabled="loading" @click="runDemo('replay-reject')">
          Replay reject
        </button>
      </div>
      <p v-if="lastDelivery">
        Last crossing {{ lastDelivery.decision.outcome }} · delivery
        {{ lastDelivery.delivery?.kind }} · attestation {{ lastDelivery.evidence?.attestationStatus }}
      </p>
    </section>

    <fieldset class="filters">
      <legend>Filter</legend>
      <label><input v-model="filter" type="radio" value="ALL" /> All</label>
      <label><input v-model="filter" type="radio" value="ACTIVE" /> Active</label>
      <label><input v-model="filter" type="radio" value="QUARANTINED" /> Quarantined</label>
      <label><input v-model="filter" type="radio" value="REJECTED" /> Rejected</label>
    </fieldset>

    <svg
      v-if="graph"
      class="graph"
      viewBox="0 0 760 320"
      role="img"
      aria-label="Integration boundary graph"
    >
      <title>Integration boundary graph</title>
      <g v-for="edge in visibleEdges" :key="edge.id">
        <path
          :d="`M ${point(edge.source).x + 70} ${point(edge.source).y + 22} L ${point('BOUNDARYMESH').x} ${point('BOUNDARYMESH').y + 22}`"
          class="edge"
          :class="[statusClass(edge.status), { 'is-selected': selectedEdgeId === edge.id }]"
          @click="selectEdge(edge)"
        />
        <path
          :d="`M ${point('BOUNDARYMESH').x + 70} ${point('BOUNDARYMESH').y + 22} L ${point(edge.destination).x} ${point(edge.destination).y + 22}`"
          class="edge"
          :class="[statusClass(edge.status), { 'is-selected': selectedEdgeId === edge.id }]"
          @click="selectEdge(edge)"
        />
      </g>
      <g
        v-for="node in graph.nodes"
        :key="node.id"
        class="node"
        :class="{ 'is-selected': selectedNodeId === node.id, 'is-plane': node.kind === 'CONTROL_PLANE' }"
        :transform="`translate(${point(node.id).x}, ${point(node.id).y})`"
        @click="selectNode(node)"
      >
        <rect width="140" height="44" rx="4" />
        <text x="70" y="27" text-anchor="middle">{{ node.label }}</text>
      </g>
    </svg>

    <section v-if="selectedNodeId" class="card stack">
      <h2>System</h2>
      <p>{{ selectedNodeId }}</p>
    </section>

    <section v-if="selectedEdge" class="card stack" data-testid="edge-detail">
      <h2>{{ selectedEdge.source }} → BoundaryMesh → {{ selectedEdge.destination }}</h2>
      <dl class="meta">
        <div><dt>Identity</dt><dd>{{ selectedEdge.identityMechanism }}</dd></div>
        <div><dt>Contract</dt><dd>{{ selectedEdge.contractReference }}</dd></div>
        <div><dt>Data policy</dt><dd>{{ selectedEdge.egressPolicy }}</dd></div>
        <div><dt>Credential</dt><dd>{{ selectedEdge.credentialReference }}</dd></div>
        <div><dt>Consent</dt><dd>{{ selectedEdge.consentState }}</dd></div>
        <div><dt>Connector</dt><dd>{{ selectedEdge.connectorRoute }}</dd></div>
        <div>
          <dt>Observed</dt>
          <dd data-testid="edge-status">
            {{ selectedEdge.status }} · configured {{ selectedEdge.configured ? 'yes' : 'no' }} ·
            observed {{ selectedEdge.observed ? 'yes' : 'no' }}
          </dd>
        </div>
        <div><dt>Recent outcome</dt><dd>{{ selectedEdge.recentOutcome ?? 'none' }}</dd></div>
        <div><dt>Delivery</dt><dd>{{ selectedEdge.recentDelivery ?? 'none' }}</dd></div>
        <div><dt>Quarantine count</dt><dd>{{ selectedEdge.quarantineCount }}</dd></div>
        <div>
          <dt>Audit chain</dt>
          <dd>
            <RouterLink :to="`/audit/${selectedEdge.auditChainId}`">{{ selectedEdge.auditChainId }}</RouterLink>
          </dd>
        </div>
        <div>
          <dt>Attestation</dt>
          <dd>
            <RouterLink v-if="selectedEdge.attestationId" :to="`/attestations/${selectedEdge.attestationId}`">
              {{ selectedEdge.attestationId }}
            </RouterLink>
            <span v-else>none</span>
          </dd>
        </div>
      </dl>
      <h3>Enforcement stages</h3>
      <ol class="stages" aria-label="Enforcement stages">
        <li v-for="control in CONTROL_CATALOG" :key="control.kind">
          <strong>{{ control.label }}</strong>
          <span>{{ attestation?.controlResults[control.kind] ?? 'from last crossing metadata' }}</span>
        </li>
        <li>
          <strong>Boundary Outcome</strong>
          <span>{{ selectedEdge.recentOutcome ?? selectedEdge.status }}</span>
        </li>
        <li>
          <strong>Delivery Result</strong>
          <span>{{ selectedEdge.recentDelivery ?? 'NO_RECENT_ACTIVITY' }}</span>
        </li>
      </ol>
    </section>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.action {
  background: $color-header;
  color: $color-header-ink;
  border: 0;
  border-radius: $radius-sm;
  padding: $space-2 $space-4;
}

.fail {
  background: $color-reject-soft;
  color: $color-reject;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: $space-4;
  border: 1px solid $color-border;
  border-radius: $radius-md;
  padding: $space-3 $space-4;
}

.graph {
  width: 100%;
  background: $color-surface;
  border: 1px solid $color-border;
  border-radius: $radius-md;
}

.edge {
  fill: none;
  stroke: $color-border;
  stroke-width: 3;
  cursor: pointer;
}

.edge.is-selected {
  stroke: $color-accent;
  stroke-width: 5;
}

.edge.status-active {
  stroke: $color-allow;
}

.edge.status-blocked {
  stroke: $color-reject;
}

.edge.status-quarantined {
  stroke: $color-quarantine;
}

.edge.status-no_recent_activity {
  stroke-dasharray: 6 4;
}

.node {
  cursor: pointer;
}

.node rect {
  fill: $color-surface-raised;
  stroke: $color-border;
}

.node.is-plane rect {
  fill: $color-accent-soft;
  stroke: $color-accent;
}

.node.is-selected rect {
  stroke: $color-accent;
  stroke-width: 2;
}

.node text {
  font-size: 11px;
  font-weight: 700;
  fill: $color-ink;
}

.meta {
  display: grid;
  gap: $space-2;
}

dt {
  color: $color-ink-muted;
  font-size: 0.8rem;
}

dd {
  margin: 0;
}

.stages {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: $space-2;
}

.stages li {
  display: flex;
  justify-content: space-between;
  gap: $space-3;
  padding: $space-2 $space-3;
  background: $color-surface-raised;
  border: 1px solid $color-border;
  border-radius: $radius-sm;
}
</style>
