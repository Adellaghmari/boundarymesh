<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
import { computed, ref } from 'vue'
import { storeToRefs } from 'pinia'
import DecisionResult from '@/components/DecisionResult.vue'
import TruthBanner from '@/components/TruthBanner.vue'
import { classifyIdentityTest, type IdentityObservation } from '@/domain/identityTestVerdict'
import { IDENTITY_LAB_SCENARIOS } from '@/domain/securityScenarios'
import { useEventsStore } from '@/stores/events'

const store = useEventsStore()
const { evaluating, identityFailure, lastIdentityEvent } = storeToRefs(store)
const selected = ref<(typeof IDENTITY_LAB_SCENARIOS)[number]['id']>('VALID_MACHINE')
const judgedScenario = ref(selected.value)
const ran = ref(false)

const observation = computed<IdentityObservation | null>(() => {
  if (!ran.value || evaluating.value) {
    return null
  }
  if (identityFailure.value) {
    return { type: 'http', status: identityFailure.value.status ?? 0 }
  }
  if (lastIdentityEvent.value) {
    return {
      type: 'decision',
      status: lastIdentityEvent.value.httpStatus ?? 200,
      outcome: lastIdentityEvent.value.outcome,
    }
  }
  return null
})

const verdict = computed(() =>
  observation.value ? classifyIdentityTest(judgedScenario.value, observation.value) : null,
)

const showDecision = computed(() =>
  Boolean(lastIdentityEvent.value && !identityFailure.value && verdict.value?.result === 'PASS' && verdict.value.httpStatus !== 401),
)

async function run(): Promise<void> {
  judgedScenario.value = selected.value
  ran.value = true
  await store.evaluateIdentityScenario(judgedScenario.value)
}
</script>

<template>
  <div class="stack">
    <header class="stack">
      <div class="cluster">
        <h1>Identity Federation Lab</h1>
        <TruthBanner />
      </div>
      <p>
        SYNTHETIC DEMO. Java / Spring Security is authoritative. This console does not validate
        JWTs. Microsoft Entra ID is not integrated.
      </p>
    </header>

    <section class="card lab" aria-labelledby="identity-lab-heading">
      <h2 id="identity-lab-heading">Machine identity scenarios</h2>
      <fieldset>
        <legend>Predefined identity crossings</legend>
        <label v-for="scenario in IDENTITY_LAB_SCENARIOS" :key="scenario.id" class="lab__option">
          <input v-model="selected" type="radio" name="identity-scenario" :value="scenario.id" />
          <span>
            <strong>{{ $show(scenario.label) }}</strong>
            <span class="muted">{{ $show(scenario.summary) }}</span>
          </span>
        </label>
      </fieldset>
      <button type="button" class="action" :disabled="evaluating" @click="run">
        {{ $show(evaluating ? 'Evaluating identity…' : 'Evaluate identity') }}
      </button>
    </section>

    <section
      v-if="verdict"
      class="card verdict"
      :class="verdict.result === 'PASS' ? 'verdict--pass' : 'verdict--fail'"
      data-testid="identity-test"
      aria-labelledby="identity-test-heading"
    >
      <h2 id="identity-test-heading" class="eyebrow">SECURITY CHECK</h2>
      <p class="status">{{ verdict.result === 'PASS' ? 'PASSED' : 'FAILED' }}</p>
      <dl class="security-result">
        <div>
          <dt>Observed result</dt>
          <dd>{{ verdict.observedResult }}</dd>
        </div>
        <div>
          <dt>Reason</dt>
          <dd>{{ verdict.reason }}</dd>
        </div>
        <div>
          <dt>Boundary action</dt>
          <dd>{{ verdict.boundaryAction }}</dd>
        </div>
      </dl>
    </section>

    <DecisionResult
      v-if="showDecision && lastIdentityEvent"
      :event="lastIdentityEvent"
      show-detail-link
    />
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.lab,
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

.verdict--fail {
  background: $color-reject-soft;
  color: $color-reject;
}

.verdict--pass {
  background: $color-allow-soft;
  color: $color-allow;
}

.eyebrow {
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.status {
  font-weight: 700;
  letter-spacing: 0.04em;
}

.security-result {
  display: grid;
  gap: $space-3;
  margin: 0;
}

.security-result div {
  display: grid;
  gap: $space-1;
}

.security-result dt {
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.security-result dd {
  margin: 0;
}
</style>
