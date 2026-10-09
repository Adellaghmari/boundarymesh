<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
import { RouterLink } from 'vue-router'
import AuthnAuthzPanel from '@/components/AuthnAuthzPanel.vue'
import ContractEvidence from '@/components/ContractEvidence.vue'
import ControlVectorView from '@/components/ControlVectorView.vue'
import CrossingPipeline from '@/components/CrossingPipeline.vue'
import EgressFieldTable from '@/components/EgressFieldTable.vue'
import OutcomeBadge from '@/components/OutcomeBadge.vue'
import TruthBanner from '@/components/TruthBanner.vue'
import WebhookDefensePanel from '@/components/WebhookDefensePanel.vue'
import { crossingLabel, OUTCOME_COPY } from '@/domain/labels'
import TimestampLabel from '@/components/TimestampLabel.vue'
import type { BoundaryEventRecord } from '@/domain/types'

defineProps<{
  event: BoundaryEventRecord
  showDetailLink?: boolean
  compact?: boolean
}>()
</script>

<template>
  <article class="card result" :data-outcome="event.outcome" aria-labelledby="decision-heading">
    <div class="cluster">
      <h2 id="decision-heading">Boundary outcome</h2>
      <OutcomeBadge :outcome="event.outcome" />
      <TruthBanner />
    </div>
    <p>{{ $show(OUTCOME_COPY[event.outcome].meaning) }}</p>
    <p class="muted">
      This is a deterministic Java engine decision, not a score and not a probabilistic judgment.
    </p>
    <p v-if="event.duplicateDelivery" class="muted">
      Legitimate duplicate delivery. Replay attack: no. Side effect: not repeated.
    </p>
    <AuthnAuthzPanel
      v-if="event.identity || event.authorization"
      :identity="event.identity"
      :authorization="event.authorization"
    />
    <WebhookDefensePanel
      v-if="event.ingressKind === 'SIGNED_WEBHOOK'"
      :controls="event.controlResults"
      :duplicate-delivery="event.duplicateDelivery"
    />
    <CrossingPipeline
      v-if="!compact && event.enforcementTrace?.length"
      :steps="event.enforcementTrace"
      :applied="event.appliedControls"
    />
    <ContractEvidence v-if="!compact" :contract="event.contract" />
    <EgressFieldTable v-if="!compact" :egress="event.egress" />
    <p v-if="event.ingressKind" class="muted">Ingress {{ $show(event.ingressKind) }}</p>

    <dl class="result__meta">
      <div>
        <dt>Transaction ID</dt>
        <dd>{{ $show(event.transactionId) }}</dd>
      </div>
      <div>
        <dt>Crossing</dt>
        <dd>{{ $show(crossingLabel(event.sourceSystem, event.destinationSystem)) }}</dd>
      </div>
      <div>
        <dt>Policy version</dt>
        <dd>{{ $show(event.policyVersion) }}</dd>
      </div>
      <div>
        <dt>Decision time</dt>
        <dd><TimestampLabel :value="event.decidedAt" /></dd>
      </div>
      <div>
        <dt>Scenario</dt>
        <dd>{{ $show(event.scenarioLabel) }}</dd>
      </div>
    </dl>

    <section>
      <h3>Reason codes</h3>
      <ul v-if="event.reasonCodes.length > 0">
        <li v-for="reason in event.reasonCodes" :key="reason.code">
          <strong>{{ $show(reason.code) }}</strong>
          <span v-if="reason.description">. {{ $show(reason.description) }}</span>
        </li>
      </ul>
      <p v-else class="muted">No reason codes. All required controls passed.</p>
    </section>

    <section>
      <h3>Applied controls</h3>
      <ControlVectorView :controls="event.controlResults" />
    </section>

    <p v-if="showDetailLink">
      <RouterLink
        :to="{ name: 'boundary-event-detail', params: { transactionId: event.transactionId } }"
      >
        Open event detail
      </RouterLink>
    </p>
  </article>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.result {
  display: grid;
  gap: $space-4;
  border-left: 4px solid $color-border;
}

.result[data-outcome='ALLOW'] {
  border-left-color: $color-allow;
}

.result[data-outcome='ALLOW_WITH_TRANSFORMATION'] {
  border-left-color: $color-transform;
}

.result[data-outcome='QUARANTINE'] {
  border-left-color: $color-quarantine;
}

.result[data-outcome='REJECT'] {
  border-left-color: $color-reject;
}

.result__meta {
  display: grid;
  gap: $space-3;
  margin: 0;
}

.result__meta dd {
  margin: 0;
  font-family: $font-mono;
  font-size: 0.9rem;
  overflow-wrap: anywhere;
}

dt {
  color: $color-ink-muted;
  font-size: 0.8rem;
}

ul {
  margin: $space-2 0 0;
  padding-left: $space-5;
}

@media (min-width: $bp-md) {
  .result__meta {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
