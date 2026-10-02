<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { LogIn, LogOut, UserRound } from '@lucide/vue'
import { useMicrosoftStore } from '@/stores/microsoft'

const store = useMicrosoftStore()
const open = ref(false)
const root = ref<HTMLElement | null>(null)

function close(): void {
  open.value = false
}

function onDocumentPointer(event: MouseEvent): void {
  if (open.value && root.value && !root.value.contains(event.target as Node)) {
    close()
  }
}

function onKey(event: KeyboardEvent): void {
  if (event.key === 'Escape') {
    close()
  }
}

onMounted(() => {
  document.addEventListener('mousedown', onDocumentPointer)
  void store.loadSession()
})

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', onDocumentPointer)
})
</script>

<template>
  <div ref="root" class="auth" @keydown="onKey">
    <p v-if="!store.session?.authenticated" class="auth__recruiter">
      <span data-testid="auth-mode">Recruiter mode</span>
      <a
        v-if="store.session?.entraEnabled"
        class="auth__signin"
        :href="store.session.loginUrl"
      >
        <LogIn :size="14" aria-hidden="true" />
        Admin sign in
      </a>
      <span v-else class="auth__hint">Sign in unavailable</span>
    </p>
    <div v-else>
      <button
        type="button"
        class="auth__operator"
        aria-haspopup="menu"
        :aria-expanded="open"
        data-testid="auth-operator"
        @click="open = !open"
      >
        <UserRound :size="14" aria-hidden="true" />
        Operator
      </button>
      <div v-if="open" class="auth__menu" role="menu">
        <p>Microsoft connected</p>
        <p class="muted">{{ $show(store.session.roleLabel) }}</p>
        <p class="muted">{{ $show(store.session.graphModeLabel) }}</p>
        <a role="menuitem" :href="store.session.logoutUrl">
          <LogOut :size="14" aria-hidden="true" />
          Sign out
        </a>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.auth {
  position: relative;
  z-index: 1;
  min-width: 0;
  text-align: right;
}

.auth__recruiter,
.auth__operator,
.auth__signin {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  color: $color-nav-muted;
  font-size: 0.72rem;
  font-weight: 650;
}

.auth__signin,
.auth__operator {
  color: $color-header-ink;
  text-decoration: none;
  background: rgb(255 255 255 / 8%);
  border: 1px solid rgb(255 255 255 / 12%);
  border-radius: 999px;
  padding: 0.35rem 0.7rem;
  cursor: pointer;
  font: inherit;
}

.auth__signin:focus-visible,
.auth__operator:focus-visible {
  @include nav-focus-ring;
}

.auth__hint {
  font-size: 0.68rem;
}

.auth__menu {
  position: absolute;
  right: 0;
  top: calc(100% + 0.5rem);
  min-width: 13rem;
  padding: $space-3;
  border-radius: 0.75rem;
  background: #0b2a38;
  border: 1px solid rgb(255 255 255 / 12%);
  box-shadow: $shadow-dropdown;
  color: $color-header-ink;
  text-align: left;
  display: grid;
  gap: 0.35rem;
  font-size: 0.8rem;
}

.auth__menu a {
  color: $color-header-ink;
  text-decoration: none;
  display: inline-flex;
  gap: 0.35rem;
  align-items: center;
}

.muted {
  color: $color-nav-muted;
}
</style>
