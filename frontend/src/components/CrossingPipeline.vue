<script setup lang="ts">
import ControlStatusBadge from '@/components/ControlStatusBadge.vue'
import { CONTROL_CATALOG } from '@/domain/labels'
import type { ControlKind, ControlStatus, TraceStepView } from '@/domain/types'

const props = defineProps<{
  steps?: TraceStepView[]
  applied?: Partial<Record<ControlKind, ControlStatus>>
}>()

const STAGES: ControlKind[] = [
  'IDENTITY',
  'AUTHORIZATION',
  'AUTHENTICITY',
  'FRESHNESS',
  'REPLAY',
  'CONTRACT',
  'EGRESS',
]

function statusFor(kind: ControlKind): ControlStatus {
  const fromTrace = props.steps?.find((step) => step.kind === kind)
  if (fromTrace) {
    return fromTrace.status
  }
  return props.applied?.[kind] ?? 'NOT_APPLICABLE'
}

function noteFor(kind: ControlKind): string {
  return props.steps?.find((step) => step.kind === kind)?.explanation
    ?? CONTROL_CATALOG.find((item) => item.kind === kind)?.summary
    ?? ''
}

function labelFor(kind: ControlKind): string {
  if (kind === 'REPLAY') {
    return 'Replay Defense'
  }
  if (kind === 'EGRESS') {
    return 'Data Boundary'
  }
  return CONTROL_CATALOG.find((item) => item.kind === kind)?.label ?? kind
}
</script>

<template>
  <ol class="rail" aria-label="Boundary crossing inspection">
    <li v-for="(kind, index) in STAGES" :key="kind" :data-status="statusFor(kind)">
      <span class="rail__index">{{ String(index + 1).padStart(2, '0') }}</span>
      <div class="rail__body">
        <div class="cluster">
          <strong>{{ labelFor(kind) }}</strong>
          <ControlStatusBadge :status="statusFor(kind)" />
        </div>
        <p>{{ noteFor(kind) }}</p>
      </div>
    </li>
  </ol>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.rail {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0;
  border-left: 3px solid $color-header;
}

.rail li {
  display: grid;
  grid-template-columns: 3rem 1fr;
  gap: $space-3;
  padding: $space-3 $space-4;
  border-bottom: 1px solid $color-border;
  background: $color-surface;
}

.rail li[data-status='PASS'] {
  box-shadow: inset 4px 0 0 $color-allow;
}

.rail li[data-status='FAIL'] {
  box-shadow: inset 4px 0 0 $color-reject;
}

.rail li[data-status='UNAVAILABLE'] {
  box-shadow: inset 4px 0 0 $color-quarantine;
}

.rail__index {
  font-family: $font-mono;
  color: $color-header;
  font-weight: 700;
}

.rail__body {
  display: grid;
  gap: $space-1;
}

.rail__body p {
  margin: 0;
  color: $color-ink-muted;
  font-size: 0.92rem;
}
</style>
