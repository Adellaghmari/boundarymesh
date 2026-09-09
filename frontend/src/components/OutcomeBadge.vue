<script setup lang="ts">
import { OUTCOME_COPY } from '@/domain/labels'
import type { BoundaryOutcome } from '@/domain/types'

defineProps<{
  outcome: BoundaryOutcome
}>()
</script>

<template>
  <span class="outcome" :data-outcome="outcome">
    <span class="outcome__label">{{ OUTCOME_COPY[outcome].label }}</span>
    <span class="visually-hidden"> — {{ OUTCOME_COPY[outcome].meaning }}</span>
  </span>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.outcome {
  display: inline-flex;
  align-items: center;
  padding: 0.2rem 0.55rem;
  border-radius: $radius-sm;
  border: 1px solid currentcolor;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.03em;
}

.outcome[data-outcome='ALLOW'] {
  color: $color-allow;
  background: $color-allow-soft;
}

.outcome[data-outcome='ALLOW_WITH_TRANSFORMATION'] {
  color: $color-transform;
  background: $color-transform-soft;
}

.outcome[data-outcome='QUARANTINE'] {
  color: $color-quarantine;
  background: $color-quarantine-soft;
}

.outcome[data-outcome='REJECT'] {
  color: $color-reject;
  background: $color-reject-soft;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
}
</style>
