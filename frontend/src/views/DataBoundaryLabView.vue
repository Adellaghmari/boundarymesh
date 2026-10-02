<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
import { ref } from 'vue'
import { storeToRefs } from 'pinia'
import DecisionResult from '@/components/DecisionResult.vue'
import EgressFieldTable from '@/components/EgressFieldTable.vue'
import PayloadCompare from '@/components/PayloadCompare.vue'
import TruthBanner from '@/components/TruthBanner.vue'
import { EGRESS_LAB_DESTINATIONS, EGRESS_LAB_SCENARIOS } from '@/domain/securityScenarios'
import { useEventsStore } from '@/stores/events'

const store = useEventsStore()
const { evaluating, errorMessage, lastEgressEvent } = storeToRefs(store)
const destination = ref<(typeof EGRESS_LAB_DESTINATIONS)[number]['id']>('RIVERTIDE')
const selected = ref<(typeof EGRESS_LAB_SCENARIOS)[number]['id']>('email-only')

async function run(): Promise<void> {
  await store.evaluateEgressScenario(destination.value, selected.value)
}
</script>

<template>
  <div class="stack">
    <header class="stack">
      <div class="cluster">
        <h1>Data Boundary Lab</h1>
        <TruthBanner />
      </div>
      <p>
        SYNTHETIC DEMO. Egress policy is specific to each destination. This is not AI PII
        classification and not GDPR automation. A payload that passes the contract can still be
        redacted or rejected.
      </p>
    </header>

    <section class="card lab" aria-labelledby="egress-lab-heading">
      <h2 id="egress-lab-heading">Egress for each destination</h2>
      <fieldset>
        <legend>Destination</legend>
        <label v-for="item in EGRESS_LAB_DESTINATIONS" :key="item.id" class="lab__option">
          <input v-model="destination" type="radio" name="egress-destination" :value="item.id" />
          <span>
            <strong>{{ $show(item.label) }}</strong>
            <span class="muted">{{ $show(item.summary) }}</span>
          </span>
        </label>
      </fieldset>
      <fieldset>
        <legend>Payload scenario</legend>
        <label v-for="scenario in EGRESS_LAB_SCENARIOS" :key="scenario.id" class="lab__option">
          <input v-model="selected" type="radio" name="egress-scenario" :value="scenario.id" />
          <span>
            <strong>{{ $show(scenario.label) }}</strong>
            <span class="muted">{{ $show(scenario.summary) }}</span>
          </span>
        </label>
      </fieldset>
      <button type="button" class="action" :disabled="evaluating" @click="run">
        {{ $show(evaluating ? 'Evaluating egress…' : 'Evaluate data boundary') }}
      </button>
    </section>

    <p v-if="errorMessage" class="card fail" role="alert">{{ $show(errorMessage) }}</p>

    <template v-if="lastEgressEvent && !errorMessage">
      <p class="card note">
        Contract status:
        <strong>{{ $show(lastEgressEvent.appliedControls.CONTRACT ?? 'n/a') }}</strong>
        does not automatically mean data egress is allowed.
      </p>
      <EgressFieldTable :egress="lastEgressEvent.egress" />
      <PayloadCompare
        :source="lastEgressEvent.sourcePayload"
        :transformed="lastEgressEvent.transformedPayload"
      />
      <DecisionResult :event="lastEgressEvent" show-detail-link compact />
    </template>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.lab,
.fail,
.note {
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

.note {
  background: $color-accent-soft;
}
</style>
