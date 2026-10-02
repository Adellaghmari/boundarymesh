<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
import { onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { storeToRefs } from 'pinia'
import TruthBanner from '@/components/TruthBanner.vue'
import { useDeliveryStore } from '@/stores/delivery'

const props = defineProps<{
  id: string
}>()

const store = useDeliveryStore()
const { evaluating, errorMessage, selectedQuarantine, lastReprocess } = storeToRefs(store)

onMounted(async () => {
  await store.loadQuarantineItem(props.id)
})

async function reprocess(): Promise<void> {
  await store.reprocess(props.id)
}
</script>

<template>
  <div class="stack">
    <header class="stack">
      <div class="cluster">
        <h1>Quarantine item</h1>
        <TruthBanner label="LOCAL / SYNTHETIC SESSION DATA" />
      </div>
      <p>
        The public recruiter view is view only. Reprocess mints a local admin JWT with
        <code>boundary.admin</code> on the Java issuer. The token is not embedded in Vue.
      </p>
      <RouterLink to="/quarantine">Back to quarantine</RouterLink>
    </header>

    <p v-if="errorMessage" class="card fail" role="alert">{{ $show(errorMessage) }}</p>

    <section v-if="selectedQuarantine" class="card stack" aria-label="Quarantine metadata">
      <h2>{{ $show(selectedQuarantine.reason) }}</h2>
      <dl class="meta">
        <div>
          <dt>Status</dt>
          <dd>{{ $show(selectedQuarantine.status) }}</dd>
        </div>
        <div>
          <dt>Destination</dt>
          <dd>{{ $show(selectedQuarantine.destination) }}</dd>
        </div>
        <div>
          <dt>Transaction</dt>
          <dd>{{ $show(selectedQuarantine.transactionId) }}</dd>
        </div>
        <div>
          <dt>Attempts</dt>
          <dd>{{ $show(selectedQuarantine.attempts) }} / {{ $show(selectedQuarantine.maxAttempts) }}</dd>
        </div>
        <div>
          <dt>Recovery eligible</dt>
          <dd>{{ $show(selectedQuarantine.eligible ? 'yes' : 'no') }}</dd>
        </div>
        <div>
          <dt>Credential reference</dt>
          <dd>{{ $show(selectedQuarantine.credentialReference ?? 'none') }}</dd>
        </div>
        <div>
          <dt>Connector state</dt>
          <dd>{{ $show(selectedQuarantine.connectorState ?? 'n/a') }}</dd>
        </div>
      </dl>
      <button
        v-if="selectedQuarantine.eligible && selectedQuarantine.status !== 'TERMINAL'"
        type="button"
        class="action"
        :disabled="evaluating"
        @click="reprocess"
      >
        {{ $show(evaluating ? 'Reprocessing…' : 'Reprocess as operator') }}
      </button>
      <p v-else class="muted">This item is not eligible for another recovery cycle.</p>
    </section>

    <section v-if="lastReprocess && !errorMessage" class="card stack" aria-label="Recovery result">
      <h2>Recovery result</h2>
      <p>
        Delivery {{ $show(lastReprocess.delivery?.kind) }} · quarantine
        {{ $show(lastReprocess.quarantine?.status ?? 'resolved or replaced') }}
      </p>
      <p>{{ $show(lastReprocess.delivery?.explanation) }}</p>
    </section>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.fail {
  background: $color-reject-soft;
  color: $color-reject;
}

.meta {
  display: grid;
  gap: $space-3;
  margin: 0;
}

.meta div {
  display: grid;
  gap: $space-1;
}

dt {
  color: $color-ink-muted;
  font-size: 0.85rem;
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
</style>
