<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
import { ref } from 'vue'
import { storeToRefs } from 'pinia'
import CrossingPipeline from '@/components/CrossingPipeline.vue'
import DecisionResult from '@/components/DecisionResult.vue'
import EgressFieldTable from '@/components/EgressFieldTable.vue'
import OutcomeBadge from '@/components/OutcomeBadge.vue'
import PayloadCompare from '@/components/PayloadCompare.vue'
import TruthBanner from '@/components/TruthBanner.vue'
import { CROSSING_LAB_SCENARIOS } from '@/domain/securityScenarios'
import { useEventsStore } from '@/stores/events'

const store = useEventsStore()
const { evaluating, errorMessage, lastCrossingEvent } = storeToRefs(store)
const selected = ref<(typeof CROSSING_LAB_SCENARIOS)[number]['id']>('composed-transform')

async function run(): Promise<void> {
  await store.evaluateCrossingScenario(selected.value)
}
</script>

<template>
  <div class="stack">
    <header class="stack">
      <div class="cluster">
        <h1>Full Boundary Crossing</h1>
        <TruthBanner />
      </div>
      <p>
        SYNTHETIC DEMO. Real local JWT identity plus contract and egress evaluation. The Java
        Boundary Enforcement Engine composes the final outcome. Connector delivery lives in Delivery
        Lab. Entra and Graph remain planned.
      </p>
    </header>

    <section class="card lab" aria-labelledby="crossing-lab-heading">
      <h2 id="crossing-lab-heading">Composed inspection</h2>
      <fieldset>
        <legend>Integrated crossing</legend>
        <label v-for="scenario in CROSSING_LAB_SCENARIOS" :key="scenario.id" class="lab__option">
          <input v-model="selected" type="radio" name="crossing-scenario" :value="scenario.id" />
          <span>
            <strong>{{ $show(scenario.label) }}</strong>
            <span class="muted">{{ $show(scenario.summary) }}</span>
          </span>
        </label>
      </fieldset>
      <button type="button" class="action" :disabled="evaluating" @click="run">
        {{ $show(evaluating ? 'Inspecting crossing…' : 'Inspect crossing') }}
      </button>
    </section>

    <p v-if="errorMessage" class="card fail" role="alert">{{ $show(errorMessage) }}</p>

    <template v-if="lastCrossingEvent && !errorMessage">
      <section class="card verdict">
        <div class="cluster">
          <h2>Final outcome</h2>
          <OutcomeBadge :outcome="lastCrossingEvent.outcome" />
        </div>
        <p v-if="lastCrossingEvent.classification">
          Contract {{ $show(lastCrossingEvent.contract?.version ?? '') }}
          {{ $show(lastCrossingEvent.classification) }}.
          Data boundary
          {{ $show(lastCrossingEvent.egress?.transformationCount ?? 0) }} field action(s).
        </p>
      </section>
      <CrossingPipeline
        :steps="lastCrossingEvent.enforcementTrace"
        :applied="lastCrossingEvent.appliedControls"
      />
      <EgressFieldTable :egress="lastCrossingEvent.egress" />
      <PayloadCompare
        :source="lastCrossingEvent.sourcePayload"
        :transformed="lastCrossingEvent.transformedPayload"
      />
      <DecisionResult :event="lastCrossingEvent" show-detail-link compact />
    </template>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.lab,
.fail,
.verdict {
  display: grid;
  gap: $space-4;
}

fieldset {
  border: 1px solid $color-border;
  border-radius: $radius-md;
  padding: $space-4;
  display: grid;
  gap: $space-3;
}

.lab__option {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: $space-3;
}

.lab__option span {
  display: grid;
  gap: $space-1;
}

.action {
  justify-self: start;
  background: $color-header;
  color: $color-header-ink;
  border: 0;
  border-radius: $radius-sm;
  padding: $space-2 $space-4;
}

.action:disabled {
  opacity: 0.7;
}

.fail {
  background: $color-reject-soft;
  color: $color-reject;
}

.verdict {
  background: $color-header;
  color: $color-header-ink;
}
</style>
