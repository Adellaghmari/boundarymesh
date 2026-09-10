<script setup lang="ts">
import { onMounted, watch } from 'vue'
import { storeToRefs } from 'pinia'
import TruthBanner from '@/components/TruthBanner.vue'
import { useEvidenceStore } from '@/stores/evidence'

const props = defineProps<{ integrationId: string }>()
const store = useEvidenceStore()
const { chain, chainVerify, errorMessage, loading, auditTamper } = storeToRefs(store)

onMounted(() => {
  void store.loadChain(props.integrationId)
})

watch(
  () => props.integrationId,
  (id) => {
    void store.loadChain(id)
  },
)

async function verify(): Promise<void> {
  await store.verifyChain(props.integrationId)
}

async function tamperDemo(): Promise<void> {
  await store.runAuditTamper()
}
</script>

<template>
  <div class="stack">
    <header class="stack">
      <div class="cluster">
        <h1>Audit chain {{ integrationId }}</h1>
        <TruthBanner />
      </div>
      <p>
        Latest 100 entries. VERIFY CHAIN recalculates SHA-256 hashes in Java. SYNTHETIC DEMO tamper
        uses an isolated copy and does not mutate this chain.
      </p>
    </header>
    <p v-if="errorMessage" class="card fail" role="alert">{{ errorMessage }}</p>
    <div class="cluster">
      <button type="button" class="action" :disabled="loading" @click="verify">Verify chain</button>
      <button type="button" class="action" :disabled="loading" @click="tamperDemo">
        Synthetic tamper demo
      </button>
    </div>
    <p v-if="chainVerify" data-testid="audit-verify">
      Java verification: <strong>{{ chainVerify.status }}</strong> — {{ chainVerify.explanation }}
    </p>
    <section v-if="auditTamper" class="card stack" data-testid="audit-tamper">
      <h2>Synthetic demo</h2>
      <p>{{ auditTamper.banner }}</p>
      <p>Original copy {{ auditTamper.original.status }}</p>
      <p>Tampered copy {{ auditTamper.tampered.status }}</p>
    </section>
    <p v-if="chain" class="muted">{{ chain.banner }} · continuity {{ chain.continuity }}</p>
    <table v-if="chain" class="entries">
      <caption class="visually-hidden">Audit entries</caption>
      <thead>
        <tr>
          <th>Seq</th>
          <th>Time</th>
          <th>Event</th>
          <th>References</th>
          <th>Previous</th>
          <th>Current</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="entry in chain.entries" :key="entry.sequence">
          <td>{{ entry.sequence }}</td>
          <td>{{ entry.occurredAt }}</td>
          <td>{{ entry.eventType }}</td>
          <td>
            <span v-for="(value, key) in entry.attributes" :key="key" class="attr">
              {{ key }}={{ value }}
            </span>
          </td>
          <td><code>{{ entry.previousHashShort }}</code></td>
          <td><code>{{ entry.currentHashShort }}</code></td>
        </tr>
      </tbody>
    </table>
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

.entries {
  width: 100%;
  border-collapse: collapse;
  background: $color-surface;
  font-size: 0.85rem;
}

th,
td {
  text-align: left;
  padding: $space-2;
  border-bottom: 1px solid $color-border;
  vertical-align: top;
}

.attr {
  display: block;
  color: $color-ink-muted;
}

code {
  font-family: $font-mono;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
}
</style>
