<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
import { ref } from 'vue'
import { storeToRefs } from 'pinia'
import ContractEvidence from '@/components/ContractEvidence.vue'
import DecisionResult from '@/components/DecisionResult.vue'
import TruthBanner from '@/components/TruthBanner.vue'
import { CONTRACT_LAB_SCENARIOS } from '@/domain/securityScenarios'
import { useEventsStore } from '@/stores/events'

const store = useEventsStore()
const { evaluating, errorMessage, lastContractEvent } = storeToRefs(store)
const selected = ref<(typeof CONTRACT_LAB_SCENARIOS)[number]['id']>('valid-v1')

async function run(): Promise<void> {
  await store.evaluateContractScenario(selected.value)
}
</script>

<template>
  <div class="stack">
    <header class="stack">
      <div class="cluster">
        <h1>Contract Drift Lab</h1>
        <TruthBanner />
      </div>
      <p>
        SYNTHETIC DEMO. QuayForge Orders versions are runtime enforcement artifacts, not wiki pages.
        A valid JWT or HMAC does not make an unsupported or drifted contract cross the boundary.
      </p>
    </header>

    <section class="card lab" aria-labelledby="contract-lab-heading">
      <h2 id="contract-lab-heading">Versioned contract scenarios</h2>
      <fieldset>
        <legend>Declared contract behavior</legend>
        <label v-for="scenario in CONTRACT_LAB_SCENARIOS" :key="scenario.id" class="lab__option">
          <input v-model="selected" type="radio" name="contract-scenario" :value="scenario.id" />
          <span>
            <strong>{{ $show(scenario.label) }}</strong>
            <span class="muted">{{ $show(scenario.summary) }}</span>
          </span>
        </label>
      </fieldset>
      <button type="button" class="action" :disabled="evaluating" @click="run">
        {{ $show(evaluating ? 'Evaluating contract…' : 'Evaluate contract') }}
      </button>
    </section>

    <p v-if="errorMessage" class="card fail" role="alert">{{ $show(errorMessage) }}</p>

    <template v-if="lastContractEvent && !errorMessage">
      <ContractEvidence :contract="lastContractEvent.contract" />
      <DecisionResult :event="lastContractEvent" show-detail-link compact />
    </template>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.lab,
.fail {
  display: grid;
  gap: $space-4;
}

fieldset {
  @include choice-fieldset;
}

.lab__option {
  @include choice-option;
}

.action {
  justify-self: start;
  @include button-primary;
}

.fail {
  background: $color-reject-soft;
  color: $color-reject;
}
</style>
