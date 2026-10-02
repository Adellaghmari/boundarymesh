<script setup lang="ts">
import PipelineSteps from '@/components/PipelineSteps.vue'
import TruthBanner from '@/components/TruthBanner.vue'
</script>

<template>
  <div class="stack">
    <header class="stack">
      <div class="cluster">
        <h1>Architecture</h1>
        <TruthBanner label="LOCAL LAB / HOSTED TOPOLOGY" />
      </div>
      <p>
        This console is the local control lab. The hosted operator path is Firebase Hosting, then
        the Web BFF, then the private API, then Firestore and the outbox, then Pub/Sub, then the
        Worker Pool. External connectors and production Entra remain disabled.
      </p>
    </header>

    <section class="split">
      <article class="card stack">
        <h2>Current / implemented</h2>
        <ul>
          <li>Vue 3 TypeScript lab console</li>
          <li>Java 25 / Spring Boot local backend</li>
          <li>Boundary Enforcement Engine</li>
          <li>Synthetic local evaluate API</li>
          <li>Spring Security JWT resource server (local issuer / JWKS)</li>
          <li>QuayForge HMAC webhook defense, freshness, and replay</li>
          <li>Contract Drift and Data Boundary Firewalls</li>
          <li>Local credential/consent boundary</li>
          <li>Separate Camel delivery worker with Resilience4j</li>
          <li>Local quarantine and admin reprocess</li>
          <li>Tamper evident SHA256 audit chain</li>
          <li>Local ECDSA P256 exchange attestations</li>
          <li>Microsoft Entra OIDC session architecture + Graph Calendar harness</li>
          <li>Private Cloud Run API and Worker Pool with dedicated service identities</li>
          <li>Firebase Hosting and authenticated Web BFF to the private API</li>
          <li>Live Firestore, Pub/Sub, and Cloud KMS transaction path</li>
        </ul>
      </article>
      <article class="card stack">
        <h2>Not in this environment</h2>
        <ul>
          <li>Microsoft Graph change notifications</li>
          <li>Production Entra/OIDC on the hosted path</li>
          <li>Live external connector delivery</li>
          <li>OpenTelemetry, Cloud Trace, or production alert policies</li>
          <li>Signed artifact provenance / Cosign</li>
        </ul>
      </article>
    </section>

    <section class="card stack">
      <h2>Current pipeline structure</h2>
      <p class="muted">
        Static Phase 2 pipeline. The Integration Boundary Graph is a separate live view on the
        boundary graph page. Project B blast radius graphs are not used here.
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
