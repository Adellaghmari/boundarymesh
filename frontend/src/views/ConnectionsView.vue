<script setup lang="ts">
import TruthBanner from '@/components/TruthBanner.vue'
import { VELORA_SYSTEMS } from '@/domain/connections'
</script>

<template>
  <div class="stack">
    <header class="stack">
      <div class="cluster">
        <h1>Connections</h1>
        <TruthBanner />
      </div>
      <p>
        Velora Interchange is the locked fictional demo environment. These systems exist so a
        recruiter can see the intended crossings. No live Microsoft Graph connector is configured.
      </p>
    </header>

    <article v-for="system in VELORA_SYSTEMS" :key="system.id" class="card stack">
      <div class="cluster">
        <h2>{{ system.name }}</h2>
        <span class="truth-banner">{{ system.connectorStatus }}</span>
      </div>
      <p><strong>Role.</strong> {{ system.role }}</p>
      <p>{{ system.character }}</p>
    </article>
  </div>
</template>
