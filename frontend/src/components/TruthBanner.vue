<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
defineProps<{
  label?: string
}>()
</script>

<template>
  <span class="truth-banner">{{ $show(label ?? 'SYNTHETIC DEMO') }}</span>
</template>
