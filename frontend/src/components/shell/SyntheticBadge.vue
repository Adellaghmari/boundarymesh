<script setup lang="ts">
</script>

<template>
  <span class="badge" data-testid="synthetic-badge">SYNTHETIC DEMO</span>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.badge {
  display: inline-flex;
  align-items: center;
  padding: 0.18rem 0.55rem;
  border: 1px solid rgb(255 255 255 / 22%);
  border-radius: 999px;
  color: $color-nav-muted;
  font-size: 0.65rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  white-space: nowrap;
}
</style>
