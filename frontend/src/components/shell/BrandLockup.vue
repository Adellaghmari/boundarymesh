<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <RouterLink to="/" class="brand" aria-label="BoundaryMesh overview">
    <span class="brand__mark" aria-hidden="true">
      <svg viewBox="0 0 32 36" fill="none">
        <path
          class="brand__halo"
          d="M16 2.5c6.2 3.2 10.7 4.4 13.2 4.8v12.2c0 7.4-5.2 12.8-13.2 16.1C7.9 30.3 2.8 24.9 2.8 17.5V7.3C5.4 6.9 9.8 5.7 16 2.5Z"
        />
        <path
          class="brand__shield"
          d="M16 4.2c5.4 2.8 9.3 3.8 11.4 4.1v11.2c0 6.3-4.4 10.8-11.4 13.6C8.9 30.3 4.6 25.8 4.6 19.5V8.3C6.8 8 10.6 7 16 4.2Z"
        />
        <path
          class="brand__stroke"
          d="M16 7.4c3.6 1.8 6.2 2.5 7.6 2.7v8.2c0 4.2-2.9 7.3-7.6 9.3-4.7-2-7.6-5.1-7.6-9.3V10.1c1.4-.2 4-1 7.6-2.7Z"
        />
        <path class="brand__accent" d="M16 12v12M11.2 16.4h9.6" />
      </svg>
    </span>
    <span class="brand__text">
      <span class="brand__name">BoundaryMesh</span>
      <span class="brand__tagline">Secure every system crossing.</span>
    </span>
  </RouterLink>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.brand {
  display: flex;
  align-items: center;
  gap: $space-3;
  color: $color-header-ink;
  text-decoration: none;
  position: relative;
  z-index: 1;
}

.brand:focus-visible {
  @include nav-focus-ring;
  border-radius: $radius-md;
}

.brand__mark {
  display: grid;
  place-items: center;
  width: 2.5rem;
  height: 2.75rem;
  filter: drop-shadow(0 0 10px rgb(20 163 154 / 45%));
}

.brand__mark svg {
  width: 2.35rem;
  height: 2.6rem;
}

.brand__halo {
  fill: rgb(20 163 154 / 18%);
}

.brand__shield {
  fill: $color-header;
  stroke: $color-nav-active;
  stroke-width: 1.2;
}

.brand__stroke {
  fill: none;
  stroke: #f7fbfb;
  stroke-width: 1.35;
}

.brand__accent {
  stroke: $color-nav-active;
  stroke-width: 1.6;
  stroke-linecap: round;
}

.brand__text {
  display: grid;
  gap: 0.1rem;
}

.brand__name {
  font-weight: 700;
  letter-spacing: 0.01em;
  line-height: 1.15;
  font-size: 1.05rem;
}

.brand__tagline {
  color: $color-nav-muted;
  font-size: 0.75rem;
  line-height: 1.3;
  font-weight: 500;
}

@media (max-width: 30rem) {
  .brand {
    min-width: 0;
  }

  .brand__tagline {
    display: none;
  }
}
</style>
