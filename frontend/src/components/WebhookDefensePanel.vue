<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
import { computed } from 'vue'
import ControlStatusBadge from '@/components/ControlStatusBadge.vue'
import type { ControlKind, ControlResult } from '@/domain/types'

const props = defineProps<{
  controls: ControlResult[]
  duplicateDelivery?: boolean
}>()

function result(kind: ControlKind): ControlResult | undefined {
  return props.controls.find((item) => item.kind === kind)
}

const authenticity = computed(() => result('AUTHENTICITY'))
const freshness = computed(() => result('FRESHNESS'))
const replay = computed(() => result('REPLAY'))
</script>

<template>
  <div class="split">
    <section v-if="authenticity" class="card-lite" aria-labelledby="authenticity-heading">
      <div class="cluster">
        <h3 id="authenticity-heading">Authenticity</h3>
        <ControlStatusBadge :status="authenticity.status" />
      </div>
      <p>{{ $show(authenticity.safeExplanation) }}</p>
    </section>
    <section v-if="freshness" class="card-lite" aria-labelledby="freshness-heading">
      <div class="cluster">
        <h3 id="freshness-heading">Freshness</h3>
        <ControlStatusBadge :status="freshness.status" />
      </div>
      <p>{{ $show(freshness.safeExplanation) }}</p>
    </section>
    <section v-if="replay" class="card-lite" aria-labelledby="replay-heading">
      <div class="cluster">
        <h3 id="replay-heading">Replay Defense</h3>
        <ControlStatusBadge :status="replay.status" />
      </div>
      <p>{{ $show(replay.safeExplanation) }}</p>
      <p v-if="duplicateDelivery" class="muted">
        Legitimate duplicate delivery. Replay attack: no. Side effect: not repeated.
      </p>
      <p v-else-if="replay.status === 'PASS'" class="muted">
        No prior nonce within the protected replay window.
      </p>
    </section>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.split {
  display: grid;
  gap: $space-3;
}

.card-lite {
  border: 1px solid $color-border;
  border-radius: $radius-md;
  padding: $space-3;
  background: $color-surface-raised;
  display: grid;
  gap: $space-2;
}

@media (min-width: $bp-md) {
  .split {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
