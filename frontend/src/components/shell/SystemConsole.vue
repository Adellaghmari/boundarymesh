<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { buildSystemConsole, type SystemConsoleTab } from '@/domain/systemConsole'
import { useEventsStore } from '@/stores/events'

const open = ref(false)
const tab = ref<SystemConsoleTab>('overview')
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

const tabs: { id: SystemConsoleTab; label: string }[] = [
  { id: 'overview', label: 'Overview' },
  { id: 'events', label: 'Events' },
  { id: 'pipeline', label: 'Pipeline' },
  { id: 'state', label: 'State' },
  { id: 'system', label: 'System' },
]

function toggle(): void {
  open.value = !open.value
}

function close(): void {
  open.value = false
}

function onKey(event: KeyboardEvent): void {
  if (event.key === 'Escape' && open.value) {
    close()
  }
}

onMounted(() => {
  document.addEventListener('keydown', onKey)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKey)
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
        <p class="drawer__title">System Console</p>
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
        <div v-if="tab === 'overview'" class="facts">
          <p><span>Runtime mode</span> {{ $show(model.runtimeMode) }}</p>
          <p><span>Current page</span> {{ $show(model.routeLabel) }}</p>
          <p><span>Selected crossing</span> {{ $show(model.crossing) }}</p>
          <p><span>Current decision</span> {{ $show(model.decision) }}</p>
          <p><span>Worker state</span> {{ $show(model.worker) }}</p>
          <p><span>Delivery state</span> {{ $show(model.delivery) }}</p>
          <p><span>Attestation state</span> {{ $show(model.attestation) }}</p>
          <ol v-if="model.log.length" class="log" aria-label="Synthetic event log">
            <li v-for="(line, index) in model.selectedLog" :key="`${line.clock}-${index}`">
              <span>[{{ line.clock }}]</span> {{ $show(line.message) }}
            </li>
          </ol>
          <p v-else class="muted">The synthetic demo has not recorded a crossing yet.</p>
        </div>
        <ol v-else-if="tab === 'events'" class="log" aria-label="Synthetic event stream">
          <li v-for="(line, index) in model.log" :key="`${line.clock}-${line.message}-${index}`">
            <span>[{{ line.clock }}]</span> {{ $show(line.message) }}
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
        <div v-else class="system">
          <p>BoundaryMesh</p>
          <p>Public portfolio mode</p>
          <p>Synthetic data</p>
          <p>Read only</p>
          <p>Private API unavailable</p>
          <p>Operator authority unavailable</p>
        </div>
      </div>
    </section>
    <button
      type="button"
      class="anchor"
      data-testid="system-console-anchor"
      :aria-expanded="open"
      aria-controls="system-console"
      @click="toggle"
    >
      <span class="anchor__label">System Console</span>
      <svg viewBox="0 0 16 16" aria-hidden="true">
        <path d="M3 6.5 8 11l5-4.5" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
      </svg>
    </button>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.dock {
  @include reduced-motion;
  position: fixed;
  z-index: $z-drawer + 10;
  left: 50%;
  bottom: 0;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  width: max-content;
  max-width: calc(100vw - 1.5rem);
  pointer-events: none;
}

.dock--open {
  width: min(48rem, calc(100vw - 1.5rem));
}

.anchor,
.drawer,
.drawer__close,
.tabs__item {
  pointer-events: auto;
}

.anchor {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: $space-2;
  height: 1.85rem;
  padding: 0 $space-3 0 $space-2;
  border: 1px solid rgb(255 255 255 / 14%);
  border-bottom: 0;
  border-radius: 999px 999px 0 0;
  background: #121212;
  color: #7ee8df;
  cursor: pointer;
  box-shadow: 0 -6px 18px rgb(0 0 0 / 28%);
}

.anchor svg {
  width: 0.85rem;
  height: 0.85rem;
}

.dock--open .anchor svg {
  transform: rotate(180deg);
}

.anchor__label {
  font-family: $font-mono;
  font-size: 0.72rem;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.anchor:focus-visible,
.drawer__close:focus-visible,
.tabs__item:focus-visible {
  @include focus-ring;
}

.drawer {
  width: 100%;
  height: min(60vh, calc(100vh - 4.5rem));
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid rgb(255 255 255 / 12%);
  border-bottom: 0;
  border-radius: 0.7rem 0.7rem 0 0;
  background: #121212;
  color: #f5f7f6;
  box-shadow: 0 -18px 40px rgb(0 0 0 / 35%);
}

.drawer__bar,
.tabs,
.facts p,
.pipeline li,
.system p,
.facts div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: $space-3;
}

.drawer__bar {
  padding: $space-3 $space-4;
  border-bottom: 1px solid rgb(255 255 255 / 8%);
}

.drawer__title {
  margin: 0;
  font-family: $font-mono;
  font-size: 0.78rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #7ee8df;
}

.drawer__close,
.tabs__item {
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
}

.drawer__close {
  font-size: 0.85rem;
  color: #d5e4e3;
}

.tabs {
  justify-content: flex-start;
  gap: $space-1;
  padding: $space-2 $space-3;
  overflow-x: auto;
  border-bottom: 1px solid rgb(255 255 255 / 8%);
}

.tabs__item {
  flex: none;
  padding: $space-2 $space-3;
  border-radius: $radius-sm;
  color: #b7c7c6;
  font-size: 0.78rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.tabs__item[aria-selected='true'] {
  background: rgb(20 163 154 / 18%);
  color: #7ee8df;
}

.drawer__body {
  min-height: 0;
  overflow: auto;
  padding: $space-4;
}

.facts,
.pipeline,
.log,
.system {
  margin: 0;
  padding: 0;
  list-style: none;
}

.facts p,
.facts div,
.pipeline li,
.system p,
.log li {
  margin: 0;
  padding: $space-2 0;
  border-bottom: 1px solid rgb(255 255 255 / 6%);
  font-size: 0.92rem;
}

.facts span,
.facts dt,
.pipeline span,
.log span {
  color: #8fb8b6;
  font-family: $font-mono;
  font-size: 0.78rem;
}

.facts dd,
.pipeline strong {
  margin: 0;
  font-weight: 600;
}

.log li {
  justify-content: flex-start;
  font-family: $font-mono;
  font-size: 0.82rem;
}

.muted {
  color: #8fb8b6;
}
</style>
