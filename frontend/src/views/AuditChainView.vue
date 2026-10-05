<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
import { onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { storeToRefs } from 'pinia'
import TruthBanner from '@/components/TruthBanner.vue'
import { useEvidenceStore } from '@/stores/evidence'

const store = useEvidenceStore()
const { chainList, errorMessage, loading } = storeToRefs(store)

onMounted(() => {
  void store.loadChains()
})
</script>

<template>
  <div class="stack" data-inspect="evidence">
    <header class="stack">
      <div class="cluster">
        <h1>Audit Chain</h1>
        <TruthBanner />
      </div>
      <p>
        Tamper evident hash chain of security and integration events. Continuity is verified by Java
        SHA256 over RFC 8785 canonical bytes. The local store is mutable. Verification detects
        retrospective modification. Not a blockchain.
      </p>
    </header>
    <p v-if="errorMessage" class="card fail" role="alert">{{ $show(errorMessage) }}</p>
    <p v-if="chainList" class="muted">{{ $show(chainList.banner) }}</p>
    <section class="card stack">
      <h2>Chains in this session</h2>
      <p v-if="loading">Loading…</p>
      <p v-else-if="!chainList || chainList.chains.length === 0">
        No audit entries yet. Run a delivery lab crossing first.
      </p>
      <ul v-else class="list">
        <li v-for="id in chainList.chains" :key="id">
          <RouterLink :to="`/audit/${id}`">{{ $show(id) }}</RouterLink>
        </li>
      </ul>
    </section>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.list {
  margin: 0;
  padding-left: $space-5;
}

.fail {
  background: $color-reject-soft;
  color: $color-reject;
}
</style>
