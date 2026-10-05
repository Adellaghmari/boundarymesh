<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { inspectNote } from '@/domain/publicInspect'
import { buildSystemConsole, type SystemConsoleTab } from '@/domain/systemConsole'
import { useEventsStore } from '@/stores/events'

const open = ref(false)
const inspecting = ref(false)
const tab = ref<SystemConsoleTab>('overview')
const activeId = ref<string | null>(null)
const cardStyle = ref<Record<string, string>>({})
const route = useRoute()
const events = useEventsStore()
const { lastEvent, newestFirst } = storeToRefs(events)

const model = computed(() =>
  buildSystemConsole({
    routeName: route.name,
    events: newestFirst.value,
    selected: lastEvent.value,
  }),
)

const note = computed(() => inspectNote(activeId.value))

const tabs: { id: SystemConsoleTab; label: string }[] = [
  { id: 'overview', label: 'Overview' },
  { id: 'events', label: 'Events' },
  { id: 'pipeline', label: 'Pipeline' },
  { id: 'state', label: 'State' },
  { id: 'system', label: 'System' },
]

function close(): void {
  open.value = false
}

function toggleConsole(): void {
  open.value = !open.value
  if (open.value) stopInspect()
}

function stopInspect(): void {
  inspecting.value = false
  activeId.value = null
  document.documentElement.classList.remove('bm-inspect')
  document.querySelectorAll('[data-inspected="true"]').forEach((node) => node.removeAttribute('data-inspected'))
}

function toggleInspect(): void {
  if (inspecting.value) {
    stopInspect()
    return
  }
  close()
  inspecting.value = true
  document.documentElement.classList.add('bm-inspect')
}

function targetFrom(event: Event): HTMLElement | null {
  const node = event.target instanceof Element ? event.target : null
  if (!node || node.closest('.dock')) return null
  return node.closest<HTMLElement>('[data-inspect]')
}

function showTarget(target: HTMLElement): void {
  document.querySelectorAll('[data-inspected="true"]').forEach((node) => {
    if (node !== target) node.removeAttribute('data-inspected')
  })
  target.setAttribute('data-inspected', 'true')
  activeId.value = target.dataset.inspect ?? null
  const rect = target.getBoundingClientRect()
  const width = Math.min(320, window.innerWidth - 24)
  const left = Math.min(Math.max(12, rect.left), window.innerWidth - width - 12)
  const below = rect.bottom + 12
  const top = below > window.innerHeight - 168 ? Math.max(12, rect.top - 156) : below
  cardStyle.value = { left: `${left}px`, top: `${top}px`, width: `${width}px` }
}

function onPointer(event: Event): void {
  if (!inspecting.value) return
  const target = targetFrom(event)
  if (target) showTarget(target)
}

function onKey(event: KeyboardEvent): void {
  if (event.key !== 'Escape') return
  if (inspecting.value) stopInspect()
  if (open.value) close()
}

onMounted(() => {
  document.addEventListener('keydown', onKey)
  document.addEventListener('pointerover', onPointer)
  document.addEventListener('click', onPointer)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKey)
  document.removeEventListener('pointerover', onPointer)
  document.removeEventListener('click', onPointer)
  stopInspect()
})
</script>

<template>
  <div class="dock" :class="{ 'dock--open': open }">
    <section
      v-show="open"
      id="system-console"
      class="drawer"
      role="dialog"
      aria-label="System Console"
      data-testid="system-console"
    >
      <header class="drawer__bar">
        <div>
          <p class="drawer__kicker">Synthetic runtime</p>
          <p class="drawer__title">System Console</p>
        </div>
        <button type="button" class="drawer__close" @click="close">Close</button>
      </header>
      <div class="tabs" role="tablist" aria-label="System Console sections">
        <button
          v-for="item in tabs"
          :id="`system-console-tab-${item.id}`"
          :key="item.id"
          type="button"
          role="tab"
          class="tabs__item"
          :aria-selected="tab === item.id"
          :tabindex="tab === item.id ? 0 : -1"
          @click="tab = item.id"
        >
          {{ item.label }}
        </button>
      </div>
      <div class="drawer__body" role="tabpanel" :aria-labelledby="`system-console-tab-${tab}`">
        <div v-if="tab === 'overview'" class="panel">
          <dl class="facts">
            <div><dt>Runtime mode</dt><dd>{{ $show(model.runtimeMode) }}</dd></div>
            <div><dt>Current page</dt><dd>{{ $show(model.routeLabel) }}</dd></div>
            <div><dt>Selected crossing</dt><dd>{{ $show(model.crossing) }}</dd></div>
            <div><dt>Current decision</dt><dd>{{ $show(model.decision) }}</dd></div>
            <div><dt>Worker state</dt><dd>{{ $show(model.worker) }}</dd></div>
            <div><dt>Delivery state</dt><dd>{{ $show(model.delivery) }}</dd></div>
            <div><dt>Attestation state</dt><dd>{{ $show(model.attestation) }}</dd></div>
          </dl>
          <ol v-if="model.selectedLog.length" class="log" aria-label="Synthetic event log">
            <li v-for="(line, index) in model.selectedLog" :key="`${line.clock}-${index}`">
              <span>{{ line.clock }}</span> {{ $show(line.message) }}
            </li>
          </ol>
          <p v-else class="muted">The synthetic demo has not recorded a crossing yet.</p>
        </div>
        <ol v-else-if="tab === 'events'" class="log" aria-label="Synthetic event stream">
          <li v-for="(line, index) in model.log" :key="`${line.clock}-${line.message}-${index}`">
            <span>{{ line.clock }}</span> {{ $show(line.message) }}
          </li>
          <li v-if="model.log.length === 0">No synthetic events yet.</li>
        </ol>
        <ol v-else-if="tab === 'pipeline'" class="pipeline" aria-label="Pipeline state">
          <li v-for="stage in model.pipeline" :key="stage.label">
            <span>{{ $show(stage.label) }}</span>
            <strong>{{ $show(stage.status) }}</strong>
          </li>
          <li v-if="model.pipeline.length === 0">No pipeline state yet.</li>
        </ol>
        <dl v-else-if="tab === 'state'" class="facts">
          <div v-for="fact in model.state" :key="fact.label">
            <dt>{{ $show(fact.label) }}</dt>
            <dd>{{ $show(fact.value) }}</dd>
          </div>
        </dl>
        <ul v-else class="system">
          <li>BoundaryMesh</li>
          <li>Public portfolio mode</li>
          <li>Synthetic data</li>
          <li>Read only</li>
          <li>Private API unavailable</li>
          <li>Operator authority unavailable</li>
        </ul>
      </div>
    </section>

    <div v-if="inspecting && activeId" class="card" role="status" data-testid="public-inspect-card" :style="cardStyle">
      <p class="card__kicker">Inspect</p>
      <p class="card__title">{{ note.title }}</p>
      <p>{{ note.summary }}</p>
      <p class="card__boundary">{{ note.boundary }}</p>
    </div>

    <div class="launcher" role="toolbar" aria-label="Public demo tools">
      <button
        type="button"
        class="launcher__button"
        data-testid="public-inspect-anchor"
        :aria-pressed="inspecting"
        @click="toggleInspect"
      >
        <svg viewBox="0 0 16 16" aria-hidden="true">
          <circle cx="8" cy="8" r="3.2" fill="none" stroke="currentColor" stroke-width="1.4" />
          <path d="M8 1.5v2.2M8 12.3v2.2M1.5 8h2.2M12.3 8h2.2" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
        </svg>
        <span>Inspect</span>
      </button>
      <button
        type="button"
        class="launcher__button launcher__button--console"
        data-testid="system-console-anchor"
        :aria-expanded="open"
        aria-controls="system-console"
        @click="toggleConsole"
      >
        <svg viewBox="0 0 16 16" aria-hidden="true">
          <rect x="2" y="3" width="12" height="10" rx="1.5" fill="none" stroke="currentColor" stroke-width="1.4" />
          <path d="M4.5 7.2h4M4.5 9.6h2.5" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
        </svg>
        <span>Console</span>
      </button>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.dock {
  @include reduced-motion;
  position: fixed;
  z-index: $z-drawer + 12;
  left: 50%;
  bottom: 0.85rem;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  width: max-content;
  max-width: calc(100vw - 1.25rem);
  pointer-events: none;
}

.dock--open {
  width: min(40rem, calc(100vw - 1.25rem));
}

.launcher,
.drawer,
.card,
.drawer__close,
.tabs__item {
  pointer-events: auto;
}

.launcher {
  display: flex;
  align-items: center;
  gap: 0.2rem;
  padding: 0.22rem;
  border: 1px solid rgb(126 232 223 / 22%);
  border-radius: 999px;
  background: #101820;
  color: #e8f6f5;
  box-shadow: 0 14px 36px rgb(4 12 20 / 38%);
  backdrop-filter: blur(10px);
}

.launcher__button {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  height: 2rem;
  padding: 0 0.85rem;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #d7ebe9;
  cursor: pointer;
  font-size: 0.78rem;
  letter-spacing: 0.04em;
}

.launcher__button svg {
  width: 0.9rem;
  height: 0.9rem;
}

.launcher__button[aria-pressed='true'],
.launcher__button[aria-expanded='true'] {
  background: rgb(20 163 154 / 22%);
  color: #9ff3ea;
}

.launcher__button:focus-visible,
.drawer__close:focus-visible,
.tabs__item:focus-visible {
  @include focus-ring;
}

.drawer {
  width: 100%;
  height: min(26rem, calc(100vh - 6.5rem));
  margin-bottom: 0.55rem;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid rgb(255 255 255 / 10%);
  border-radius: 1rem;
  background: #101820;
  color: #f4f7f6;
  box-shadow: 0 22px 50px rgb(4 12 20 / 42%);
}

.drawer__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: $space-3;
  padding: 0.9rem 1rem 0.7rem;
}

.drawer__kicker,
.card__kicker {
  margin: 0;
  color: #8fd9d2;
  font-size: 0.68rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.drawer__title,
.card__title {
  margin: 0.1rem 0 0;
  font-size: 1rem;
  font-weight: 650;
}

.drawer__close {
  border: 0;
  background: transparent;
  color: #d5e4e3;
  cursor: pointer;
  font-size: 0.82rem;
}

.tabs {
  display: flex;
  gap: 0.25rem;
  padding: 0 0.75rem 0.7rem;
  overflow-x: auto;
}

.tabs__item {
  flex: none;
  padding: 0.35rem 0.7rem;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #b7c9c8;
  cursor: pointer;
  font-size: 0.75rem;
}

.tabs__item[aria-selected='true'] {
  background: rgb(255 255 255 / 8%);
  color: #f7fbfb;
}

.drawer__body {
  min-height: 0;
  overflow: auto;
  padding: 0.2rem 1rem 1rem;
}

.facts,
.pipeline,
.log,
.system {
  margin: 0;
  padding: 0;
  list-style: none;
}

.facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.65rem 1rem;
}

.facts div {
  min-width: 0;
}

.facts dt,
.pipeline span,
.log span {
  color: #8fb8b6;
  font-size: 0.72rem;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.facts dd,
.pipeline strong {
  margin: 0.15rem 0 0;
  font-weight: 600;
}

.pipeline li,
.log li,
.system li {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: $space-3;
  padding: 0.45rem 0;
}

.log {
  margin-top: 0.8rem;
}

.log li {
  justify-content: flex-start;
  font-family: $font-mono;
  font-size: 0.78rem;
}

.log span {
  font-family: $font-mono;
  text-transform: none;
  letter-spacing: 0;
}

.system li {
  justify-content: flex-start;
}

.muted,
.card__boundary {
  color: #9eb8b6;
}

.card {
  position: fixed;
  z-index: $z-drawer + 20;
  display: grid;
  gap: 0.35rem;
  padding: 0.85rem 0.95rem;
  border: 1px solid rgb(126 232 223 / 24%);
  border-radius: 0.9rem;
  background: #101820;
  color: #f4f7f6;
  box-shadow: 0 18px 40px rgb(4 12 20 / 36%);
}

.card p {
  margin: 0;
  font-size: 0.88rem;
}

.card__boundary {
  font-size: 0.78rem;
}

@media (max-width: 48rem) {
  .facts {
    grid-template-columns: 1fr;
  }
}
</style>

<style lang="scss">
html.bm-inspect,
html.bm-inspect body {
  cursor: crosshair;
}

html.bm-inspect [data-inspected='true'] {
  outline: 2px solid rgb(126 232 223 / 85%);
  outline-offset: 4px;
  border-radius: 0.6rem;
}

html.bm-inspect .dock,
html.bm-inspect .dock * {
  cursor: pointer;
}
</style>
