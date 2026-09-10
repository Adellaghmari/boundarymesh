<script setup lang="ts">
</script>

<template>
  <svg class="mesh" viewBox="0 0 420 160" aria-hidden="true" focusable="false">
    <defs>
      <linearGradient id="meshFade" x1="0" x2="1">
        <stop offset="0" stop-color="#7ee8df" stop-opacity="0" />
        <stop offset="1" stop-color="#7ee8df" stop-opacity="0.35" />
      </linearGradient>
    </defs>
    <path d="M20 130 C 90 40, 170 150, 250 70 S 390 20, 420 90" stroke="url(#meshFade)" fill="none" />
    <path d="M40 150 C 120 80, 200 160, 300 90 S 400 40, 420 60" stroke="url(#meshFade)" fill="none" />
    <path d="M0 90 C 80 10, 160 120, 260 50 S 380 10, 420 40" stroke="url(#meshFade)" fill="none" />
  </svg>
</template>

<style scoped lang="scss">
.mesh {
  position: absolute;
  right: 0;
  top: 0;
  width: min(28rem, 46%);
  height: 100%;
  pointer-events: none;
  opacity: 0.55;
}
</style>
