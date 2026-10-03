<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import OutcomeBadge from '@/components/OutcomeBadge.vue'
import TruthBanner from '@/components/TruthBanner.vue'
import { formatPortfolioDate, millisecondsUntilNextLocalDate } from '@/domain/portfolioDate'
import type { BoundaryOutcome } from '@/domain/types'

const outcomes: BoundaryOutcome[] = [
  'ALLOW',
  'ALLOW_WITH_TRANSFORMATION',
  'QUARANTINE',
  'REJECT',
]

const architecture = [
  'Browser',
  'Firebase Hosting',
  'Web BFF',
  'Private BoundaryMesh API',
  'Firestore and a transactional outbox',
  'Pub/Sub',
  'Worker',
  'External provider boundary',
]

const crossing = [
  'A synthetic QuayForge order for Rivertide arrives as an untrusted payload.',
  'BoundaryMesh checks identity, authorization, authenticity, freshness, and replay.',
  'It validates the integration contract and decides which fields may leave.',
  'The decision is recorded, and Cloud KMS signs an exchange attestation.',
  'Allowed work becomes a delivery instruction. The worker reports the terminal state. No external connector is called.',
]

const currentDate = ref(formatPortfolioDate(new Date()))
let dateTimer: number | undefined

function refreshDate(): void {
  const now = new Date()
  currentDate.value = formatPortfolioDate(now)
  dateTimer = window.setTimeout(refreshDate, millisecondsUntilNextLocalDate(now))
}

onMounted(() => {
  refreshDate()
})

onUnmounted(() => {
  if (dateTimer !== undefined) window.clearTimeout(dateTimer)
})
</script>

<template>
  <div class="portfolio">
    <a class="skip-link" href="#main">Skip to content</a>
    <header class="portfolio__header">
      <a class="brand" href="#top">
        <span class="brand__mark" aria-hidden="true">
          <svg viewBox="0 0 32 36" fill="none">
            <path
              class="brand__halo"
              d="M16 2.5c6.2 3.2 10.7 4.4 13.2 4.8v12.2c0 7.4-5.2 12.8-13.2 16.1C7.9 30.3 2.8 24.9 2.8 17.5V7.3C5.4 6.9 9.8 5.7 16 2.5Z"
            />
            <path
              class="brand__shield"
              d="M16 4.2c5.4 2.8 9.3 3.8 11.4 4.1v11.2c0 6.3-4.4 10.8-11.4 13.6C8.9 30.3 4.6 25.8 4.6 19.5V8.3C6.8 8 10.6 7 16 4.2Z"
            />
            <path
              class="brand__stroke"
              d="M16 7.4c3.6 1.8 6.2 2.5 7.6 2.7v8.2c0 4.2-2.9 7.3-7.6 9.3-4.7-2-7.6-5.1-7.6-9.3V10.1c1.4-.2 4-1 7.6-2.7Z"
            />
            <path class="brand__accent" d="M16 12v12M11.2 16.4h9.6" />
          </svg>
        </span>
        <span class="brand__text">
          <span class="brand__name">BoundaryMesh</span>
          <span class="brand__tagline">Secure every system crossing.</span>
        </span>
      </a>
      <a class="portfolio__operator" href="/operator">Open operator console</a>
    </header>

    <main id="main" class="portfolio__main stack">
      <section id="top" class="hero card stack">
        <div class="cluster">
          <p class="eyebrow">Portfolio</p>
          <TruthBanner label="PUBLIC PAGE" />
        </div>
        <h1>BoundaryMesh</h1>
        <p class="lede">Secure every system crossing.</p>
        <p>
          A successful HTTP request does not prove that one system should be allowed to exchange
          data or actions with another. BoundaryMesh sits in front of that exchange and asks one
          question: can this transaction cross this security boundary?
        </p>
        <p>
          A system crossing is an untrusted payload leaving one trust domain and entering another.
          One example is a signed partner webhook that becomes a settlement instruction.
          BoundaryMesh checks who is calling, whether they may act, whether the payload is
          authentic and fresh, and whether it has been seen before. It then decides what may leave.
        </p>
      </section>

      <section class="card stack" aria-labelledby="built-heading">
        <h2 id="built-heading">What I built</h2>
        <p>
          I built a Vue console, a Java and Spring API, and an Apache Camel worker. A transaction
          is checked before a delivery instruction can exist. The hosted path keeps the core API
          private behind a web BFF. This is a live portfolio and development environment. It is not
          enterprise production, and it does not carry paid customer traffic.
        </p>
      </section>

      <section class="card stack" aria-labelledby="architecture-heading">
        <h2 id="architecture-heading">Architecture</h2>
        <p>
          The browser loads static files from Firebase Hosting. Operator calls go to the web BFF.
          The BFF calls the private API. The API writes Firestore, including a transactional
          outbox, and publishes to Pub/Sub. A worker consumes that event. External connectors stay
          off on purpose.
        </p>
        <ol class="flow">
          <li v-for="step in architecture" :key="step">{{ step }}</li>
        </ol>
        <ul class="notes">
          <li>Firebase Authentication protects the operator console. This page does not use it.</li>
          <li>The core API is private. The browser never holds service credentials.</li>
          <li>Secret Manager can hold a named secret for the worker. This page does not read one.</li>
          <li>Cloud KMS signs exchange attestations. That is application evidence, not artifact signing.</li>
        </ul>
      </section>

      <section class="card stack" aria-labelledby="crossing-heading">
        <div class="cluster">
          <h2 id="crossing-heading">Example crossing</h2>
          <TruthBanner label="SYNTHETIC EXAMPLE" />
        </div>
        <p>This is presentation only. Nothing on this page calls the API, the worker, or a cloud service.</p>
        <ol class="flow">
          <li v-for="step in crossing" :key="step">{{ step }}</li>
        </ol>
        <div class="cluster" aria-label="Possible decisions">
          <OutcomeBadge v-for="outcome in outcomes" :key="outcome" :outcome="outcome" />
        </div>
      </section>

      <section class="card stack" aria-labelledby="highlights-heading">
        <h2 id="highlights-heading">Engineering highlights</h2>
        <ul class="notes">
          <li>Vue and TypeScript on the frontend. Java, Spring, and Apache Camel on the backend.</li>
          <li>Firebase Hosting, Cloud Run, Firestore, Pub/Sub, and a worker process the hosted path.</li>
          <li>Pulumi owns the infrastructure. GitHub Actions delivers a digest pinned release.</li>
          <li>CI, deploy, and workload identities use Workload Identity Federation, with no keys managed by users.</li>
          <li>Cloud KMS signs exchange attestations. SBOM and provenance are published and unsigned.</li>
          <li>A person starts promotion. Deployment evidence is immutable. Rollback is implemented and has not been run against live traffic.</li>
        </ul>
      </section>

      <section class="card stack" aria-labelledby="stack-heading">
        <h2 id="stack-heading">Tech stack</h2>
        <p>
          Vue 3, TypeScript, Pinia, and Vite. Java 25, Spring Boot, Spring Security, Apache Camel,
          and Resilience4j. Cloud Run, Firebase Hosting, Firestore, Pub/Sub, Secret Manager, and
          Cloud KMS. Pulumi and GitHub Actions.
        </p>
      </section>

      <section class="card stack" aria-labelledby="status-heading">
        <h2 id="status-heading">Live portfolio status</h2>
        <p>
          <span class="date-label">Current date</span>
          <time data-testid="portfolio-date">{{ currentDate }}</time>
        </p>
        <ul class="notes">
          <li>The portfolio environment is live. It is not enterprise production.</li>
          <li>Rollback is implemented, with preserved targets. It has not been executed against the live environment.</li>
          <li>SBOM and provenance are published and unsigned.</li>
          <li>Production Entra is disabled. Production external connectors are disabled.</li>
          <li>OpenTelemetry is not implemented.</li>
          <li>This public page is the recruiter explanation. Operator actions stay behind sign in.</li>
        </ul>
      </section>

      <section class="card stack" aria-labelledby="security-heading">
        <h2 id="security-heading">Security and delivery</h2>
        <p>
          GitHub Actions authenticates to Google Cloud with Workload Identity Federation. A
          deployment consumes a release artifact that is already pinned to a digest. It does not
          rebuild the image. The private API stays behind the web BFF.
        </p>
      </section>

      <section class="card stack" aria-labelledby="testing-heading">
        <h2 id="testing-heading">Testing</h2>
        <p>
          The repository has backend, frontend, infrastructure, deploy script, and browser tests.
          They are separate suites. This page does not add those counts together, and it does not
          depend on a running API to render.
        </p>
      </section>

      <section class="card stack" aria-labelledby="scope-heading">
        <h2 id="scope-heading">Project scope</h2>
        <p>
          BoundaryMesh shows one internal event path, infrastructure as code, delivery automation,
          and real security boundaries. It does not claim paid production, a live rollback that
          already succeeded, a signed software supply chain, OpenTelemetry, hosted Entra, or live
          external connectors. Workstation credentials exist, so human access is not universally
          keyless.
        </p>
        <div class="cluster">
          <a class="action" href="https://github.com/Adellaghmari/boundarymesh">Source on GitHub</a>
          <a class="action action--quiet" href="/operator">Open operator console</a>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.portfolio {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgb(20 163 154 / 16%), transparent 28rem),
    $color-canvas;
}

.portfolio__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: $space-4;
  padding: $space-4 max($space-5, calc((100vw - #{$measure}) / 2));
  background: linear-gradient(100deg, $color-header 0%, $color-header-mid 62%, $color-header-teal 140%);
  color: $color-header-ink;
  box-shadow: $shadow-header;
}

.portfolio__main {
  width: min(100%, $measure);
  margin: 0 auto;
  padding: $space-6 $space-4 $space-7;
}

.hero h1 {
  font-size: clamp(2.2rem, 5vw, 3.6rem);
}

.lede {
  font-size: 1.25rem;
  color: $color-ink;
}

.eyebrow {
  margin: 0;
  color: $color-accent;
  font-size: 0.75rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.flow,
.notes {
  margin: 0;
  padding-left: 1.2rem;
  display: grid;
  gap: $space-2;
}

.flow {
  list-style: decimal;
}

.date-label {
  display: block;
  color: $color-ink-muted;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.brand {
  display: flex;
  align-items: center;
  gap: $space-3;
  color: inherit;
  text-decoration: none;
}

.brand__mark {
  display: grid;
  width: 2.5rem;
  height: 2.75rem;
}

.brand__mark svg {
  width: 2.35rem;
  height: 2.6rem;
}

.brand__halo {
  fill: rgb(20 163 154 / 18%);
}

.brand__shield {
  fill: $color-header;
  stroke: $color-nav-active;
  stroke-width: 1.2;
}

.brand__stroke {
  fill: none;
  stroke: $color-header-ink;
  stroke-width: 1.35;
}

.brand__accent {
  stroke: $color-nav-active;
  stroke-width: 1.6;
  stroke-linecap: round;
}

.brand__text {
  display: grid;
}

.brand__name {
  font-weight: 700;
}

.brand__tagline {
  color: $color-nav-muted;
  font-size: 0.75rem;
}

.portfolio__operator,
.action {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  border-radius: $radius-md;
  padding: 0.7rem 1rem;
  background: $color-accent;
  color: white;
  text-decoration: none;
  font-weight: 700;
}

.action--quiet {
  background: transparent;
  color: $color-accent;
  border: 1px solid $color-accent;
}

.portfolio__header .portfolio__operator {
  background: transparent;
  color: $color-header-ink;
  border: 1px solid $color-nav-active-border;
}

@media (max-width: 40rem) {
  .portfolio__header {
    align-items: flex-start;
    flex-direction: column;
  }

  .brand__tagline {
    display: none;
  }
}
</style>
