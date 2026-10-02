<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
import { onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useRuntimeStore } from '@/stores/runtime'

const store = useRuntimeStore()
const {
  health,
  ready,
  loading,
  errorMessage,
  retryable,
  lastCheckedAt,
  backendReachable,
  backendReady,
} = storeToRefs(store)

onMounted(() => {
  void store.refreshStatus()
})
</script>

<template>
  <section class="card status-card" aria-labelledby="backend-status-heading">
    <div class="cluster">
      <h2 id="backend-status-heading">Local backend status</h2>
      <span class="truth-banner">LOCAL PROJECT DATA</span>
    </div>
    <p class="muted">
      This checks the API configured for this browser session. In local development it is the Java
      process on this machine. The hosted operator console is a separate Firebase Hosting + BFF path.
    </p>

    <div class="status-card__body" aria-live="polite">
      <div v-if="loading && !health" class="status-card__placeholder">
        Checking local health and readiness…
      </div>
      <p v-else-if="errorMessage" class="status-card__error" role="alert">{{ $show(errorMessage) }}</p>
      <dl v-else class="status-card__grid">
        <div>
          <dt>Backend</dt>
          <dd>{{ $show(backendReachable ? 'Reachable' : 'Unknown') }}</dd>
        </div>
        <div>
          <dt>Health</dt>
          <dd>{{ $show(health?.status ?? '—') }}</dd>
        </div>
        <div>
          <dt>Readiness</dt>
          <dd>{{ $show(backendReady ? ready?.status : (ready?.status ?? '—')) }}</dd>
        </div>
        <div>
          <dt>API connectivity</dt>
          <dd>{{ $show(backendReachable && backendReady ? 'Connected' : 'Not connected') }}</dd>
        </div>
        <div>
          <dt>Service</dt>
          <dd>{{ $show(health?.service ?? '—') }}</dd>
        </div>
        <div>
          <dt>Truth</dt>
          <dd>{{ $show(health?.truth ?? '—') }}</dd>
        </div>
      </dl>
      <p v-if="lastCheckedAt" class="muted">Last checked {{ $show(lastCheckedAt) }}</p>
    </div>

    <div class="cluster">
      <button type="button" class="action" :disabled="loading" @click="store.refreshStatus()">
        {{ $show(loading ? 'Checking…' : retryable ? 'Retry' : 'Refresh status') }}
      </button>
    </div>
  </section>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.status-card {
  display: grid;
  gap: $space-4;
  min-height: 14rem;
}

.status-card__placeholder,
.status-card__error {
  min-height: 4.5rem;
  padding: $space-4;
  border-radius: $radius-md;
  border: 1px dashed $color-border;
}

.status-card__error {
  border-color: $color-reject;
  background: $color-reject-soft;
}

.status-card__grid {
  display: grid;
  gap: $space-3;
  margin: 0;
}

.status-card__grid div {
  display: grid;
  gap: $space-1;
}

dt {
  color: $color-ink-muted;
  font-size: 0.8rem;
}

dd {
  margin: 0;
  font-weight: 600;
}

.action {
  background: $color-accent;
  color: white;
  border: 0;
  border-radius: $radius-sm;
  padding: $space-2 $space-4;
}

.action:disabled {
  opacity: 0.7;
}

@media (min-width: $bp-md) {
  .status-card__grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
