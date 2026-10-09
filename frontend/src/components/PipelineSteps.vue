<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
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
  'Record audit chain',
  'Sign exchange attestation',
]
</script>

<template>
  <ol class="pipeline" aria-label="Deterministic enforcement pipeline">
    <li v-for="(step, index) in steps" :key="step">
      <span class="pipeline__index">{{ $show(index + 1) }}</span>
      <span>{{ $show(step) }}</span>
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
  grid-template-columns: auto 1fr;
  gap: $space-3;
  align-items: center;
  padding: $space-3 $space-3;
  background: $color-surface-raised;
  border: 1px solid $color-border;
  border-radius: $radius-sm;
}

.pipeline__index {
  display: grid;
  width: 1.6rem;
  height: 1.6rem;
  place-items: center;
  border-radius: 999px;
  background: $color-accent-soft;
  color: $color-accent-bright;
  font-size: 0.75rem;
  font-weight: 700;
}

@media (min-width: $bp-md) {
  .pipeline {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
