<script setup lang="ts">
import ControlStatusBadge from '@/components/ControlStatusBadge.vue'
import type { AuthorizationView, IdentityView } from '@/domain/types'

defineProps<{
  identity?: IdentityView
  authorization?: AuthorizationView
}>()
</script>

<template>
  <div v-if="identity || authorization" class="split">
    <section v-if="identity" class="card-lite" aria-labelledby="authn-heading">
      <div class="cluster">
        <h3 id="authn-heading">Authentication</h3>
        <ControlStatusBadge :status="identity.status" />
      </div>
      <p>{{ identity.explanation ?? 'Identity control from the Java engine.' }}</p>
      <p v-if="identity.issuer" class="muted">Issuer {{ identity.issuer }}</p>
      <p v-if="identity.principal" class="muted">Principal {{ identity.principal }}</p>
    </section>
    <section v-if="authorization" class="card-lite" aria-labelledby="authz-heading">
      <div class="cluster">
        <h3 id="authz-heading">Authorization</h3>
        <ControlStatusBadge :status="authorization.status" />
      </div>
      <p>{{ authorization.explanation ?? 'Authorization control from the Java engine.' }}</p>
      <p class="muted">
        Granted scopes:
        {{ authorization.grantedScopes.length > 0 ? authorization.grantedScopes.join(', ') : 'none' }}
      </p>
    </section>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.split {
  display: grid;
  gap: $space-3;
}

.card-lite {
  border: 1px solid $color-border;
  border-radius: $radius-md;
  padding: $space-3;
  background: $color-surface-raised;
  display: grid;
  gap: $space-2;
}

@media (min-width: $bp-md) {
  .split {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
