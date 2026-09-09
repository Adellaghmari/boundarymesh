<script setup lang="ts">
defineProps<{
  source?: string
  transformed?: string | null
}>()
</script>

<template>
  <section v-if="source || transformed" class="compare" aria-labelledby="compare-heading">
    <h3 id="compare-heading">Before / after</h3>
    <p class="muted">
      SYNTHETIC DEMO values are shown here because they are fictional lab fixtures. Engine evidence
      and logs still use field paths, not values.
    </p>
    <div class="compare__grid">
      <article>
        <h4>Source</h4>
        <pre>{{ source ?? '—' }}</pre>
      </article>
      <article>
        <h4>Destination result</h4>
        <pre>{{ transformed || 'No outbound payload. Crossing did not transform for delivery.' }}</pre>
      </article>
    </div>
  </section>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.compare {
  display: grid;
  gap: $space-3;
}

.compare__grid {
  display: grid;
  gap: $space-3;
}

article {
  border: 1px solid $color-border;
  border-radius: $radius-md;
  padding: $space-3;
  background: $color-surface-raised;
}

pre {
  margin: 0;
  font-family: $font-mono;
  font-size: 0.8rem;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

@media (min-width: $bp-md) {
  .compare__grid {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
