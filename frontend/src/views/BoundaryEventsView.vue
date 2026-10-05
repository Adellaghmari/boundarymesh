<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
import { RouterLink } from 'vue-router'
import OutcomeBadge from '@/components/OutcomeBadge.vue'
import TruthBanner from '@/components/TruthBanner.vue'
import { crossingLabel } from '@/domain/labels'
import { useEventsStore } from '@/stores/events'
import { storeToRefs } from 'pinia'

const store = useEventsStore()
const { newestFirst } = storeToRefs(store)
</script>

<template>
  <div class="stack" data-inspect="events">
    <header class="stack">
      <div class="cluster">
        <h1>Boundary events</h1>
        <TruthBanner label="LOCAL / SYNTHETIC SESSION DATA" />
      </div>
      <p>
        Phase 2 has no persistent event store. This list is the evaluations from the current browser
        session. Refreshing the page resets it.
      </p>
    </header>

    <p v-if="newestFirst.length === 0" class="card muted">
      No session evaluations yet.
      <RouterLink to="/">Evaluate a crossing on Overview</RouterLink>
      to populate this list.
    </p>

    <table v-else class="events">
      <caption class="visually-hidden">
        Session boundary events
      </caption>
      <thead>
        <tr>
          <th scope="col">Transaction ID</th>
          <th scope="col">Source</th>
          <th scope="col">Destination</th>
          <th scope="col">Outcome</th>
          <th scope="col">Timestamp</th>
          <th scope="col">Primary reason</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="event in newestFirst" :key="event.transactionId">
          <td>
            <RouterLink
              :to="{
                name: 'boundary-event-detail',
                params: { transactionId: event.transactionId },
              }"
            >
              {{ $show(event.transactionId) }}
            </RouterLink>
          </td>
          <td>{{ $show(crossingLabel(event.sourceSystem, event.destinationSystem).split(' → ')[0]) }}</td>
          <td>{{ $show(crossingLabel(event.sourceSystem, event.destinationSystem).split(' → ')[1]) }}</td>
          <td><OutcomeBadge :outcome="event.outcome" /></td>
          <td>{{ $show(event.decidedAt) }}</td>
          <td>{{ $show(event.reasonCodes[0]?.code ?? '—') }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.events {
  width: 100%;
  border-collapse: collapse;
  background: $color-surface;
  border: 1px solid $color-border;
  font-size: 0.92rem;
}

th,
td {
  text-align: left;
  padding: $space-3;
  border-bottom: 1px solid $color-border;
  vertical-align: top;
  overflow-wrap: anywhere;
}

th {
  background: $color-surface-raised;
  font-size: 0.8rem;
  color: $color-ink-muted;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
}

@media (max-width: $bp-md) {
  .events,
  thead,
  tbody,
  th,
  td,
  tr {
    display: block;
  }

  thead {
    display: none;
  }

  tr {
    padding: $space-3;
    border-bottom: 1px solid $color-border;
  }
}
</style>
