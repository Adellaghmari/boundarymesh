<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
import { storeToRefs } from 'pinia'
import DecisionResult from '@/components/DecisionResult.vue'
import TruthBanner from '@/components/TruthBanner.vue'
import { EVALUATION_SCENARIOS, type ScenarioId } from '@/domain/scenarios'
import { useEventsStore } from '@/stores/events'

const store = useEventsStore()
const { selectedScenarioId, evaluating, errorMessage, retryable, lastEvent } = storeToRefs(store)

function selectScenario(id: ScenarioId): void {
  store.selectedScenarioId = id
}
</script>

<template>
  <section class="card lab" aria-labelledby="lab-heading" data-inspect="evaluation">
    <div class="cluster">
      <h2 id="lab-heading">Synthetic evaluation lab</h2>
      <TruthBanner />
    </div>
    <p>
      Select a Velora Interchange scenario. The Java Boundary Enforcement Engine decides the
      outcome. The console does not reproduce the algorithm.
    </p>

    <fieldset class="lab__scenarios" data-inspect="scenarios">
      <legend>Predefined crossings</legend>
      <label v-for="scenario in EVALUATION_SCENARIOS" :key="scenario.id" class="lab__option">
        <input
          type="radio"
          name="scenario"
          :value="scenario.id"
          :checked="selectedScenarioId === scenario.id"
          @change="selectScenario(scenario.id)"
        />
        <span>
          <strong>{{ $show(scenario.label) }}</strong>
          <span class="muted">{{ $show(scenario.summary) }}</span>
        </span>
      </label>
    </fieldset>

    <div class="cluster">
      <button type="button" class="action" :disabled="evaluating" @click="store.evaluateSelected()">
        {{ $show(evaluating ? 'Evaluating crossing…' : 'Evaluate crossing') }}
      </button>
    </div>

    <p v-if="errorMessage" class="lab__error" role="alert">
      {{ $show(errorMessage) }}
      <button v-if="retryable" type="button" class="linkish" @click="store.evaluateSelected()">
        Retry
      </button>
    </p>

    <DecisionResult v-if="lastEvent" :event="lastEvent" show-detail-link />
  </section>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.lab {
  display: grid;
  gap: $space-4;
}

.lab__scenarios {
  @include choice-fieldset;
}

.lab__option {
  @include choice-option;
}

.lab__error {
  padding: $space-3;
  border-radius: $radius-md;
  background: $color-reject-soft;
  color: $color-reject;
}

.action {
  @include button-primary;
}

.linkish {
  margin-left: $space-3;
  @include button-secondary;
  padding: 0.35rem 0.7rem;
}
</style>
