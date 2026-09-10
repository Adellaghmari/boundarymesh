<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { X } from '@lucide/vue'
import HeaderAuthControl from '@/components/shell/HeaderAuthControl.vue'
import { MOBILE_GROUPS } from '@/config/navigation'
import { useActiveNav } from '@/composables/useActiveNav'
import { navIcon } from '@/config/navIcons'

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const { isActive } = useActiveNav()
const closeButton = ref<HTMLButtonElement | null>(null)

function onKey(event: KeyboardEvent): void {
  if (event.key === 'Escape' && props.open) {
    emit('close')
  }
}

watch(
  () => props.open,
  async (value) => {
    document.body.style.overflow = value ? 'hidden' : ''
    if (value) {
      document.addEventListener('keydown', onKey)
      await nextTick()
      closeButton.value?.focus()
    } else {
      document.removeEventListener('keydown', onKey)
    }
  },
)

onBeforeUnmount(() => {
  document.body.style.overflow = ''
  document.removeEventListener('keydown', onKey)
})
</script>

<template>
  <div v-if="open" class="drawer">
    <div class="drawer__backdrop" @click="emit('close')" />
    <div
      class="drawer__panel"
      role="dialog"
      aria-modal="true"
      aria-labelledby="mobile-nav-title"
    >
      <div class="drawer__top">
        <h2 id="mobile-nav-title">Navigate</h2>
        <button
          ref="closeButton"
          type="button"
          class="drawer__close"
          aria-label="Close navigation"
          @click="emit('close')"
        >
          <X :size="18" aria-hidden="true" />
        </button>
      </div>
      <nav class="drawer__groups" aria-label="Mobile">
        <section v-for="group in MOBILE_GROUPS" :key="group.id">
          <h3>{{ group.label }}</h3>
          <RouterLink
            v-for="item in group.items"
            :key="item.name"
            :to="item.to"
            class="drawer__link"
            :aria-current="isActive(item.name) ? 'page' : undefined"
            @click="emit('close')"
          >
            <component :is="navIcon(item.name)" :size="16" aria-hidden="true" />
            {{ item.label }}
          </RouterLink>
        </section>
        <section class="drawer__identity">
          <h3>Identity</h3>
          <HeaderAuthControl />
        </section>
      </nav>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.drawer__backdrop {
  position: fixed;
  inset: 0;
  background: rgb(7 21 39 / 55%);
  z-index: $z-drawer;
}

.drawer__panel {
  position: fixed;
  top: 0;
  right: 0;
  z-index: $z-drawer;
  width: min(22rem, 92vw);
  height: 100vh;
  overflow: auto;
  background: #071b2b;
  color: $color-header-ink;
  padding: $space-5;
  box-shadow: $shadow-dropdown;
}

.drawer__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $space-4;
}

.drawer__close {
  background: transparent;
  border: 0;
  color: $color-header-ink;
  cursor: pointer;
}

.drawer__close:focus-visible {
  @include nav-focus-ring;
}

.drawer__groups {
  display: grid;
  gap: $space-4;
}

.drawer__groups h3 {
  color: $color-nav-muted;
  font-size: 0.72rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  margin-bottom: $space-2;
}

.drawer__link {
  display: flex;
  gap: $space-3;
  align-items: center;
  color: $color-header-ink;
  text-decoration: none;
  padding: 0.55rem 0.4rem;
  border-radius: $radius-md;
}

.drawer__link[aria-current='page'] {
  background: rgb(20 163 154 / 22%);
}

.drawer__link:focus-visible {
  @include nav-focus-ring;
}

.drawer__identity :deep(.auth) {
  text-align: left;
}
</style>
