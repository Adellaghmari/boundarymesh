<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
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
      <p v-if="attestation" class="claim">{{ $show(attestation.claim) }}</p>
    </header>
    <p v-if="errorMessage" class="card fail" role="alert">{{ $show(errorMessage) }}</p>
    <div class="cluster">
      <button type="button" class="action" :disabled="loading" @click="verify">Verify signature</button>
      <button type="button" class="action" :disabled="loading" @click="tamperDemo">
        Synthetic tamper demo
      </button>
    </div>
    <p v-if="attestationVerify" data-testid="attestation-verify">
      Java verification: <strong>{{ $show(attestationVerify.status) }}</strong>.
      {{ $show(attestationVerify.explanation) }}
    </p>
    <section v-if="attestationTamper" class="card stack" data-testid="attestation-tamper">
      <h2>Synthetic demo</h2>
      <p>{{ $show(attestationTamper.banner) }}</p>
      <p>Original copy {{ $show(attestationTamper.original?.status ?? 'NONE') }}</p>
      <p>Tampered copy {{ $show(attestationTamper.tampered?.status ?? 'NONE') }}</p>
    </section>
    <dl v-if="attestation" class="meta">
      <div>
        <dt>Transaction</dt>
        <dd>{{ $show(attestation.transactionId) }}</dd>
      </div>
      <div>
        <dt>Crossing</dt>
        <dd>{{ $show(attestation.source) }} → {{ $show(attestation.destination) }}</dd>
      </div>
      <div>
        <dt>Boundary outcome</dt>
        <dd>{{ $show(attestation.boundaryOutcome) }}</dd>
      </div>
      <div>
        <dt>Contract</dt>
        <dd>{{ $show(attestation.contractId) }} {{ $show(attestation.contractVersion) }}</dd>
      </div>
      <div>
        <dt>Data boundary policy</dt>
        <dd>{{ $show(attestation.egressPolicyId) }} · {{ $show(attestation.transformationCount) }} transformations</dd>
      </div>
      <div>
        <dt>Credential</dt>
        <dd>{{ $show(attestation.credentialReference) }}</dd>
      </div>
      <div>
        <dt>Consent</dt>
        <dd>{{ $show(attestation.consentResult) }}</dd>
      </div>
      <div>
        <dt>Connector route</dt>
        <dd>{{ $show(attestation.connectorRoute) }}</dd>
      </div>
      <div>
        <dt>Delivery</dt>
        <dd>{{ $show(attestation.deliveryResult) }}</dd>
      </div>
      <div>
        <dt>Audit reference</dt>
        <dd>
          <RouterLink :to="`/audit/${attestation.auditChainId}`">
            {{ $show(attestation.auditChainId) }} @ {{ $show(attestation.auditSequence) }}
          </RouterLink>
          ({{ $show(attestation.auditHeadShort) }})
        </dd>
      </div>
      <div>
        <dt>Canonicalized / signed</dt>
        <dd>
          RFC 8785 bytes · {{ $show(attestation.algorithm) }} · key {{ $show(attestation.keyId) }} version
          {{ $show(attestation.keyVersion) }}
        </dd>
      </div>
      <div>
        <dt>Signature status</dt>
        <dd data-testid="attestation-status">
          {{ $show(attestation.status) }}
          {{ $show(attestation.signaturePresent ? '(signature present)' : '(unsigned)') }}
        </dd>
      </div>
    </dl>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.action {
  @include button-primary;
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
