<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import CrossingPipeline from '@/components/CrossingPipeline.vue'
import OutcomeBadge from '@/components/OutcomeBadge.vue'
import TruthBanner from '@/components/TruthBanner.vue'
import { formatPortfolioDate, millisecondsUntilNextLocalDate } from '@/domain/portfolioDate'
import {
  EXPLORER_LOCKED_ACTIONS,
  EXPLORER_SECTIONS,
  PORTFOLIO_EXPLORER_CROSSINGS,
  explorerTrace,
  filterPortfolioCrossings,
  type ExplorerSection,
} from '@/domain/portfolioExplorer'
import { BOUNDARY_OUTCOMES, type BoundaryOutcome } from '@/domain/types'

const crossings = PORTFOLIO_EXPLORER_CROSSINGS
const query = ref('')
const outcomeFilter = ref<BoundaryOutcome | 'ALL'>('ALL')
const section = ref<ExplorerSection>('overview')
const selectedId = ref(crossings[0]?.id ?? '')
const aboutOpen = ref(false)
const aboutPanel = ref<HTMLElement | null>(null)
const currentDate = ref(formatPortfolioDate(new Date()))
let dateTimer: number | undefined

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

const visible = computed(() => filterPortfolioCrossings(crossings, query.value, outcomeFilter.value))
const selected = computed(() => visible.value.find((crossing) => crossing.id === selectedId.value) ?? visible.value[0] ?? null)
const trace = computed(() => (selected.value ? explorerTrace(selected.value) : []))

watch(visible, (rows) => {
  if (!rows.some((crossing) => crossing.id === selectedId.value)) {
    selectedId.value = rows[0]?.id ?? ''
  }
})

function count(outcome: BoundaryOutcome): number {
  return crossings.filter((crossing) => crossing.outcome === outcome).length
}

function selectCrossing(id: string): void {
  selectedId.value = id
}

function selectSection(id: ExplorerSection): void {
  section.value = id
}

function refreshDate(): void {
  const now = new Date()
  currentDate.value = formatPortfolioDate(now)
  dateTimer = window.setTimeout(refreshDate, millisecondsUntilNextLocalDate(now))
}

async function openAbout(): Promise<void> {
  aboutOpen.value = true
  await nextTick()
  aboutPanel.value?.focus()
}

function closeAbout(): void {
  aboutOpen.value = false
}

function onAboutKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape' && aboutOpen.value) closeAbout()
}

onMounted(() => {
  document.title = 'BoundaryMesh'
  refreshDate()
  window.addEventListener('keydown', onAboutKeydown)
})

onUnmounted(() => {
  if (dateTimer !== undefined) window.clearTimeout(dateTimer)
  window.removeEventListener('keydown', onAboutKeydown)
})
</script>

<template>
  <div class="explorer">
    <a class="skip-link" href="#main">Skip to content</a>
    <header class="explorer__header">
      <a class="brand" href="/" aria-label="BoundaryMesh">
        <span class="brand__mark" aria-hidden="true">
          <svg viewBox="0 0 32 36" fill="none">
            <path class="brand__halo" d="M16 2.5c6.2 3.2 10.7 4.4 13.2 4.8v12.2c0 7.4-5.2 12.8-13.2 16.1C7.9 30.3 2.8 24.9 2.8 17.5V7.3C5.4 6.9 9.8 5.7 16 2.5Z" />
            <path class="brand__shield" d="M16 4.2c5.4 2.8 9.3 3.8 11.4 4.1v11.2c0 6.3-4.4 10.8-11.4 13.6C8.9 30.3 4.6 25.8 4.6 19.5V8.3C6.8 8 10.6 7 16 4.2Z" />
            <path class="brand__stroke" d="M16 7.4c3.6 1.8 6.2 2.5 7.6 2.7v8.2c0 4.2-2.9 7.3-7.6 9.3-4.7-2-7.6-5.1-7.6-9.3V10.1c1.4-.2 4-1 7.6-2.7Z" />
            <path class="brand__accent" d="M16 12v12M11.2 16.4h9.6" />
          </svg>
        </span>
        <span class="brand__text">
          <span class="brand__name">BoundaryMesh</span>
          <span class="brand__tagline">Secure every system crossing.</span>
        </span>
      </a>
      <nav class="explorer__links" aria-label="Application">
        <button type="button" class="nav-link" @click="openAbout">About the project</button>
        <a class="nav-link" href="https://github.com/Adellaghmari/boundarymesh">Source on GitHub</a>
        <a class="nav-link" href="/operator">Operator sign in</a>
      </nav>
    </header>

    <main id="main" class="explorer__main stack">
      <div class="cluster">
        <TruthBanner label="Portfolio demo" />
        <TruthBanner label="Synthetic data" />
      </div>
      <div class="product-heading">
        <h1>System crossings</h1>
        <p>Synthetic QuayForge and Rivertide data. This is not customer traffic.</p>
      </div>
      <div class="workspace">
        <aside class="card stack" aria-label="Synthetic crossings">
          <label class="field">
            <span>Search</span>
            <input v-model="query" type="search" data-testid="explorer-search" placeholder="Search crossings" />
          </label>
          <label class="field">
            <span>Decision</span>
            <select v-model="outcomeFilter" data-testid="explorer-filter">
              <option value="ALL">All decisions</option>
              <option value="ALLOW">Allow</option>
              <option value="ALLOW_WITH_TRANSFORMATION">Allow with transformation</option>
              <option value="QUARANTINE">Quarantine</option>
              <option value="REJECT">Reject</option>
            </select>
          </label>
          <p v-if="visible.length === 0" class="muted">No crossing matches this search.</p>
          <div class="crossing-list">
            <button
              v-for="crossing in visible"
              :key="crossing.id"
              type="button"
              class="crossing"
              :aria-pressed="selected?.id === crossing.id"
              @click="selectCrossing(crossing.id)"
            >
              <span class="crossing__name">{{ crossing.name }}</span>
              <span class="muted">{{ crossing.source }} to {{ crossing.destination }}</span>
              <OutcomeBadge :outcome="crossing.outcome" />
            </button>
          </div>
        </aside>

        <section v-if="selected" class="detail stack" aria-live="polite">
          <div class="tabs" role="tablist" aria-label="Product views">
            <button
              v-for="tab in EXPLORER_SECTIONS"
              :key="tab.id"
              type="button"
              role="tab"
              class="tabs__tab"
              :aria-selected="section === tab.id"
              @click="selectSection(tab.id)"
            >
              {{ tab.label }}
            </button>
          </div>

          <article v-if="section === 'overview'" class="card stack" role="tabpanel">
            <h2>Overview</h2>
            <p>{{ selected.summary }}</p>
            <ul class="metrics">
              <li v-for="outcome in BOUNDARY_OUTCOMES" :key="outcome">
                <OutcomeBadge :outcome="outcome" />
                <strong>{{ count(outcome) }}</strong>
              </li>
            </ul>
            <CrossingPipeline :steps="trace" />
          </article>

          <article v-else-if="section === 'crossing'" class="card stack" role="tabpanel">
            <h2>Incoming crossing</h2>
            <p>{{ selected.incoming }}</p>
            <h3>Source identity</h3>
            <p>{{ selected.identity }}</p>
            <h3>Authorization</h3>
            <p>{{ selected.authorization }}</p>
            <h3>Authenticity</h3>
            <p>{{ selected.authenticity }}</p>
            <h3>Freshness</h3>
            <p>{{ selected.freshness }}</p>
            <h3>Replay detection</h3>
            <p>{{ selected.replay }}</p>
            <h3>Contract validation</h3>
            <p>{{ selected.contract }}</p>
          </article>

          <article v-else-if="section === 'policy'" class="card stack" role="tabpanel">
            <h2>Policy decision</h2>
            <div class="cluster" data-testid="policy-decision">
              <OutcomeBadge :outcome="selected.outcome" />
              <p>{{ selected.policy }}</p>
            </div>
            <h3>Transformation</h3>
            <p>{{ selected.transformation }}</p>
          </article>

          <article v-else-if="section === 'evidence'" class="card stack" role="tabpanel">
            <h2>Exchange attestation</h2>
            <p data-testid="attestation">{{ selected.attestation }}</p>
            <h3>Cloud KMS</h3>
            <p>{{ selected.signing }}</p>
            <h2>Evidence timeline</h2>
            <div data-testid="timeline" class="stack">
              <details v-for="entry in selected.timeline" :key="entry.id" class="timeline" :open="entry === selected.timeline[0]">
                <summary>{{ entry.title }}</summary>
                <p>{{ entry.detail }}</p>
              </details>
            </div>
          </article>

          <article v-else class="card stack" role="tabpanel">
            <h2>Delivery instruction</h2>
            <p>{{ selected.instruction }}</p>
            <h3>Worker processing</h3>
            <p>{{ selected.worker }}</p>
            <h3>Terminal state</h3>
            <p data-testid="delivery-state">{{ selected.terminal }}</p>
          </article>
        </section>
      </div>

      <section class="card stack" aria-label="Operator actions">
        <h2>Operator actions</h2>
        <p class="muted">These controls are disabled. Real operator actions require authentication. This demo does not change cloud state.</p>
        <div class="cluster">
          <button v-for="action in EXPLORER_LOCKED_ACTIONS" :key="action" class="locked" type="button" disabled>
            {{ action }}
          </button>
        </div>
      </section>
    </main>

    <div v-if="aboutOpen" class="about-layer" @click.self="closeAbout">
      <section
        ref="aboutPanel"
        class="about card stack"
        role="dialog"
        aria-modal="true"
        aria-labelledby="about-title"
        tabindex="-1"
      >
        <div class="cluster about__bar">
          <h2 id="about-title">About the project</h2>
          <button type="button" class="about__close" @click="closeAbout">Close</button>
        </div>
        <p>BoundaryMesh decides whether one system may exchange data or an action with another.</p>
        <h3>The problem</h3>
        <p>
          A successful request does not prove that one system should be allowed to exchange data or
          actions with another. BoundaryMesh sits in front of that exchange.
        </p>
        <h3>What I built</h3>
        <p>
          I built a Vue console, a Java and Spring API, and an Apache Camel worker. A transaction is
          checked before a delivery instruction can exist. This is a live portfolio and development
          environment. It is not enterprise production.
        </p>
        <h3>Architecture</h3>
        <p>
          The browser loads this application from Firebase Hosting. Operator calls go to the web BFF.
          The BFF calls the private API. The API writes Firestore, including a transactional outbox,
          and publishes to Pub/Sub. A worker consumes that event. External connectors stay off on purpose.
        </p>
        <ol class="about__flow">
          <li v-for="step in architecture" :key="step">{{ step }}</li>
        </ol>
        <h3>Security model</h3>
        <p>
          Firebase Authentication protects the operator console. This public application does not use it.
          The private API stays behind the web BFF. The browser never holds service credentials. Cloud KMS
          signs exchange attestations. Real operator actions require authentication.
        </p>
        <h3>Technology stack</h3>
        <p>
          Vue 3, TypeScript, and Vite. Java, Spring Boot, and Apache Camel. Cloud Run, Firebase Hosting,
          Firestore, Pub/Sub, and Cloud KMS. Pulumi and GitHub Actions.
        </p>
        <h3>Synthetic data</h3>
        <p>
          The crossings in this application are synthetic QuayForge and Rivertide data. They are not
          customer traffic. Nothing on this public page calls the private API.
        </p>
        <p>
          <span class="date-label">Current date</span>
          <time data-testid="portfolio-date">{{ currentDate }}</time>
        </p>
        <a href="https://github.com/Adellaghmari/boundarymesh">Source on GitHub</a>
      </section>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.explorer {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgb(20 163 154 / 16%), transparent 28rem),
    $color-canvas;
}

.explorer__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: $space-4;
  flex-wrap: wrap;
  padding: $space-4 max($space-5, calc((100vw - #{$measure}) / 2));
  background: linear-gradient(100deg, $color-header 0%, $color-header-mid 62%, $color-header-teal 140%);
  color: $color-header-ink;
  box-shadow: $shadow-header;
}

.explorer__main {
  width: min(100%, $measure);
  margin: 0 auto;
  padding: $space-6 $space-4 $space-7;
}

.explorer__links {
  display: flex;
  flex-wrap: wrap;
  gap: $space-3;
}

.nav-link {
  display: inline-flex;
  align-items: center;
  border: 0;
  background: transparent;
  color: $color-header-ink;
  font: inherit;
  font-weight: 700;
  text-decoration: none;
  padding: 0.35rem 0.15rem;
  cursor: pointer;
}

.product-heading h1 {
  font-size: clamp(1.6rem, 3vw, 2.2rem);
}

.product-heading p {
  color: $color-ink-muted;
}

.about-layer {
  position: fixed;
  inset: 0;
  z-index: $z-drawer;
  display: grid;
  place-items: center;
  padding: $space-4;
  background: rgb(7 21 39 / 48%);
}

.about {
  width: min(100%, 40rem);
  max-height: min(85vh, 46rem);
  overflow: auto;
}

.about__bar {
  justify-content: space-between;
}

.about__close,
.about__flow {
  margin: 0;
}

.about__close {
  border: 1px solid $color-border;
  border-radius: $radius-md;
  background: $color-surface;
  color: $color-ink;
  font: inherit;
  font-weight: 700;
  padding: 0.45rem 0.8rem;
  cursor: pointer;
}

.about__flow {
  padding-left: 1.2rem;
  display: grid;
  gap: $space-2;
}

.date-label {
  display: block;
  color: $color-ink-muted;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.workspace {
  display: grid;
  grid-template-columns: minmax(16rem, 22rem) minmax(0, 1fr);
  gap: $space-4;
  align-items: start;
}

.field {
  display: grid;
  gap: $space-2;
  font-weight: 700;
}

.field input,
.field select,
.crossing,
.tabs__tab,
button {
  font: inherit;
}

.field input,
.field select {
  width: 100%;
  min-width: 0;
  border: 1px solid $color-border;
  border-radius: $radius-md;
  padding: 0.65rem 0.75rem;
  background: $color-surface;
  color: $color-ink;
}

.crossing-list {
  display: grid;
  gap: $space-2;
}

.crossing {
  display: grid;
  gap: $space-2;
  width: 100%;
  min-width: 0;
  text-align: left;
  border: 1px solid $color-border;
  border-radius: $radius-md;
  padding: $space-3;
  background: $color-surface-raised;
  color: inherit;
  cursor: pointer;
}

.crossing[aria-pressed='true'] {
  border-color: $color-accent;
  background: $color-accent-soft;
}

.crossing__name {
  font-weight: 700;
}

.tabs {
  display: flex;
  flex-wrap: wrap;
  gap: $space-2;
}

.tabs__tab {
  border: 1px solid $color-border;
  border-radius: $radius-md;
  padding: 0.55rem 0.8rem;
  background: $color-surface;
  color: $color-ink;
  cursor: pointer;
}

.tabs__tab[aria-selected='true'] {
  background: $color-header;
  color: $color-header-ink;
  border-color: $color-header;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: $space-3;
  margin: 0;
  padding: 0;
  list-style: none;
}

.metrics li {
  display: grid;
  gap: $space-2;
  min-width: 0;
  padding: $space-3;
  border: 1px solid $color-border;
  border-radius: $radius-md;
  background: $color-surface-raised;
}

.timeline {
  border-top: 1px solid $color-border;
  padding-top: $space-2;
}

.timeline summary {
  cursor: pointer;
  font-weight: 700;
}

.locked {
  border: 1px solid $color-border;
  border-radius: $radius-md;
  padding: 0.7rem 1rem;
  background: $color-surface-raised;
  color: $color-ink-muted;
  font: inherit;
  font-weight: 700;
}

.locked:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.brand {
  display: flex;
  align-items: center;
  gap: $space-3;
  min-width: 0;
  color: inherit;
  text-decoration: none;
}

.brand__mark {
  display: grid;
  width: 2.5rem;
  height: 2.75rem;
  flex: none;
}

.brand__mark svg {
  width: 2.35rem;
  height: 2.6rem;
}

.brand__halo { fill: rgb(20 163 154 / 18%); }
.brand__shield { fill: $color-header; stroke: $color-nav-active; stroke-width: 1.2; }
.brand__stroke { fill: none; stroke: $color-header-ink; stroke-width: 1.35; }
.brand__accent { stroke: $color-nav-active; stroke-width: 1.6; stroke-linecap: round; }

.brand__text { display: grid; min-width: 0; }
.brand__name { font-weight: 700; }
.brand__tagline { color: $color-nav-muted; font-size: 0.75rem; }

@media (max-width: 48rem) {
  .workspace,
  .metrics {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 40rem) {
  .explorer__header {
    align-items: flex-start;
  }

  .brand__tagline {
    display: none;
  }

  .metrics {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
