<script setup lang="ts">
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
            <strong>{{ scenario.label }}</strong>
            <span class="muted">{{ scenario.summary }}</span>
          </span>
        </label>
      </fieldset>
      <button type="button" class="action" :disabled="evaluating" @click="run">
        {{ evaluating ? 'Evaluating contract…' : 'Evaluate contract' }}
      </button>
    </section>

    <p v-if="errorMessage" class="card fail" role="alert">{{ errorMessage }}</p>

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
</style>
