<script setup lang="ts">
import { ref } from 'vue'
import {
  Activity,
  LayoutGrid,
  Menu,
  PackageCheck,
  ScrollText,
} from '@lucide/vue'
import BrandLockup from '@/components/shell/BrandLockup.vue'
import HeaderAuthControl from '@/components/shell/HeaderAuthControl.vue'
import HeaderMeshDecor from '@/components/shell/HeaderMeshDecor.vue'
import MobileNavigation from '@/components/shell/MobileNavigation.vue'
import NavDropdown from '@/components/shell/NavDropdown.vue'
import NavItem from '@/components/shell/NavItem.vue'
import SyntheticBadge from '@/components/shell/SyntheticBadge.vue'
import { EVIDENCE_NAV, LAB_NAV, OPERATIONS_NAV, PRIMARY_NAV, STATUS_NAV } from '@/config/navigation'
import { NAV_ICONS, navIcon } from '@/config/navIcons'
import { useActiveNav } from '@/composables/useActiveNav'

const { isActive, labsActive, operationsActive, evidenceActive } = useActiveNav()
const mobileOpen = ref(false)
</script>

<template>
  <header class="header">
    <HeaderMeshDecor />
    <div class="header__inner">
      <div class="header__brand">
        <BrandLockup />
        <SyntheticBadge />
      </div>
      <nav class="header__desktop" aria-label="Primary">
        <div class="header__primary">
          <NavItem
            v-for="item in PRIMARY_NAV"
            :key="item.name"
            :to="item.to"
            :label="item.label"
            :icon="navIcon(item.name)"
            :current="isActive(item.name)"
            stacked
          />
        </div>
        <span class="header__rule" aria-hidden="true" />
        <div class="header__secondary">
          <NavDropdown
            id="labs"
            label="Labs"
            :icon="LayoutGrid"
            :items="LAB_NAV"
            :current="labsActive"
            :icons="NAV_ICONS"
            :active-name="isActive"
          />
          <NavDropdown
            id="operations"
            label="Operations"
            :icon="PackageCheck"
            :items="OPERATIONS_NAV"
            :current="operationsActive"
            :icons="NAV_ICONS"
            :active-name="isActive"
          />
          <NavDropdown
            id="evidence"
            label="Evidence"
            :icon="ScrollText"
            :items="EVIDENCE_NAV"
            :current="evidenceActive"
            :icons="NAV_ICONS"
            :active-name="isActive"
          />
          <NavItem
            :to="STATUS_NAV.to"
            :label="STATUS_NAV.label"
            :icon="Activity"
            :current="isActive(STATUS_NAV.name)"
          />
        </div>
      </nav>
      <div class="header__utilities">
        <HeaderAuthControl />
        <button
          type="button"
          class="header__menu"
          aria-label="Open navigation"
          aria-controls="mobile-nav-title"
          :aria-expanded="mobileOpen"
          @click="mobileOpen = true"
        >
          <Menu :size="20" aria-hidden="true" />
        </button>
      </div>
    </div>
    <MobileNavigation :open="mobileOpen" @close="mobileOpen = false" />
  </header>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.header {
  position: sticky;
  top: 0;
  z-index: $z-header;
  color: $color-header-ink;
  background:
    linear-gradient(105deg, $color-header 0%, $color-header-mid 52%, $color-header-teal 100%);
  box-shadow: $shadow-header;
  border-bottom: 1px solid rgb(126 232 223 / 18%);
}

.header__inner {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, auto) minmax(0, 1fr) auto;
  gap: $space-4;
  align-items: center;
  padding: 1.05rem 1.35rem 1.15rem;
  min-height: 6.25rem;
}

.header__brand {
  display: flex;
  align-items: center;
  gap: $space-3;
}

.header__desktop {
  display: none;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  gap: 0.35rem;
  min-width: 0;
}

.header__primary,
.header__secondary {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 0.15rem;
}

.header__rule {
  display: none;
}

.header__utilities {
  display: flex;
  align-items: center;
  justify-self: end;
  gap: $space-3;
  flex-shrink: 0;
}

.header__menu {
  display: inline-flex;
  flex-shrink: 0;
  background: rgb(255 255 255 / 8%);
  border: 1px solid rgb(255 255 255 / 14%);
  color: $color-header-ink;
  border-radius: 0.6rem;
  padding: 0.4rem;
  cursor: pointer;
}

.header__menu:focus-visible {
  @include nav-focus-ring;
}

@media (max-width: $bp-lg) {
  .header__inner {
    grid-template-columns: minmax(0, 1fr) auto;
    min-height: 4.35rem;
    padding: 0.7rem 1rem;
  }

  .header__utilities :deep(.auth) {
    display: none;
  }
}

@media (min-width: $bp-lg) {
  .header__desktop {
    display: flex;
  }

  .header__menu {
    display: none;
  }
}

@media (min-width: $bp-xl) {
  .header__inner {
    padding-inline: 1.75rem;
  }

  .header__primary {
    gap: 0.25rem;
  }
}
</style>
