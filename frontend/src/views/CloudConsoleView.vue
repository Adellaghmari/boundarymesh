<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
import { onMounted, onUnmounted, ref } from 'vue'
import { BoundaryMeshApiError } from '@/api/errors'
import {
  readAttestation,
  readDelivery,
  submitSyntheticTransaction,
  verifyAttestation,
  verifyAuditChain,
  type AttestationSummary,
  type DeliveryEvidence,
  type TransactionSubmission,
  type Verification,
} from '@/api/cloud'
import {
  firebaseConfigured,
  initializeCloudSession,
  signInOperator,
  signOutOperator,
} from '@/api/cloudSession'

type AuthState = 'loading' | 'signed-out' | 'authenticated' | 'unauthorized' | 'expired' | 'unavailable'
const authState = ref<AuthState>('loading')
const operatorEmail = ref('')
const busy = ref(false)
const message = ref('')
const submission = ref<TransactionSubmission | null>(null)
const delivery = ref<DeliveryEvidence | null>(null)
const attestation = ref<AttestationSummary | null>(null)
const attestationCheck = ref<Verification | null>(null)
const auditCheck = ref<Verification | null>(null)
const waiting = ref(false)
const timedOut = ref(false)
let timer: number | undefined
let active = true

function handleError(error: unknown): void {
  if (error instanceof BoundaryMeshApiError && error.status === 401) {
    authState.value = 'expired'
    message.value = error.errorCode === 'RECENT_SIGN_IN_REQUIRED'
      ? 'Sign in again to establish a recent secure session.'
      : 'Your session expired. Sign in again to continue.'
  } else if (error instanceof BoundaryMeshApiError && error.status === 403) {
    if (error.errorCode === 'OPERATOR_NOT_AUTHORIZED') {
      authState.value = 'unauthorized'
      message.value = 'This account is not authorized to operate BoundaryMesh.'
    } else {
      authState.value = 'signed-out'
      message.value = error.errorCode === 'CSRF_REJECTED'
        ? 'The secure session could not be established. Refresh and try again.'
        : 'The secure session request was rejected. Refresh and sign in again.'
    }
  } else if (error instanceof BoundaryMeshApiError && error.status === 502) {
    message.value = 'The private BoundaryMesh API is temporarily unavailable.'
  } else if (error instanceof BoundaryMeshApiError && error.kind === 'unavailable') {
    authState.value = 'unavailable'
    message.value = 'The secure web edge is unavailable. Retry when the service is reachable.'
  } else {
    authState.value = 'unavailable'
    message.value = error instanceof Error ? error.message : 'The request could not be completed.'
  }
}

async function restoreSession(): Promise<void> {
  message.value = ''
  authState.value = 'loading'
  try {
    const email = await initializeCloudSession()
    if (email === null) {
      authState.value = 'signed-out'
      return
    }
    operatorEmail.value = email
    authState.value = 'authenticated'
  } catch (error) {
    handleError(error)
  }
}

async function signIn(): Promise<void> {
  busy.value = true
  message.value = ''
  try {
    operatorEmail.value = await signInOperator()
    authState.value = 'authenticated'
  } catch (error) {
    handleError(error)
  } finally {
    busy.value = false
  }
}

async function logout(): Promise<void> {
  busy.value = true
  try {
    await signOutOperator()
    authState.value = 'signed-out'
    operatorEmail.value = ''
    submission.value = null
    delivery.value = null
    waiting.value = false
    timedOut.value = false
    if (timer !== undefined) window.clearTimeout(timer)
  } catch (error) {
    handleError(error)
  } finally {
    busy.value = false
  }
}

async function pollDelivery(instructionId: string, attempt = 0): Promise<void> {
  if (!active || authState.value !== 'authenticated') return
  try {
    const state = await readDelivery(instructionId)
    if (state) {
      delivery.value = state
      waiting.value = false
      return
    }
    if (attempt >= 10) {
      waiting.value = false
      timedOut.value = true
      return
    }
    timer = window.setTimeout(() => void pollDelivery(instructionId, attempt + 1), 3000)
  } catch (error) {
    waiting.value = false
    handleError(error)
  }
}

async function submit(): Promise<void> {
  if (busy.value || authState.value !== 'authenticated') return
  busy.value = true
  message.value = ''
  delivery.value = null
  attestation.value = null
  attestationCheck.value = null
  auditCheck.value = null
  timedOut.value = false
  if (timer !== undefined) window.clearTimeout(timer)
  try {
    const id = crypto.randomUUID()
    const accepted = await submitSyntheticTransaction(id)
    submission.value = accepted
    if (accepted.instruction_id) {
      waiting.value = true
      void pollDelivery(accepted.instruction_id)
    }
    if (accepted.attestation_id && accepted.audit_chain_id) {
      const [att, signed, audit] = await Promise.all([
        readAttestation(accepted.attestation_id),
        verifyAttestation(accepted.attestation_id),
        verifyAuditChain(accepted.audit_chain_id),
      ])
      attestation.value = att
      attestationCheck.value = signed
      auditCheck.value = audit
    }
  } catch (error) {
    handleError(error)
  } finally {
    busy.value = false
  }
}

onMounted(() => void restoreSession())
onUnmounted(() => {
  active = false
  if (timer !== undefined) window.clearTimeout(timer)
})
</script>

<template>
  <div class="cloud-shell">
    <a class="skip-link" href="#main">Skip to content</a>
    <header class="cloud-header">
      <div>
        <strong>BoundaryMesh</strong>
        <span>Governed cloud transactions</span>
      </div>
      <button v-if="authState === 'authenticated'" type="button" :disabled="busy" @click="logout">Sign out</button>
    </header>
    <main id="main" class="cloud-main stack">
      <div class="hero stack">
        <p class="eyebrow">PRIVATE CLOUD PIPELINE · SYNTHETIC DATA</p>
        <h1>See a system crossing governed end to end.</h1>
        <p>Submit one synthetic QuayForge order for Rivertide. BoundaryMesh evaluates it, signs evidence, publishes a delivery instruction, and reports the worker's actual terminal state. No external connector is configured.</p>
      </div>
      <p v-if="message" class="card fail" role="alert">{{ $show(message) }}</p>
      <section v-if="authState !== 'authenticated'" class="card stack" aria-label="Operator sign in">
        <h2>Operator access</h2>
        <p v-if="authState === 'loading'">Checking your session…</p>
        <p v-else-if="authState === 'unauthorized'">Your account is authenticated but not on the operator allowlist.</p>
        <p v-else-if="authState === 'expired'">Your session has expired. Sign in again.</p>
        <p v-else-if="authState === 'unavailable'">The secure web edge is unavailable.</p>
        <p v-else>Sign in with your approved Google account to operate the private BoundaryMesh API.</p>
        <button v-if="authState !== 'loading'" type="button" :disabled="busy || !firebaseConfigured()" @click="signIn">Sign in</button>
        <p v-if="!firebaseConfigured()" class="muted">Firebase web sign in is not configured for this build.</p>
        <button v-if="authState === 'unavailable'" type="button" @click="restoreSession">Retry connection</button>
      </section>
      <template v-else>
        <p class="muted">Signed in as {{ $show(operatorEmail || 'authorized operator') }}. The private API is called by the web edge, never by service credentials held in the browser.</p>
        <section class="card stack" aria-label="Synthetic transaction">
          <h2>Submit a governed transaction</h2>
          <p>Integration: QuayForge orders · Contract: v1 · Destination: Rivertide. The example contains generated synthetic identifiers only.</p>
          <button type="button" :disabled="busy || waiting" @click="submit">{{ $show(busy ? 'Submitting…' : 'Submit synthetic transaction') }}</button>
        </section>
        <section v-if="submission" class="card stack" aria-label="Transaction result">
          <h2>Boundary decision</h2>
          <p><strong>{{ $show(submission.outcome) }}</strong> · {{ $show(submission.submission_status) }}</p>
          <p>Transaction {{ $show(submission.transaction_id) }}</p>
          <p v-if="submission.queued">Queued for asynchronous delivery. Acceptance is not external delivery success.</p>
          <p v-else>No delivery was queued for this decision.</p>
          <p v-if="submission.instruction_id">Instruction {{ $show(submission.instruction_id) }}</p>
        </section>
        <section v-if="attestation || attestationCheck || auditCheck" class="card stack" aria-label="Cryptographic evidence">
          <h2>Evidence</h2>
          <p v-if="attestation">Attestation {{ $show(attestation.status) }} · {{ $show(attestation.algorithm) }} · key version {{ $show(attestation.key_version) }}</p>
          <p v-if="attestationCheck">Signature verification: {{ $show(attestationCheck.status) }}</p>
          <p v-if="auditCheck">Audit chain verification: {{ $show(auditCheck.status) }} · {{ $show(auditCheck.entries_checked ?? '—') }} entries checked</p>
        </section>
        <section v-if="submission?.queued" class="card stack" aria-label="Delivery state" aria-live="polite">
          <h2>Worker delivery</h2>
          <p v-if="waiting">Awaiting the worker's durable result…</p>
          <p v-else-if="timedOut">No terminal result appeared within 30 seconds. The instruction remains unresolved in this view; refresh later.</p>
          <template v-if="delivery">
            <p><strong>{{ $show(delivery.state) }}</strong> · {{ $show(delivery.provider_category) }}</p>
            <p>Provider called: {{ $show(delivery.provider_called ? 'YES' : 'NO') }} · Attempts: {{ $show(delivery.attempt_count) }}</p>
            <p v-if="delivery.state === 'QUARANTINED'">BoundaryMesh retained terminal custody. External delivery did not succeed.</p>
          </template>
        </section>
      </template>
    </main>
  </div>
</template>

<style scoped lang="scss">
.cloud-shell { min-height: 100vh; background: #f2f7f8; color: #172d39; }
.cloud-header { display: flex; justify-content: space-between; align-items: center; gap: 1rem; padding: 1.2rem max(1.5rem, calc((100vw - 72rem) / 2)); background: #102935; color: white; }
.cloud-header div { display: flex; flex-direction: column; }
.cloud-header strong { font-size: 1.35rem; }
.cloud-header span { color: #b9d7d9; font-size: .85rem; }
.cloud-main { max-width: 72rem; margin: auto; padding: 3rem 1.5rem 5rem; gap: 1.4rem; }
.hero { max-width: 46rem; gap: .8rem; }
.hero h1 { font-size: clamp(2rem, 4vw, 3.4rem); }
.eyebrow { letter-spacing: .12em; color: #086c6b; font-size: .76rem; font-weight: 800; }
.card { padding: 1.5rem; border: 1px solid #d2e1e4; border-radius: 1rem; background: white; max-width: 55rem; }
.stack { display: grid; gap: .8rem; }
.muted { color: #526a76; }
.fail { border-color: #b44545; }
button { width: fit-content; border: 0; border-radius: .55rem; padding: .7rem 1.1rem; background: #006e70; color: white; cursor: pointer; }
button:disabled { opacity: .55; cursor: default; }
button:focus-visible { outline: 3px solid #72dfd9; outline-offset: 2px; }
@media (max-width: 40rem) { .cloud-header { padding: 1rem; } .cloud-main { padding: 2rem 1rem; } }
</style>
