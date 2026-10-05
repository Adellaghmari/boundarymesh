<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
import { onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { storeToRefs } from 'pinia'
import TruthBanner from '@/components/TruthBanner.vue'
import { useDeliveryStore } from '@/stores/delivery'

const store = useDeliveryStore()
const { evaluating, errorMessage, quarantineList } = storeToRefs(store)

onMounted(async () => {
  await store.loadQuarantine()
})
</script>

<template>
  <div class="stack" data-inspect="delivery">
    <header class="stack">
      <div class="cluster">
        <h1>Quarantine</h1>
        <TruthBanner label="LOCAL / SYNTHETIC SESSION DATA" />
      </div>
      <p>
        Recoverable operational holds. Invalid JWT, invalid HMAC, and malicious replay are REJECT
        and never become quarantine items. This browser view currently reads local memory for this session.
        The cloud worker has recorded durable quarantine in Firestore, but browser readback is not
        connected yet.
      </p>
    </header>

    <p v-if="quarantineList" class="muted">{{ $show(quarantineList.banner) }}</p>
    <p v-if="errorMessage" class="card fail" role="alert">{{ $show(errorMessage) }}</p>

    <section class="card stack">
      <div class="cluster">
        <h2>Open and historical items</h2>
        <button type="button" class="action" :disabled="evaluating" @click="store.loadQuarantine()">
          Refresh
        </button>
      </div>
      <p v-if="quarantineList && quarantineList.items.length === 0" class="muted">
        No quarantine items in this local session.
      </p>
      <table v-else-if="quarantineList">
        <caption class="visually-hidden">Local quarantine items</caption>
        <thead>
          <tr>
            <th scope="col">Reason</th>
            <th scope="col">Status</th>
            <th scope="col">Destination</th>
            <th scope="col">Attempts</th>
            <th scope="col">Eligible</th>
            <th scope="col">Item</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in quarantineList.items" :key="item.id">
            <td>{{ $show(item.reason) }}</td>
            <td>{{ $show(item.status) }}</td>
            <td>{{ $show(item.destination) }}</td>
            <td>{{ $show(item.attempts) }} / {{ $show(item.maxAttempts) }}</td>
            <td>{{ $show(item.eligible ? 'yes' : 'no') }}</td>
            <td>
              <RouterLink :to="`/quarantine/${item.id}`">Inspect</RouterLink>
            </td>
          </tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.fail {
  background: $color-reject-soft;
  color: $color-reject;
}

.action {
  background: $color-header;
  color: $color-header-ink;
  border: 0;
  border-radius: $radius-sm;
  padding: $space-2 $space-4;
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
