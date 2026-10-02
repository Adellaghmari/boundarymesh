<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
import { ref } from 'vue'
import { storeToRefs } from 'pinia'
import DecisionResult from '@/components/DecisionResult.vue'
import TruthBanner from '@/components/TruthBanner.vue'
import { WEBHOOK_LAB_SCENARIOS } from '@/domain/securityScenarios'
import { useEventsStore } from '@/stores/events'

const store = useEventsStore()
const { evaluating, webhookFailure, lastWebhookEvent } = storeToRefs(store)
const selected = ref<(typeof WEBHOOK_LAB_SCENARIOS)[number]['id']>('valid')

async function run(): Promise<void> {
  await store.evaluateWebhookScenario(selected.value)
}
</script>

<template>
  <div class="stack">
    <header class="stack">
      <div class="cluster">
        <h1>Webhook Defense Lab</h1>
        <TruthBanner />
      </div>
      <p>
        SYNTHETIC DEMO for QuayForge Orders. The browser never receives the HMAC secret. Signing
        happens on the local Java harness. Microsoft Graph subscriptions are not implemented.
      </p>
    </header>

    <section class="card lab" aria-labelledby="webhook-lab-heading">
      <h2 id="webhook-lab-heading">Signed webhook scenarios</h2>
      <fieldset>
        <legend>Predefined webhook crossings</legend>
        <label v-for="scenario in WEBHOOK_LAB_SCENARIOS" :key="scenario.id" class="lab__option">
          <input v-model="selected" type="radio" name="webhook-scenario" :value="scenario.id" />
          <span>
            <strong>{{ $show(scenario.label) }}</strong>
            <span class="muted">{{ $show(scenario.summary) }}</span>
          </span>
        </label>
      </fieldset>
      <button type="button" class="action" :disabled="evaluating" @click="run">
        {{ $show(evaluating ? 'Evaluating webhook…' : 'Evaluate webhook') }}
      </button>
    </section>

    <p v-if="webhookFailure" class="card fail" role="alert">{{ $show(webhookFailure.message) }}</p>

    <DecisionResult
      v-if="lastWebhookEvent && !webhookFailure"
      :event="lastWebhookEvent"
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
</style>
