<script setup lang="ts">
import { CONTROL_STATUS_COPY } from '@/domain/labels'
import type { ControlStatus } from '@/domain/types'

defineProps<{
  status: ControlStatus
}>()
</script>

<template>
  <span class="status" :data-status="status">{{ CONTROL_STATUS_COPY[status] }}</span>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.status {
  display: inline-flex;
  padding: 0.15rem 0.45rem;
  border-radius: $radius-sm;
  border: 1px solid currentcolor;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.03em;
}

.status[data-status='PASS'] {
  color: $color-pass;
  background: $color-allow-soft;
}

.status[data-status='FAIL'] {
  color: $color-fail;
  background: $color-reject-soft;
}

.status[data-status='NOT_APPLICABLE'] {
  color: $color-na;
  background: $color-surface-raised;
}

.status[data-status='UNAVAILABLE'] {
  color: $color-unavailable;
  background: $color-transform-soft;
}
</style>
