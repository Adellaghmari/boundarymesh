<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
import TruthBanner from '@/components/TruthBanner.vue'
import { CAPABILITY_CLAIMS } from '@/config/capabilities'
import { CAPABILITY_STATUS_COPY } from '@/domain/labels'
</script>

<template>
  <div class="stack" data-inspect="status">
    <header class="stack">
      <div class="cluster">
        <h1>Project status</h1>
        <TruthBanner label="REVIEWED EVIDENCE SUMMARY" />
      </div>
      <p>
        These labels summarize reviewed project evidence; they are not a live cloud status feed.
        This local page is the lab console. The hosted operator SPA on Firebase Hosting is a
        separate bundle and requires sign in.
      </p>
    </header>

    <table class="claims">
      <caption class="visually-hidden">
        Capability status
      </caption>
      <thead>
        <tr>
          <th scope="col">Capability</th>
          <th scope="col">Status</th>
          <th scope="col">Notes</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="claim in CAPABILITY_CLAIMS" :key="claim.id">
          <th scope="row">{{ $show(claim.label) }}</th>
          <td>
            <span class="claim-status" :data-status="claim.status">
              {{ $show(CAPABILITY_STATUS_COPY[claim.status]) }}
            </span>
          </td>
          <td>{{ $show(claim.notes) }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.claims {
  width: 100%;
  border-collapse: collapse;
  background: $color-surface;
  border: 1px solid $color-border;
}

th,
td {
  text-align: left;
  padding: $space-3;
  border-bottom: 1px solid $color-border;
  vertical-align: top;
}

.claim-status {
  display: inline-flex;
  padding: 0.15rem 0.45rem;
  border: 1px solid currentcolor;
  border-radius: $radius-sm;
  font-size: 0.75rem;
  font-weight: 700;
}

.claim-status[data-status='PLANNED'] {
  color: $color-ink-muted;
}

.claim-status[data-status='IMPLEMENTED'],
.claim-status[data-status='TESTED'] {
  color: $color-allow;
  background: $color-allow-soft;
}

.claim-status[data-status='LIVE VERIFIED'] {
  color: $color-accent;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
}
</style>
