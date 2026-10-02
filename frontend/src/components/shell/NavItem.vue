<script setup lang="ts">
import type { Component } from 'vue'
import { RouterLink } from 'vue-router'

defineProps<{
  to: string
  label: string
  icon: Component
  current: boolean
  stacked?: boolean
}>()
</script>

<template>
  <RouterLink
    :to="to"
    class="nav-item"
    :class="{ 'is-active': current, 'is-stacked': stacked }"
    :aria-current="current ? 'page' : undefined"
  >
    <component :is="icon" :size="stacked ? 16 : 15" aria-hidden="true" />
    <span>{{ label }}</span>
  </RouterLink>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.nav-item {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.5rem 0.85rem;
  border-radius: $radius-nav;
  color: $color-nav-muted;
  text-decoration: none;
  font-size: 0.8rem;
  font-weight: 600;
  letter-spacing: 0.01em;
  line-height: 1;
  white-space: nowrap;
  border: 1px solid transparent;
  background: transparent;
  transition:
    background 160ms ease,
    color 160ms ease,
    box-shadow 160ms ease;
  @include reduced-motion;
}

.nav-item.is-stacked {
  flex-direction: row;
  gap: 0.35rem;
  padding: 0.45rem 0.65rem;
  min-width: 0;
}

.nav-item:hover {
  color: $color-header-ink;
  background: rgb(255 255 255 / 6%);
}

.nav-item:focus-visible {
  @include nav-focus-ring;
}

.nav-item.is-active {
  color: #ffffff;
  background: linear-gradient(180deg, #1bb3a9 0%, #0e8f88 100%);
  border-color: $color-nav-active-border;
  box-shadow: $shadow-nav-active;
}
</style>
