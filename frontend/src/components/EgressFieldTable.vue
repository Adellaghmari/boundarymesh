<script setup lang="ts">
import type { EgressView } from '@/domain/types'

defineProps<{
  egress?: EgressView
}>()
</script>

<template>
  <section v-if="egress" class="table-wrap" aria-labelledby="egress-heading">
    <h3 id="egress-heading">Data boundary actions</h3>
    <p class="muted">
      Policy {{ egress.policyId }} {{ egress.policyVersion }} → {{ egress.destination }}. Evidence is
      field paths, not values.
    </p>
    <table>
      <caption>Source field, boundary action, destination result</caption>
      <thead>
        <tr>
          <th scope="col">Source field</th>
          <th scope="col">Boundary action</th>
          <th scope="col">Rule</th>
          <th scope="col">Destination result</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="field in egress.fields" :key="`${field.path}-${field.ruleId}`" :data-action="field.action">
          <td><code>{{ field.path }}</code></td>
          <td>{{ field.action }}</td>
          <td>{{ field.ruleId }}</td>
          <td>
            <span v-if="field.action === 'ALLOW'">copied</span>
            <span v-else-if="field.action === 'REDACT'">[REDACTED]</span>
            <span v-else>omitted / blocked</span>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.table-wrap {
  display: grid;
  gap: $space-3;
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.92rem;
}

th,
td {
  text-align: left;
  padding: $space-2 $space-3;
  border-bottom: 1px solid $color-border;
}

th {
  color: $color-ink-muted;
  font-size: 0.78rem;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

tr[data-action='REDACT'] td:nth-child(2) {
  color: $color-transform;
  font-weight: 700;
}

tr[data-action='DENY'] td:nth-child(2) {
  color: $color-reject;
  font-weight: 700;
}

tr[data-action='ALLOW'] td:nth-child(2) {
  color: $color-allow;
  font-weight: 700;
}

code {
  font-family: $font-mono;
}

caption {
  text-align: left;
  color: $color-ink-muted;
  padding-bottom: $space-2;
}
</style>
