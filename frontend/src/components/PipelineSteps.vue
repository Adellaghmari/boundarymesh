<script setup lang="ts">
const steps = [
  'Authenticate',
  'Authorize',
  'Verify authenticity',
  'Check replay / freshness',
  'Validate contract',
  'Govern data',
  'Enforce boundary',
  'Resolve credential / consent',
  'Deliver or quarantine',
]
</script>

<template>
  <ol class="pipeline" aria-label="Deterministic enforcement pipeline">
    <li v-for="(step, index) in steps" :key="step">
      <span class="pipeline__index">{{ index + 1 }}</span>
      <span>{{ step }}</span>
    </li>
  </ol>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.pipeline {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: $space-2;
}

.pipeline li {
  display: grid;
  grid-template-columns: 1.75rem 1fr;
  gap: $space-3;
  align-items: center;
  padding: $space-2 $space-3;
  background: $color-surface-raised;
  border: 1px solid $color-border;
  border-radius: $radius-sm;
}

.pipeline__index {
  font-weight: 700;
  color: $color-accent;
}

@media (min-width: $bp-md) {
  .pipeline {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
