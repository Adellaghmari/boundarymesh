<script setup lang="ts">
import { onMounted, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { storeToRefs } from 'pinia'
import TruthBanner from '@/components/TruthBanner.vue'
import { useEvidenceStore } from '@/stores/evidence'

const props = defineProps<{ id: string }>()
const store = useEvidenceStore()
const { attestation, attestationVerify, errorMessage, loading, attestationTamper } = storeToRefs(store)

onMounted(() => {
  void store.loadAttestation(props.id)
})

watch(
  () => props.id,
  (id) => {
    void store.loadAttestation(id)
  },
)

async function verify(): Promise<void> {
  await store.verifySignature(props.id)
}

async function tamperDemo(): Promise<void> {
  await store.runAttestationTamper()
}
</script>

<template>
  <div class="stack">
    <header class="stack">
      <div class="cluster">
        <h1>Exchange Attestation</h1>
        <TruthBanner />
      </div>
      <p v-if="attestation" class="claim">{{ attestation.claim }}</p>
    </header>
    <p v-if="errorMessage" class="card fail" role="alert">{{ errorMessage }}</p>
    <div class="cluster">
      <button type="button" class="action" :disabled="loading" @click="verify">Verify signature</button>
      <button type="button" class="action" :disabled="loading" @click="tamperDemo">
        Synthetic tamper demo
      </button>
    </div>
    <p v-if="attestationVerify" data-testid="attestation-verify">
      Java verification: <strong>{{ attestationVerify.status }}</strong> —
      {{ attestationVerify.explanation }}
    </p>
    <section v-if="attestationTamper" class="card stack" data-testid="attestation-tamper">
      <h2>Synthetic demo</h2>
      <p>{{ attestationTamper.banner }}</p>
      <p>Original copy {{ attestationTamper.original?.status ?? 'NONE' }}</p>
      <p>Tampered copy {{ attestationTamper.tampered?.status ?? 'NONE' }}</p>
    </section>
    <dl v-if="attestation" class="meta">
      <div>
        <dt>Transaction</dt>
        <dd>{{ attestation.transactionId }}</dd>
      </div>
      <div>
        <dt>Crossing</dt>
        <dd>{{ attestation.source }} → {{ attestation.destination }}</dd>
      </div>
      <div>
        <dt>Boundary outcome</dt>
        <dd>{{ attestation.boundaryOutcome }}</dd>
      </div>
      <div>
        <dt>Contract</dt>
        <dd>{{ attestation.contractId }} {{ attestation.contractVersion }}</dd>
      </div>
      <div>
        <dt>Data-boundary policy</dt>
        <dd>{{ attestation.egressPolicyId }} · {{ attestation.transformationCount }} transformations</dd>
      </div>
      <div>
        <dt>Credential</dt>
        <dd>{{ attestation.credentialReference }}</dd>
      </div>
      <div>
        <dt>Consent</dt>
        <dd>{{ attestation.consentResult }}</dd>
      </div>
      <div>
        <dt>Connector route</dt>
        <dd>{{ attestation.connectorRoute }}</dd>
      </div>
      <div>
        <dt>Delivery</dt>
        <dd>{{ attestation.deliveryResult }}</dd>
      </div>
      <div>
        <dt>Audit reference</dt>
        <dd>
          <RouterLink :to="`/audit/${attestation.auditChainId}`">
            {{ attestation.auditChainId }} @ {{ attestation.auditSequence }}
          </RouterLink>
          ({{ attestation.auditHeadShort }})
        </dd>
      </div>
      <div>
        <dt>Canonicalized / signed</dt>
        <dd>
          RFC 8785 bytes · {{ attestation.algorithm }} · key {{ attestation.keyId }} version
          {{ attestation.keyVersion }}
        </dd>
      </div>
      <div>
        <dt>Signature status</dt>
        <dd data-testid="attestation-status">
          {{ attestation.status }}
          {{ attestation.signaturePresent ? '(signature present)' : '(unsigned)' }}
        </dd>
      </div>
    </dl>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.action {
  background: $color-header;
  color: $color-header-ink;
  border: 0;
  border-radius: $radius-sm;
  padding: $space-2 $space-4;
}

.fail {
  background: $color-reject-soft;
  color: $color-reject;
}

.claim {
  font-weight: 600;
}

.meta {
  display: grid;
  gap: $space-3;
  background: $color-surface;
  border: 1px solid $color-border;
  border-radius: $radius-md;
  padding: $space-4;
}

dt {
  color: $color-ink-muted;
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

dd {
  margin: 0;
}
</style>
