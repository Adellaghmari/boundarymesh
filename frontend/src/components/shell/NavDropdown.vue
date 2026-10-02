<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { Component } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { ChevronDown } from '@lucide/vue'
import type { NavDestination } from '@/config/navigation'

const props = defineProps<{
  id: string
  label: string
  icon: Component
  items: readonly NavDestination[]
  current: boolean
  icons: Record<string, Component>
  activeName: (name: string) => boolean
}>()

const open = ref(false)
const root = ref<HTMLElement | null>(null)
const button = ref<HTMLButtonElement | null>(null)
const menuId = `nav-menu-${props.id}`
const route = useRoute()

function close(): void {
  open.value = false
}

function toggle(): void {
  open.value = !open.value
}

function onDocumentPointer(event: MouseEvent): void {
  if (!open.value || !root.value) {
    return
  }
  if (!root.value.contains(event.target as Node)) {
    close()
  }
}

function onKey(event: KeyboardEvent): void {
  if (!open.value) {
    if ((event.key === 'ArrowDown' || event.key === 'Enter' || event.key === ' ') && event.target === button.value) {
      event.preventDefault()
      open.value = true
    }
    return
  }
  const items = Array.from(root.value?.querySelectorAll<HTMLElement>('[role="menuitem"]') ?? [])
  const index = items.indexOf(document.activeElement as HTMLElement)
  if (event.key === 'Escape') {
    event.preventDefault()
    close()
    button.value?.focus()
    return
  }
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    const next = items[(index + 1 + items.length) % items.length]
    next?.focus()
  }
  if (event.key === 'ArrowUp') {
    event.preventDefault()
    const prev = items[(index - 1 + items.length) % items.length]
    prev?.focus()
  }
  if (event.key === 'Home') {
    event.preventDefault()
    items[0]?.focus()
  }
  if (event.key === 'End') {
    event.preventDefault()
    items[items.length - 1]?.focus()
  }
}

watch(
  () => route.fullPath,
  () => {
    close()
  },
)

watch(open, async (value) => {
  if (value) {
    await nextTick()
    root.value?.querySelector<HTMLElement>('[role="menuitem"]')?.focus()
  }
})

onMounted(() => {
  document.addEventListener('mousedown', onDocumentPointer)
})

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', onDocumentPointer)
})
</script>

<template>
  <div ref="root" class="dropdown" @keydown="onKey">
    <button
      ref="button"
      type="button"
      class="dropdown__button"
      :class="{ 'is-current': current, 'is-open': open }"
      :aria-expanded="open"
      :aria-controls="menuId"
      aria-haspopup="menu"
      @click="toggle"
    >
      <component :is="icon" :size="15" aria-hidden="true" />
      <span>{{ $show(label) }}</span>
      <ChevronDown :size="14" aria-hidden="true" />
    </button>
    <div v-if="open" :id="menuId" class="dropdown__menu" role="menu">
      <RouterLink
        v-for="item in items"
        :key="item.name"
        :to="item.to"
        class="dropdown__item"
        role="menuitem"
        :aria-current="activeName(item.name) ? 'page' : undefined"
      >
        <component :is="icons[item.name]" :size="16" aria-hidden="true" />
        <span>
          <span class="dropdown__title">{{ $show(item.label) }}</span>
          <span v-if="item.description" class="dropdown__hint">{{ $show(item.description) }}</span>
        </span>
      </RouterLink>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.dropdown {
  position: relative;
}

.dropdown__button {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.5rem 0.85rem;
  border: 1px solid transparent;
  border-radius: $radius-nav;
  background: transparent;
  color: $color-nav-muted;
  font: inherit;
  font-size: 0.8rem;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
  cursor: pointer;
  transition:
    background 160ms ease,
    color 160ms ease;
  @include reduced-motion;
}

.dropdown__button:hover,
.dropdown__button.is-open {
  color: $color-header-ink;
  background: rgb(255 255 255 / 7%);
}

.dropdown__button.is-current {
  color: #ffffff;
  background: linear-gradient(180deg, #1bb3a9 0%, #0e8f88 100%);
  border-color: $color-nav-active-border;
  box-shadow: $shadow-nav-active;
}

.dropdown__button:focus-visible {
  @include nav-focus-ring;
}

.dropdown__menu {
  position: absolute;
  top: calc(100% + 0.55rem);
  right: 0;
  z-index: $z-dropdown;
  min-width: 16.5rem;
  padding: $space-2;
  border-radius: 0.85rem;
  border: 1px solid rgb(255 255 255 / 12%);
  background: #0b2a38;
  box-shadow: $shadow-dropdown;
}

.dropdown__item {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: $space-3;
  align-items: start;
  padding: 0.6rem 0.7rem;
  border-radius: 0.65rem;
  color: $color-header-ink;
  text-decoration: none;
}

.dropdown__item:hover,
.dropdown__item:focus-visible {
  background: rgb(255 255 255 / 8%);
}

.dropdown__item:focus-visible {
  @include nav-focus-ring;
}

.dropdown__title {
  display: block;
  font-size: 0.85rem;
  font-weight: 650;
}

.dropdown__hint {
  display: block;
  color: $color-nav-muted;
  font-size: 0.72rem;
  font-weight: 500;
}
</style>
