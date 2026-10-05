<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
import { onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { storeToRefs } from 'pinia'
import TruthBanner from '@/components/TruthBanner.vue'
import { useEvidenceStore } from '@/stores/evidence'

const store = useEvidenceStore()
const { attestations, errorMessage, loading } = storeToRefs(store)

onMounted(() => {
  void store.loadAttestations()
})
</script>

<template>
  <div class="stack" data-inspect="attestations">
    <header class="stack">
      <div class="cluster">
        <h1>Exchange Attestations</h1>
        <TruthBanner />
      </div>
      <p>
        Cryptographic Exchange Attestation records what BoundaryMesh enforced for a terminal
        crossing. This screen currently reads local demo evidence signed by an ephemeral ECDSA P256
        key. The private cloud API has live evidence signed by Cloud KMS, but browser readback is not
        connected yet.
      </p>
    </header>
    <p v-if="errorMessage" class="card fail" role="alert">{{ $show(errorMessage) }}</p>
    <p v-if="attestations" class="muted">{{ $show(attestations.banner) }}</p>
    <p v-if="loading">Loading…</p>
    <p v-else-if="!attestations || attestations.items.length === 0">
      No attestations yet. Run a delivery lab crossing first.
    </p>
    <ul v-else class="list">
      <li v-for="item in attestations.items" :key="item.id">
        <RouterLink :to="`/attestations/${item.id}`">
          {{ $show(item.source) }} → {{ $show(item.destination) }} · {{ $show(item.boundaryOutcome) }} ·
          {{ $show(item.status) }}
        </RouterLink>
      </li>
    </ul>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.list {
  margin: 0;
  padding-left: $space-5;
  display: grid;
  gap: $space-2;
}

.fail {
  background: $color-reject-soft;
  color: $color-reject;
}
</style>
