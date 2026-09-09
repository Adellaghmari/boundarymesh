<script setup lang="ts">
import type { ContractView } from '@/domain/types'

defineProps<{
  contract?: ContractView
}>()
</script>

<template>
  <section v-if="contract" class="evidence" aria-labelledby="contract-evidence-heading">
    <h3 id="contract-evidence-heading">Contract evidence</h3>
    <dl>
      <div>
        <dt>Contract ID</dt>
        <dd>{{ contract.id ?? 'unresolved' }}</dd>
      </div>
      <div>
        <dt>Version</dt>
        <dd>{{ contract.version ?? 'unknown' }}</dd>
      </div>
      <div>
        <dt>Classification</dt>
        <dd>{{ contract.classification ?? 'NONE' }}</dd>
      </div>
    </dl>
    <p v-if="contract.truncatedCount > 0" class="muted">
      Violation list truncated. Additional count: {{ contract.truncatedCount }}
    </p>
    <ul v-if="contract.violations.length > 0">
      <li v-for="(item, index) in contract.violations" :key="`${item.path}-${index}`">
        <code>{{ item.path }}</code>
        <span>{{ item.keyword }}</span>
        <span v-if="item.expected || item.actual" class="muted">
          expected {{ item.expected || 'n/a' }} / actual {{ item.actual || 'n/a' }}
        </span>
      </li>
    </ul>
    <p v-else class="muted">No schema violations. Paths only — no payload values.</p>
  </section>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.evidence {
  display: grid;
  gap: $space-3;
}

dl {
  display: grid;
  gap: $space-3;
  margin: 0;
}

dd {
  margin: 0;
  font-family: $font-mono;
}

ul {
  margin: 0;
  padding-left: $space-5;
  display: grid;
  gap: $space-2;
}

code {
  font-family: $font-mono;
  margin-right: $space-2;
}

@media (min-width: $bp-md) {
  dl {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
