<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
import { ref } from 'vue'
import { storeToRefs } from 'pinia'
import DecisionResult from '@/components/DecisionResult.vue'
import TruthBanner from '@/components/TruthBanner.vue'
import { IDENTITY_LAB_SCENARIOS } from '@/domain/securityScenarios'
import { useEventsStore } from '@/stores/events'

const store = useEventsStore()
const { evaluating, identityFailure, lastIdentityEvent } = storeToRefs(store)
const selected = ref<(typeof IDENTITY_LAB_SCENARIOS)[number]['id']>('VALID_MACHINE')

async function run(): Promise<void> {
  await store.evaluateIdentityScenario(selected.value)
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
      v-if="identityFailure?.kind === 'unauthorized'"
      class="card fail"
      data-testid="authn-fail"
      aria-labelledby="authn-fail-heading"
    >
      <h2 id="authn-fail-heading">Authentication</h2>
      <p class="status">FAIL</p>
      <p>Authentication could not be established.</p>
      <p class="muted">HTTP {{ $show(identityFailure.status ?? 401) }}. No domain crossing was evaluated.</p>
    </section>

    <section
      v-else-if="identityFailure?.kind === 'forbidden'"
      class="card fail"
      data-testid="authz-http-fail"
      aria-labelledby="authz-http-fail-heading"
    >
      <h2 id="authz-http-fail-heading">Authorization</h2>
      <p class="status">FAIL</p>
      <p>{{ $show(identityFailure.message) }}</p>
      <p class="muted">HTTP {{ $show(identityFailure.status ?? 403) }}.</p>
    </section>

    <p v-else-if="identityFailure" class="card fail" role="alert">{{ $show(identityFailure.message) }}</p>

    <DecisionResult
      v-if="lastIdentityEvent && !identityFailure"
      :event="lastIdentityEvent"
      show-detail-link
    />
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

.status {
  font-weight: 700;
  letter-spacing: 0.04em;
}
</style>
