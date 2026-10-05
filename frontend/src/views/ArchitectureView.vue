<script setup lang="ts">
import PipelineSteps from '@/components/PipelineSteps.vue'
import TruthBanner from '@/components/TruthBanner.vue'
</script>

<template>
  <div class="stack" data-inspect="architecture">
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

    <section class="card stack">
      <h2>Implemented architecture</h2>
      <ul>
        <li>Vue 3 TypeScript lab console</li>
        <li>Java 25 / Spring Boot local backend</li>
        <li>Boundary Enforcement Engine</li>
        <li>Synthetic local evaluate API</li>
        <li>Spring Security JWT resource server (local issuer / JWKS)</li>
        <li>QuayForge HMAC webhook defense, freshness, and replay</li>
        <li>Contract Drift and Data Boundary Firewalls</li>
        <li>Local credential and consent boundary</li>
        <li>Separate Camel delivery worker with Resilience4j</li>
        <li>Local quarantine and admin reprocess</li>
        <li>Tamper evident SHA256 audit chain</li>
        <li>Local ECDSA P256 exchange attestations</li>
        <li>Microsoft Entra OIDC session architecture and Graph Calendar harness</li>
        <li>Private Cloud Run API and Worker Pool with dedicated service identities</li>
        <li>Firebase Hosting and authenticated Web BFF to the private API</li>
        <li>Live Firestore, Pub/Sub, and Cloud KMS transaction path</li>
      </ul>
    </section>

    <section class="card stack" data-testid="public-demo-scope">
      <h2>Public demo scope</h2>
      <p class="muted">
        These are real project boundaries. They are not available in this public environment, and
        they are not described as implemented.
      </p>
      <ul class="scope">
        <li>
          <span class="scope__label">Planned</span>
          Microsoft Graph change notifications
        </li>
        <li>
          <span class="scope__label">Intentionally disabled in public demo</span>
          Production Entra OIDC on the hosted path
        </li>
        <li>
          <span class="scope__label">Not enabled in public environment</span>
          Live external connector delivery
        </li>
        <li>
          <span class="scope__label">Future enhancement</span>
          OpenTelemetry, Cloud Trace, or production alert policies
        </li>
        <li>
          <span class="scope__label">Planned</span>
          Signed artifact provenance and Cosign
        </li>
      </ul>
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

ul {
  margin: 0;
  padding-left: $space-5;
}

.scope {
  list-style: none;
  padding-left: 0;
  display: grid;
  gap: $space-3;
}

.scope li {
  display: flex;
  flex-wrap: wrap;
  gap: $space-2;
  align-items: baseline;
}

.scope__label {
  display: inline-flex;
  align-items: center;
  padding: 0.1rem $space-2;
  border: 1px solid $color-border;
  border-radius: $radius-sm;
  background: $color-surface-raised;
  color: $color-ink-muted;
  font-size: 0.75rem;
  font-weight: 700;
}
</style>
