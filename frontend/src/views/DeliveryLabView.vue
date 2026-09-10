<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { storeToRefs } from 'pinia'
import DecisionResult from '@/components/DecisionResult.vue'
import OutcomeBadge from '@/components/OutcomeBadge.vue'
import PayloadCompare from '@/components/PayloadCompare.vue'
import TruthBanner from '@/components/TruthBanner.vue'
import { toDecisionEvent } from '@/domain/mappers'
import { DELIVERY_LAB_SCENARIOS } from '@/domain/securityScenarios'
import type { DestinationSystem } from '@/domain/types'
import { DESTINATION_SYSTEMS } from '@/domain/types'
import { useDeliveryStore } from '@/stores/delivery'

const store = useDeliveryStore()
const { evaluating, errorMessage, lastDelivery } = storeToRefs(store)
const selected = ref<(typeof DELIVERY_LAB_SCENARIOS)[number]['id']>('transformed-delivery')

async function run(): Promise<void> {
  await store.evaluateDelivery(selected.value)
}

function destinationOf(value: string | undefined): DestinationSystem {
  if (value && (DESTINATION_SYSTEMS as readonly string[]).includes(value)) {
    return value as DestinationSystem
  }
  return 'NONE'
}

const decisionEvent = computed(() => {
  if (!lastDelivery.value) {
    return null
  }
  return toDecisionEvent({
    scenarioId: selected.value,
    scenarioLabel: selected.value,
    sourceSystem: 'QUAYFORGE',
    destinationSystem: destinationOf(lastDelivery.value.instruction?.destination),
    integrationId: 'delivery-lab',
    response: lastDelivery.value.decision,
    requestId: lastDelivery.value.requestId,
  })
})
</script>

<template>
  <div class="stack">
    <header class="stack">
      <div class="cluster">
        <h1>Delivery Lab</h1>
        <TruthBanner />
      </div>
      <p>
        SYNTHETIC DEMO. After the Boundary Enforcement Engine allows a crossing, a delivery
        instruction may enter the local Camel worker. REJECT and pre-delivery QUARANTINE never reach
        a connector. Pub/Sub remains planned.
      </p>
    </header>

    <section class="card lab" aria-labelledby="delivery-lab-heading">
      <h2 id="delivery-lab-heading">Outbound crossing scenarios</h2>
      <fieldset>
        <legend>Delivery inspection</legend>
        <label v-for="scenario in DELIVERY_LAB_SCENARIOS" :key="scenario.id" class="lab__option">
          <input v-model="selected" type="radio" name="delivery-scenario" :value="scenario.id" />
          <span>
            <strong>{{ scenario.label }}</strong>
            <span class="muted">{{ scenario.summary }}</span>
          </span>
        </label>
      </fieldset>
      <button type="button" class="action" :disabled="evaluating" @click="run">
        {{ evaluating ? 'Delivering…' : 'Run delivery' }}
      </button>
    </section>

    <p v-if="errorMessage" class="card fail" role="alert">{{ errorMessage }}</p>

    <template v-if="lastDelivery && !errorMessage">
      <p class="muted">{{ lastDelivery.banner }}</p>
      <section class="card verdict">
        <div class="cluster">
          <h2>Boundary decision</h2>
          <OutcomeBadge :outcome="lastDelivery.decision.outcome" />
        </div>
        <p>
          Delivery result
          <strong data-testid="delivery-kind">{{ lastDelivery.delivery?.kind ?? 'NOT_ATTEMPTED' }}</strong>
        </p>
      </section>

      <ol class="stages" aria-label="Delivery stages">
        <li v-for="stage in lastDelivery.stages" :key="stage">
          <strong>{{ stage }}</strong>
          <p v-if="stage === 'Boundary Decision'">
            {{ lastDelivery.decision.outcome }}
          </p>
          <p v-else-if="stage === 'Delivery Instruction'">
            <template v-if="lastDelivery.instruction">
              {{ lastDelivery.instruction.destination }} ·
              {{ lastDelivery.instruction.contractReference }}
            </template>
            <template v-else>None. REJECT or pre-delivery QUARANTINE never produces an instruction.</template>
          </p>
          <p v-else-if="stage === 'Credential / Consent'">
            Credential {{ lastDelivery.credential.reference ?? 'none' }} /
            {{ lastDelivery.credential.status }}. Consent {{ lastDelivery.consent.status }}.
          </p>
          <p v-else-if="stage === 'Camel Route'">
            {{
              lastDelivery.delivery?.kind === 'NOT_ATTEMPTED' || !lastDelivery.delivery?.routeId
                ? 'not routed'
                : lastDelivery.delivery.routeId
            }}
          </p>
          <p v-else-if="stage === 'Connector'">
            {{ lastDelivery.delivery?.connectorId ?? 'none' }} · provider called
            {{ lastDelivery.delivery?.providerCalled ? 'yes' : 'no' }}
          </p>
          <p v-else-if="stage === 'Resilience'">
            Circuit {{ lastDelivery.delivery?.circuitState ?? 'n/a' }} · attempts
            {{ lastDelivery.delivery?.attemptCount ?? 0 }} · HTTP
            {{ lastDelivery.delivery?.httpStatus ?? 'none' }}
          </p>
          <p v-else-if="stage === 'Audit / Attestation'">
            Chain {{ lastDelivery.evidence?.auditChainId ?? 'none' }} · attestation
            {{ lastDelivery.evidence?.attestationStatus ?? 'none' }}
          </p>
          <p v-else>
            {{ lastDelivery.delivery?.kind }} — {{ lastDelivery.delivery?.explanation }}
          </p>
        </li>
      </ol>

      <section v-if="lastDelivery.instruction" class="card stack" aria-label="Approved delivery body">
        <h2>Approved payload</h2>
        <p class="muted">
          Camel receives the transformed body. The original email must not appear after redaction.
        </p>
        <pre>{{ lastDelivery.instruction.payloadJson }}</pre>
      </section>

      <section v-if="lastDelivery.quarantine" class="card stack">
        <h2>Quarantine</h2>
        <p>
          {{ lastDelivery.quarantine.reason }} · {{ lastDelivery.quarantine.status }} ·
          eligible {{ lastDelivery.quarantine.eligible ? 'yes' : 'no' }}
        </p>
        <RouterLink :to="`/quarantine/${lastDelivery.quarantine.id}`">Open quarantine item</RouterLink>
      </section>

      <section v-if="lastDelivery.evidence" class="card stack">
        <h2>Audit and attestation</h2>
        <p>
          {{ lastDelivery.evidence.attestationStatus }} on chain
          {{ lastDelivery.evidence.auditChainId }}
        </p>
        <p>
          <RouterLink :to="`/audit/${lastDelivery.evidence.auditChainId}`">Open audit chain</RouterLink>
          ·
          <RouterLink :to="`/attestations/${lastDelivery.evidence.attestationId}`">
            Open attestation
          </RouterLink>
          ·
          <RouterLink to="/boundary-graph">Open boundary graph</RouterLink>
        </p>
      </section>

      <PayloadCompare
        :source="lastDelivery.decision.source_payload"
        :transformed="lastDelivery.decision.transformed_payload"
      />
      <DecisionResult v-if="decisionEvent" :event="decisionEvent" show-detail-link compact />
    </template>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.lab,
.fail,
.verdict,
.stages {
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

.stages {
  list-style: none;
  margin: 0;
  padding: 0;
}

.stages li {
  background: $color-surface;
  border: 1px solid $color-border;
  border-radius: $radius-md;
  padding: $space-4;
}

pre {
  overflow: auto;
  background: $color-surface-raised;
  padding: $space-3;
  border-radius: $radius-sm;
}
</style>
