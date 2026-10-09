<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
import BackendStatusCard from '@/components/BackendStatusCard.vue'
import EvaluationLab from '@/components/EvaluationLab.vue'
import OutcomeBadge from '@/components/OutcomeBadge.vue'
import PipelineSteps from '@/components/PipelineSteps.vue'
import TruthBanner from '@/components/TruthBanner.vue'
import { BOUNDARY_OUTCOMES } from '@/domain/types'
import { OUTCOME_COPY } from '@/domain/labels'
</script>

<template>
  <div class="stack">
    <section class="hero card" data-inspect="overview">
      <div class="cluster">
        <p class="eyebrow">Cross system security enforcement console</p>
        <TruthBanner />
      </div>
      <h1>BoundaryMesh</h1>
      <p class="lede">Secure every system crossing.</p>
      <p class="question">Can this transaction cross this security boundary?</p>
      <p>
        BoundaryMesh sits in front of integrations between systems. It is not an assistant, not a
        release board, and not a generic API client. A transaction is authenticated, authorized,
        verified, checked against its contract, and governed before a delivery instruction can reach
        Apache Camel. Credential and consent checks sit in front of the connector. Microsoft Entra
        OIDC and Microsoft Graph Calendar are implemented locally as a harness, with live tenant
        verification only when configured. The hosted operator console is a separate Firebase
        Hosting and BFF path. This page talks to the local API. A valid identity is not an automatic
        crossing.
      </p>
    </section>

    <BackendStatusCard />

    <section class="card stack" data-inspect="pipeline">
      <h2>Deterministic pipeline</h2>
      <p class="muted">
        Every crossing is evaluated in this order. Credential and consent checks, Camel delivery,
        quarantine recovery, tamper evident audit, and cryptographic attestations are implemented
        locally. Hosted Firestore and Cloud KMS are verified on the deployed path.
      </p>
      <PipelineSteps />
    </section>

    <section class="card stack" data-inspect="outcomes">
      <h2>Boundary outcomes</h2>
      <ul class="outcomes">
        <li v-for="outcome in BOUNDARY_OUTCOMES" :key="outcome">
          <OutcomeBadge :outcome="outcome" />
          <p>{{ $show(OUTCOME_COPY[outcome].meaning) }}</p>
        </li>
      </ul>
    </section>

    <EvaluationLab />
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.hero {
  display: grid;
  gap: $space-4;
  background:
    radial-gradient(ellipse 22rem 9rem at 0% 0%, rgb(255 255 255 / 4%), transparent 70%),
    radial-gradient(ellipse 16rem 7rem at 100% 0%, rgb(25 183 176 / 5%), transparent 72%),
    $color-surface;
  box-shadow:
    inset 0 1px 0 rgb(255 255 255 / 6%),
    $shadow-card;
}

.eyebrow {
  font-size: 0.72rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: $color-accent-bright;
  font-weight: 700;
}

.lede {
  font-size: 1.35rem;
  color: $color-accent-bright;
  font-weight: 640;
  letter-spacing: -0.02em;
}

.question {
  font-size: 1.12rem;
  font-weight: 650;
  letter-spacing: -0.015em;
}

.outcomes {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: $space-3;
}

.outcomes li {
  display: grid;
  gap: $space-2;
  align-content: start;
  padding: $space-4;
  border: 1px solid $color-border;
  border-radius: $radius-sm;
  background: $color-surface-raised;
}

@media (min-width: $bp-md) {
  .outcomes {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
