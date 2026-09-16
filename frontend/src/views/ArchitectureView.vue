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
        This console currently demonstrates the local control labs. Phase 15I separately deployed
        and verified the private cloud transaction pipeline; browser access to it is Phase 15J work.
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
          <li>Contract Drift and Data Boundary Firewalls</li>
          <li>Local credential/consent boundary</li>
          <li>Separate Camel delivery worker with Resilience4j</li>
          <li>Local quarantine and admin reprocess</li>
          <li>Tamper-evident SHA-256 audit chain</li>
          <li>Local ECDSA P-256 exchange attestations</li>
          <li>Microsoft Entra OIDC session architecture + Graph Calendar harness</li>
          <li>Private Cloud Run API and Worker Pool with dedicated service identities</li>
          <li>Live Firestore, Pub/Sub, and Cloud KMS transaction path</li>
        </ul>
      </article>
      <article class="card stack">
        <h2>Planned</h2>
        <ul>
          <li>Live Microsoft tenant verification (when credentials exist)</li>
          <li>Microsoft Graph change notifications</li>
          <li>Firebase Hosting and authenticated browser-to-private-API access</li>
          <li>Browser readback of live transaction, audit, attestation, and delivery evidence</li>
          <li>Production Entra/OIDC and external connector delivery</li>
        </ul>
      </article>
    </section>

    <section class="card stack">
      <h2>Current pipeline structure</h2>
      <p class="muted">
        Static Phase 2 pipeline. The Integration Boundary Graph is a separate live-state view at
        /boundary-graph. Project B blast-radius graphs are not used here.
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
