<script setup lang="ts">
defineProps<{
  label?: string
}>()
</script>

<template>
  <span class="truth-banner">{{ label ?? 'SYNTHETIC DEMO' }}</span>
</template>
