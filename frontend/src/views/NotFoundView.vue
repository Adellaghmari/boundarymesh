<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <section class="card stack">
    <h1>Page not found</h1>
    <p>That route is not part of the Phase 2 console.</p>
    <p>
      <RouterLink to="/">Return to overview</RouterLink>
    </p>
  </section>
</template>
