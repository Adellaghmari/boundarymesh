<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { storeToRefs } from 'pinia'
import OutcomeBadge from '@/components/OutcomeBadge.vue'
import TruthBanner from '@/components/TruthBanner.vue'
import { seedPublicDemo } from '@/demo/seedPublicDemo'
import { crossingLabel } from '@/domain/labels'
import TimestampLabel from '@/components/TimestampLabel.vue'
import { useEventsStore } from '@/stores/events'

const store = useEventsStore()
const { newestFirst, errorMessage } = storeToRefs(store)
const seeding = ref(false)

onMounted(async () => {
  if (store.events.length > 0) return
  seeding.value = true
  try {
    await seedPublicDemo()
  } finally {
    seeding.value = false
  }
})
</script>

<template>
  <div class="stack" data-inspect="events">
    <header class="stack">
      <p class="eyebrow">Session evidence</p>
      <div class="cluster">
        <h1>Boundary Events</h1>
        <TruthBanner label="LOCAL / SYNTHETIC SESSION DATA" />
      </div>
      <p>
        Phase 2 has no persistent event store. This list is the evaluations from the current browser
        session. A fresh session asks the local Java engine for the four predefined crossings, the
        same scenarios the hosted demo seeds before first paint. Refreshing the page clears that
        memory and evaluates them again. Open a row to read the controls behind a decision.
      </p>
    </header>

    <p v-if="seeding" class="card muted">Evaluating the four predefined crossings.</p>
    <p v-if="errorMessage" class="card fail" role="alert">{{ $show(errorMessage) }}</p>

    <section v-if="!seeding && newestFirst.length === 0" class="card empty" aria-labelledby="events-empty-heading">
      <p class="eyebrow">Nothing recorded yet</p>
      <h2 id="events-empty-heading">No session evaluations</h2>
      <p>
        The engine did not return a decision for this session. Confirm the local BoundaryMesh API
        is running, then refresh, or evaluate one crossing on Overview.
      </p>
      <RouterLink class="action" to="/">Evaluate a crossing on Overview</RouterLink>
    </section>

    <div v-else class="events-wrap">
    <table class="events">
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
          <td><TimestampLabel :value="event.decidedAt" /></td>
          <td>{{ $show(event.reasonCodes[0]?.code ?? '—') }}</td>
        </tr>
      </tbody>
    </table>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.eyebrow {
  margin: 0;
  color: $color-accent-bright;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.empty {
  display: grid;
  gap: $space-3;
  max-width: 40rem;
}

.action {
  @include button-primary;
}

tbody tr {
  transition: background-color 140ms ease;
}

tbody tr:hover td {
  background: $color-surface-hover;
}

.events-wrap {
  overflow-x: auto;
  border: 1px solid $color-border;
  border-radius: $radius-md;
  background: $color-surface;
  box-shadow: $shadow-card;
}

.events {
  width: 100%;
  min-width: 52rem;
  border-collapse: collapse;
  background: $color-surface;
  font-size: 0.92rem;
}

th,
td {
  text-align: left;
  padding: 0.9rem 1rem;
  border-bottom: 1px solid $color-border;
  vertical-align: middle;
  overflow-wrap: normal;
  word-break: normal;
}

th {
  background: $color-surface-raised;
  color: $color-ink-muted;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

tbody tr:last-child td {
  border-bottom: 0;
}

td a {
  font-weight: 650;
  text-decoration: none;
}

td a:hover {
  text-decoration: underline;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
}

@media (max-width: $bp-md) {
  .events {
    min-width: 0;
  }

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
