<script setup lang="ts">
import PipelineSteps from '@/components/PipelineSteps.vue'
import TruthBanner from '@/components/TruthBanner.vue'
</script>

<template>
  <div class="stack">
    <header class="stack">
      <div class="cluster">
        <h1>Architecture</h1>
        <TruthBanner label="CURRENT VERSUS PLANNED" />
      </div>
      <p>
        This page describes what Phases 1–4 actually run locally. It does not present the final
        cloud architecture as deployed.
      </p>
    </header>

    <section class="split">
      <article class="card stack">
        <h2>Current / implemented</h2>
        <ul>
          <li>Vue 3 TypeScript recruiter console</li>
          <li>Java 25 / Spring Boot local backend</li>
          <li>Boundary Enforcement Engine</li>
          <li>Synthetic local evaluate API</li>
          <li>Spring Security JWT resource server (local issuer / JWKS)</li>
          <li>QuayForge HMAC webhook defense, freshness, and replay</li>
        </ul>
      </article>
      <article class="card stack">
        <h2>Planned</h2>
        <ul>
          <li>Microsoft Entra ID</li>
          <li>Microsoft Graph</li>
          <li>Apache Camel</li>
          <li>GCP, Pub/Sub, Firestore</li>
          <li>Secret Manager, Cloud KMS</li>
          <li>Pulumi</li>
        </ul>
      </article>
    </section>

    <section class="card stack">
      <h2>Current pipeline structure</h2>
      <p class="muted">
        Static Phase 2 pipeline. This is not the Integration Boundary Graph. That flagship remains
        PLANNED until real state-driven graph logic exists.
      </p>
      <PipelineSteps />
    </section>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.split {
  display: grid;
  gap: $space-4;
}

ul {
  margin: 0;
  padding-left: $space-5;
}

@media (min-width: $bp-md) {
  .split {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
