<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import DecisionResult from '@/components/DecisionResult.vue'
import TruthBanner from '@/components/TruthBanner.vue'
import { CREDENTIAL_LAB_SCENARIOS } from '@/domain/securityScenarios'
import { useDeliveryStore } from '@/stores/delivery'

const store = useDeliveryStore()
const { evaluating, errorMessage, catalog, lastCredentialEvent } = storeToRefs(store)
const selected = ref<(typeof CREDENTIAL_LAB_SCENARIOS)[number]['id']>('transformed-delivery')

onMounted(async () => {
  await store.loadCatalog()
})

async function run(): Promise<void> {
  await store.evaluateCredential(selected.value)
}

async function rotate(): Promise<void> {
  await store.rotate()
}
</script>

<template>
  <div class="stack">
    <header class="stack">
      <div class="cluster">
        <h1>Credential &amp; Consent</h1>
        <TruthBanner label="LOCAL SECURITY MODEL / SYNTHETIC DEMO" />
      </div>
      <p>
        Application and domain code work with credential references and consent grants. Secret
        values are resolved only in the delivery worker, immediately before a connector call. Google
        Secret Manager remains PLANNED.
      </p>
    </header>

    <p v-if="catalog" class="muted">{{ catalog.banner }}</p>

    <section class="card stack" aria-labelledby="credential-catalog-heading">
      <div class="cluster">
        <h2 id="credential-catalog-heading">Credential references</h2>
        <button type="button" class="action" data-action="rotate" :disabled="evaluating" @click="rotate">
          Rotate CoveCRM
        </button>
      </div>
      <table v-if="catalog">
        <caption class="visually-hidden">Local credential versions</caption>
        <thead>
          <tr>
            <th scope="col">Reference</th>
            <th scope="col">Version</th>
            <th scope="col">Status</th>
            <th scope="col">Destination</th>
            <th scope="col">Rotated</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in catalog.credentials" :key="`${row.id}:${row.version}`">
            <td>{{ row.id }}</td>
            <td>{{ row.version }}</td>
            <td>{{ row.status }}</td>
            <td>{{ row.destination }}</td>
            <td>{{ row.rotatedAt }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="card stack" aria-labelledby="consent-catalog-heading">
      <h2 id="consent-catalog-heading">Consent grants</h2>
      <table v-if="catalog">
        <caption class="visually-hidden">Local consent permissions</caption>
        <thead>
          <tr>
            <th scope="col">Grant</th>
            <th scope="col">Destination</th>
            <th scope="col">Operation</th>
            <th scope="col">Status</th>
            <th scope="col">Kind</th>
            <th scope="col">Permissions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in catalog.consents" :key="row.id">
            <td>{{ row.id }}</td>
            <td>{{ row.destination }}</td>
            <td>{{ row.operation }}</td>
            <td>{{ row.status }}</td>
            <td>{{ row.kind }}</td>
            <td>{{ row.permissions.join(', ') }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="card lab" aria-labelledby="credential-lab-heading">
      <h2 id="credential-lab-heading">Boundary scenarios</h2>
      <fieldset>
        <legend>Credential and consent behavior</legend>
        <label v-for="scenario in CREDENTIAL_LAB_SCENARIOS" :key="scenario.id" class="lab__option">
          <input v-model="selected" type="radio" name="credential-scenario" :value="scenario.id" />
          <span>
            <strong>{{ scenario.label }}</strong>
            <span class="muted">{{ scenario.summary }}</span>
          </span>
        </label>
      </fieldset>
      <button type="button" class="action" data-action="evaluate" :disabled="evaluating" @click="run">
        {{ evaluating ? 'Evaluating…' : 'Evaluate credential boundary' }}
      </button>
    </section>

    <p v-if="errorMessage" class="card fail" role="alert">{{ errorMessage }}</p>
    <DecisionResult v-if="lastCredentialEvent && !errorMessage" :event="lastCredentialEvent" compact />
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

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  text-align: left;
  padding: $space-2 $space-3;
  border-bottom: 1px solid $color-border;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
}
</style>
