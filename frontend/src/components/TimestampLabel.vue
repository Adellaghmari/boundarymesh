<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { formatTimestamp } from '@/domain/timestamp'

const props = defineProps<{
  value: string
}>()

const now = ref(new Date())
let timer: ReturnType<typeof setInterval> | undefined

function tick(): void {
  now.value = new Date()
}

watch(() => props.value, tick)

onMounted(() => {
  tick()
  if (import.meta.env.MODE === 'test') {
    return
  }
  timer = setInterval(tick, 30_000)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})

const view = computed(() => formatTimestamp(props.value, now.value))
</script>

<template>
  <time v-if="view" class="stamp" :datetime="view.instant" :title="view.exact">
    <span>{{ view.primary }}</span>
    <span v-if="view.relative" class="muted">{{ view.relative }}</span>
  </time>
  <span v-else>{{ value }}</span>
</template>

<style scoped lang="scss">
.stamp {
  display: inline-grid;
  gap: 0.1rem;
  line-height: 1.3;
  justify-items: start;
}

.stamp span:first-child {
  white-space: nowrap;
}
</style>
