<script setup lang="ts">
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
    <section class="hero card">
      <div class="cluster">
        <p class="eyebrow">Cross-system security enforcement console</p>
        <TruthBanner />
      </div>
      <h1>BoundaryMesh</h1>
      <p class="lede">Secure every system crossing.</p>
      <p class="question">Can this transaction cross this security boundary?</p>
      <p>
        BoundaryMesh sits in front of system-to-system integrations. It is not an assistant, not a
        release board, and not a generic API client. Phase 2 evaluates synthetic Velora Interchange
        crossings against the local Java Boundary Enforcement Engine.
      </p>
    </section>

    <BackendStatusCard />

    <section class="card stack">
      <h2>Deterministic pipeline</h2>
      <p class="muted">
        Every crossing is evaluated in this order. Later phases add delivery, quarantine workflow,
        and signed attestations. Those are not implemented here.
      </p>
      <PipelineSteps />
    </section>

    <section class="card stack">
      <h2>Boundary outcomes</h2>
      <ul class="outcomes">
        <li v-for="outcome in BOUNDARY_OUTCOMES" :key="outcome">
          <OutcomeBadge :outcome="outcome" />
          <p>{{ OUTCOME_COPY[outcome].meaning }}</p>
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
  gap: $space-3;
}

.eyebrow {
  font-size: 0.8rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: $color-ink-muted;
  font-weight: 700;
}

.lede {
  font-size: 1.25rem;
  color: $color-accent;
  font-weight: 600;
}

.question {
  font-size: 1.1rem;
  font-weight: 700;
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
  padding: $space-3;
  border: 1px solid $color-border;
  border-radius: $radius-md;
}

@media (min-width: $bp-md) {
  .outcomes {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
