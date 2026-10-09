<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { storeToRefs } from 'pinia'
import TruthBanner from '@/components/TruthBanner.vue'
import type { BoundaryGraphEdge, BoundaryGraphNode } from '@/domain/types'
import { CONTROL_CATALOG } from '@/domain/labels'
import { useEvidenceStore } from '@/stores/evidence'

const NODE_W = 188
const NODE_H = 44
const VIEW_W = 880
const VIEW_H = 308

const LAYOUT: Record<string, { x: number; y: number }> = {
  QUAYFORGE: { x: 48, y: 36 },
  M365: { x: 48, y: 132 },
  HELIX365: { x: 48, y: 228 },
  BOUNDARYMESH: { x: 346, y: 132 },
  COVECRM: { x: 644, y: 36 },
  LUMENWAY: { x: 644, y: 132 },
  RIVERTIDE: { x: 644, y: 228 },
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
  return LAYOUT[id] ?? { x: 346, y: 132 }
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
  <div class="stack" data-inspect="graph">
    <header class="stack">
      <div class="cluster">
        <h1>Integration Boundary Graph</h1>
        <TruthBanner />
      </div>
      <p>
        Security boundaries between external systems. This is not a service blast radius graph and
        not a static architecture poster. Nodes and edges come from the local Java graph API.
      </p>
    </header>
    <p v-if="graph" class="muted">{{ $show(graph.banner) }}</p>
    <p v-if="errorMessage" class="card fail" role="alert">{{ $show(errorMessage) }}</p>

    <section class="card stack">
      <h2>One minute crossing</h2>
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
        Last crossing {{ $show(lastDelivery.decision.outcome) }} · delivery
        {{ $show(lastDelivery.delivery?.kind) }} · attestation {{ $show(lastDelivery.evidence?.attestationStatus) }}
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
      :viewBox="`0 0 ${VIEW_W} ${VIEW_H}`"
      role="img"
      aria-label="Integration boundary graph"
    >
      <title>Integration boundary graph</title>
      <g v-for="edge in visibleEdges" :key="edge.id">
        <path
          :d="`M ${point(edge.source).x + NODE_W / 2} ${point(edge.source).y + NODE_H / 2} L ${point('BOUNDARYMESH').x} ${point('BOUNDARYMESH').y + NODE_H / 2}`"
          class="edge"
          :class="[statusClass(edge.status), { 'is-selected': selectedEdgeId === edge.id }]"
          @click="selectEdge(edge)"
        />
        <path
          :d="`M ${point('BOUNDARYMESH').x + NODE_W / 2} ${point('BOUNDARYMESH').y + NODE_H / 2} L ${point(edge.destination).x} ${point(edge.destination).y + NODE_H / 2}`"
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
        <rect :width="NODE_W" :height="NODE_H" rx="4" />
        <text :x="NODE_W / 2" :y="28" text-anchor="middle">{{ $show(node.label) }}</text>
      </g>
    </svg>

    <section v-if="selectedNodeId" class="card stack">
      <h2>System</h2>
      <p>{{ $show(selectedNodeId) }}</p>
    </section>

    <section v-if="selectedEdge" class="card stack" data-testid="edge-detail">
      <h2>{{ $show(selectedEdge.source) }} → BoundaryMesh → {{ $show(selectedEdge.destination) }}</h2>
      <dl class="meta">
        <div><dt>Identity</dt><dd>{{ $show(selectedEdge.identityMechanism) }}</dd></div>
        <div><dt>Contract</dt><dd>{{ $show(selectedEdge.contractReference) }}</dd></div>
        <div><dt>Data policy</dt><dd>{{ $show(selectedEdge.egressPolicy) }}</dd></div>
        <div><dt>Credential</dt><dd>{{ $show(selectedEdge.credentialReference) }}</dd></div>
        <div><dt>Consent</dt><dd>{{ $show(selectedEdge.consentState) }}</dd></div>
        <div><dt>Connector</dt><dd>{{ $show(selectedEdge.connectorRoute) }}</dd></div>
        <div>
          <dt>Observed</dt>
          <dd data-testid="edge-status">
            {{ $show(selectedEdge.status) }} · configured {{ $show(selectedEdge.configured ? 'yes' : 'no') }} ·
            observed {{ $show(selectedEdge.observed ? 'yes' : 'no') }}
          </dd>
        </div>
        <div><dt>Recent outcome</dt><dd>{{ $show(selectedEdge.recentOutcome ?? 'none') }}</dd></div>
        <div><dt>Delivery</dt><dd>{{ $show(selectedEdge.recentDelivery ?? 'none') }}</dd></div>
        <div><dt>Quarantine count</dt><dd>{{ $show(selectedEdge.quarantineCount) }}</dd></div>
        <div>
          <dt>Audit chain</dt>
          <dd>
            <RouterLink :to="`/audit/${selectedEdge.auditChainId}`">{{ $show(selectedEdge.auditChainId) }}</RouterLink>
          </dd>
        </div>
        <div>
          <dt>Attestation</dt>
          <dd>
            <RouterLink v-if="selectedEdge.attestationId" :to="`/attestations/${selectedEdge.attestationId}`">
              {{ $show(selectedEdge.attestationId) }}
            </RouterLink>
            <span v-else>none</span>
          </dd>
        </div>
      </dl>
      <h3>Enforcement stages</h3>
      <ol class="stages" aria-label="Enforcement stages">
        <li v-for="control in CONTROL_CATALOG" :key="control.kind">
          <strong>{{ $show(control.label) }}</strong>
          <span>{{ $show(attestation?.controlResults[control.kind] ?? 'from last crossing metadata') }}</span>
        </li>
        <li>
          <strong>Boundary Outcome</strong>
          <span>{{ $show(selectedEdge.recentOutcome ?? selectedEdge.status) }}</span>
        </li>
        <li>
          <strong>Delivery Result</strong>
          <span>{{ $show(selectedEdge.recentDelivery ?? 'NO_RECENT_ACTIVITY') }}</span>
        </li>
      </ol>
    </section>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.action {
  @include button-primary;
}

.fail {
  background: $color-reject-soft;
  color: $color-reject;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: $space-3;
  margin: 0;
  border: 1px solid $color-border;
  border-radius: $radius-md;
  padding: $space-4;
  background: $color-surface;
  box-shadow: $shadow-card;

  legend {
    padding: 0 $space-2;
    color: $color-ink-muted;
    font-size: 0.75rem;
    font-weight: 700;
    letter-spacing: 0.06em;
    text-transform: uppercase;
  }

  label {
    display: inline-flex;
    align-items: center;
    gap: $space-2;
    padding: 0.4rem 0.75rem;
    border: 1px solid $color-border;
    border-radius: $radius-sm;
    background: $color-surface-raised;
    cursor: pointer;
  }

  label:has(input:checked) {
    border-color: $color-accent;
    background: $color-accent-soft;
    color: $color-accent-bright;
  }

  input {
    accent-color: $color-accent;
  }
}

.graph {
  display: block;
  width: min(100%, 56rem);
  margin-inline: auto;
  background: $color-surface;
  border: 1px solid $color-border;
  border-radius: $radius-md;
}

.edge {
  fill: none;
  stroke: $color-border-strong;
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
