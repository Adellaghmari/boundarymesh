<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
import { CONTROL_CATALOG } from '@/domain/labels'
import type { ControlResult } from '@/domain/types'
import ControlStatusBadge from '@/components/ControlStatusBadge.vue'

const props = defineProps<{
  controls: ControlResult[]
}>()

function resultFor(kind: string): ControlResult | undefined {
  return props.controls.find((item) => item.kind === kind)
}
</script>

<template>
  <ol class="vector" aria-label="Control vector">
    <li v-for="entry in CONTROL_CATALOG" :key="entry.kind" class="vector__item">
      <div class="vector__head">
        <h3>{{ $show(entry.label) }}</h3>
        <ControlStatusBadge v-if="resultFor(entry.kind)" :status="resultFor(entry.kind)!.status" />
        <span v-else class="muted">No result</span>
      </div>
      <p class="muted">{{ $show(resultFor(entry.kind)?.safeExplanation ?? entry.summary) }}</p>
    </li>
  </ol>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.vector {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: $space-3;
}

.vector__item {
  padding: $space-3;
  border: 1px solid $color-border;
  border-radius: $radius-md;
  background: $color-surface-raised;
}

.vector__head {
  display: flex;
  justify-content: space-between;
  gap: $space-3;
  align-items: center;
  margin-bottom: $space-2;
}

@media (min-width: $bp-md) {
  .vector {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: $bp-lg) {
  .vector {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
