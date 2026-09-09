<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import DecisionResult from '@/components/DecisionResult.vue'
import TruthBanner from '@/components/TruthBanner.vue'
import { useEventsStore } from '@/stores/events'

const route = useRoute()
const store = useEventsStore()

const transactionId = computed(() => String(route.params.transactionId ?? ''))
const event = computed(() => store.eventById(transactionId.value))
</script>

<template>
  <div class="stack">
    <p>
      <RouterLink to="/boundary-events">Back to boundary events</RouterLink>
    </p>
    <div class="cluster">
      <h1>Boundary event</h1>
      <TruthBanner />
    </div>

    <DecisionResult v-if="event" :event="event" />

    <section v-else class="card stack">
      <h2>Not in this session</h2>
      <p>
        Transaction <code>{{ transactionId }}</code> is not in the current browser session. Phase 2
        has no persistent event store, so a refresh or a new tab cannot recover earlier evaluations.
      </p>
      <p>
        <RouterLink to="/">Evaluate a crossing</RouterLink>
        or return to
        <RouterLink to="/boundary-events">the session list</RouterLink>.
      </p>
    </section>
  </div>
</template>
