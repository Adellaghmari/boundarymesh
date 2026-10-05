<script setup lang="ts">
import { show as $show } from '@/domain/displayCopy'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { inspectNote } from '@/domain/publicInspect'
import {
  buildSystemConsole,
  inspectorRoutes,
  type InspectorRoute,
  type SystemConsoleTab,
} from '@/domain/systemConsole'
import { OUTCOME_COPY } from '@/domain/labels'
import { useEventsStore } from '@/stores/events'
import type { BoundaryEventRecord } from '@/domain/types'

const open = ref(false)
const inspecting = ref(false)
const tool = ref<SystemConsoleTab>('overview')
const activeId = ref<string | null>(null)
const cardStyle = ref<Record<string, string>>({})
const pinned = ref<BoundaryEventRecord | null>(null)
const lineIndex = ref(0)
const stageIndex = ref(0)
const groupId = ref('runtime')
const paneWidth = ref(280)
const route = useRoute()
const router = useRouter()
const events = useEventsStore()
const { lastEvent, newestFirst } = storeToRefs(events)

const tools: { id: SystemConsoleTab; label: string }[] = [
  { id: 'overview', label: 'Overview' },
  { id: 'state', label: 'State' },
  { id: 'timeline', label: 'Timeline' },
  { id: 'pipeline', label: 'Pipeline' },
  { id: 'routes', label: 'Routes' },
  { id: 'system', label: 'System' },
  { id: 'about', label: 'About' },
]

const routes = computed(() => inspectorRoutes())
const activeEvent = computed(() => pinned.value ?? lastEvent.value ?? newestFirst.value[0] ?? null)
const model = computed(() =>
  buildSystemConsole({
    routeName: route.name,
    events: newestFirst.value,
    selected: activeEvent.value,
  }),
)
const note = computed(() => inspectNote(activeId.value))
const toolLabel = computed(() => tools.find((item) => item.id === tool.value)?.label ?? 'Overview')
const activeLine = computed(() => model.value.selectedLog[lineIndex.value] ?? null)
const activeStage = computed(() => model.value.stages[stageIndex.value] ?? null)
const activeGroup = computed(() => model.value.groups.find((group) => group.id === groupId.value) ?? model.value.groups[0])
const currentRoute = computed(() => routes.value.find((item) => item.name === route.name) ?? null)

watch(activeEvent, () => {
  lineIndex.value = 0
  stageIndex.value = 0
})

watch(lastEvent, () => {
  pinned.value = null
})

function close(): void {
  open.value = false
}

function toggleWorkspace(): void {
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
  if (!node || node.closest('.bm-inspector')) return null
  return node.closest<HTMLElement>('[data-inspect]')
}

function showTarget(target: HTMLElement): void {
  document.querySelectorAll('[data-inspected="true"]').forEach((node) => {
    if (node !== target) node.removeAttribute('data-inspected')
  })
  target.setAttribute('data-inspected', 'true')
  activeId.value = target.dataset.inspect ?? null
  const rect = target.getBoundingClientRect()
  const width = 220
  const left = Math.min(Math.max(8, rect.left), Math.max(8, window.innerWidth - width - 8))
  const top = rect.top > 42 ? rect.top - 34 : Math.min(rect.bottom + 6, window.innerHeight - 40)
  cardStyle.value = { left: `${left}px`, top: `${top}px`, maxWidth: `${width}px` }
}

function onPointer(event: Event): void {
  if (!inspecting.value) return
  const target = targetFrom(event)
  if (target) showTarget(target)
}

function onClick(event: MouseEvent): void {
  if (!inspecting.value) return
  const target = targetFrom(event)
  if (!target) return
  event.preventDefault()
  event.stopPropagation()
  showTarget(target)
}

function onKey(event: KeyboardEvent): void {
  if (event.key !== 'Escape') return
  if (inspecting.value) stopInspect()
  if (open.value) close()
}

function openRoute(item: InspectorRoute): void {
  void router.push(item.to)
}

function onResizeStart(event: PointerEvent): void {
  if (window.innerWidth < 768) return
  const origin = event.clientX
  const start = paneWidth.value
  const move = (ev: PointerEvent) => {
    paneWidth.value = Math.min(520, Math.max(200, start + ev.clientX - origin))
  }
  const stop = () => {
    window.removeEventListener('pointermove', move)
    window.removeEventListener('pointerup', stop)
  }
  window.addEventListener('pointermove', move)
  window.addEventListener('pointerup', stop)
}

onMounted(() => {
  document.addEventListener('keydown', onKey)
  document.addEventListener('pointerover', onPointer)
  document.addEventListener('click', onClick, true)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKey)
  document.removeEventListener('pointerover', onPointer)
  document.removeEventListener('click', onClick, true)
  stopInspect()
})
</script>

<template>
  <section
    v-show="open"
    id="system-console"
    class="workspace bm-inspector"
    role="region"
    aria-label="Public inspector"
    data-testid="system-console"
  >
    <nav class="rail" data-testid="inspector-rail" aria-label="Inspector tools">
      <button
        v-for="item in tools"
        :id="`system-console-tab-${item.id}`"
        :key="item.id"
        type="button"
        role="tab"
        :aria-selected="tool === item.id"
        :aria-label="item.label"
        :title="item.label"
        @click="tool = item.id"
      >
        <svg v-if="item.id === 'overview'" viewBox="0 0 16 16" aria-hidden="true">
          <rect x="2" y="2" width="5" height="5" rx="0.6" fill="none" stroke="currentColor" stroke-width="1.3" />
          <rect x="9" y="2" width="5" height="5" rx="0.6" fill="none" stroke="currentColor" stroke-width="1.3" />
          <rect x="2" y="9" width="5" height="5" rx="0.6" fill="none" stroke="currentColor" stroke-width="1.3" />
          <rect x="9" y="9" width="5" height="5" rx="0.6" fill="none" stroke="currentColor" stroke-width="1.3" />
        </svg>
        <svg v-else-if="item.id === 'state'" viewBox="0 0 16 16" aria-hidden="true">
          <path d="M5 3.2 2.4 8 5 12.8M11 3.2 13.6 8 11 12.8" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
        <svg v-else-if="item.id === 'timeline'" viewBox="0 0 16 16" aria-hidden="true">
          <path d="M3 3.5v9M3 5.2h7.2M3 8h5.2M3 10.8h8.2" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" />
        </svg>
        <svg v-else-if="item.id === 'pipeline'" viewBox="0 0 16 16" aria-hidden="true">
          <path d="M3 4h10M3 8h10M3 12h10" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" />
          <circle cx="6" cy="4" r="1.1" fill="currentColor" />
          <circle cx="10" cy="8" r="1.1" fill="currentColor" />
          <circle cx="7" cy="12" r="1.1" fill="currentColor" />
        </svg>
        <svg v-else-if="item.id === 'routes'" viewBox="0 0 16 16" aria-hidden="true">
          <circle cx="4" cy="4" r="1.4" fill="none" stroke="currentColor" stroke-width="1.3" />
          <circle cx="12" cy="12" r="1.4" fill="none" stroke="currentColor" stroke-width="1.3" />
          <path d="M5.2 5.2 10.6 10.6" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" />
        </svg>
        <svg v-else-if="item.id === 'system'" viewBox="0 0 16 16" aria-hidden="true">
          <path d="M8 1.8 12.6 3.6v3.8c0 2.8-1.8 4.7-4.6 6-2.8-1.3-4.6-3.2-4.6-6V3.6L8 1.8z" fill="none" stroke="currentColor" stroke-width="1.3" />
        </svg>
        <svg v-else viewBox="0 0 16 16" aria-hidden="true">
          <circle cx="8" cy="8" r="5.2" fill="none" stroke="currentColor" stroke-width="1.3" />
          <path d="M8 7.2V11M8 5.1v.1" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" />
        </svg>
      </button>
    </nav>

    <div class="main">
      <header class="bar">
        <span class="bar__mark" aria-hidden="true">
          <svg viewBox="0 0 16 16">
            <path d="M8 1.6 12.8 3.5v4c0 2.9-1.9 4.9-4.8 6.2C5.1 12.4 3.2 10.4 3.2 7.5v-4L8 1.6z" fill="none" stroke="currentColor" stroke-width="1.3" />
          </svg>
        </span>
        <strong>{{ toolLabel }}</strong>
        <span class="bar__mode">Synthetic demo</span>
        <button type="button" class="bar__close" aria-label="Close" @click="close">
          <svg viewBox="0 0 16 16" aria-hidden="true">
            <path d="M4.2 4.2 11.8 11.8M11.8 4.2 4.2 11.8" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" />
          </svg>
        </button>
      </header>

      <div class="split" data-testid="inspector-split" :style="{ '--pane': `${paneWidth}px` }">
        <div class="pane pane--list">
          <p class="pane__label">
            {{
              tool === 'overview'
                ? 'Crossings'
                : tool === 'state'
                  ? 'Groups'
                  : tool === 'timeline'
                    ? 'Events'
                    : tool === 'pipeline'
                      ? 'Stages'
                      : tool === 'routes'
                        ? 'Public routes'
                        : tool === 'system'
                          ? 'Boundary'
                          : 'Inspector'
            }}
          </p>
          <div class="pane__scroll">
            <template v-if="tool === 'overview'">
              <button
                v-for="(item, index) in newestFirst"
                :key="`${item.transactionId}-${item.outcome}-${index}`"
                type="button"
                class="row"
                :aria-current="item === activeEvent"
                @click="pinned = item"
              >
                <span>{{ $show(item.scenarioLabel) }}</span>
                <em>{{ $show(OUTCOME_COPY[item.outcome].label) }}</em>
              </button>
              <p v-if="newestFirst.length === 0" class="empty">No synthetic crossing yet.</p>
            </template>
            <template v-else-if="tool === 'state'">
              <button
                v-for="group in model.groups"
                :key="group.id"
                type="button"
                class="row"
                :aria-current="group.id === activeGroup?.id"
                @click="groupId = group.id"
              >
                <span>{{ group.label }}</span>
              </button>
            </template>
            <template v-else-if="tool === 'timeline'">
              <button
                v-for="(line, index) in model.selectedLog"
                :key="`${line.clock}-${index}`"
                type="button"
                class="row row--mono"
                :aria-current="index === lineIndex"
                @click="lineIndex = index"
              >
                <span>{{ line.clock }}</span>
                <em>{{ line.message }}</em>
              </button>
              <p v-if="model.selectedLog.length === 0" class="empty">No synthetic events yet.</p>
            </template>
            <template v-else-if="tool === 'pipeline'">
              <button
                v-for="(stage, index) in model.stages"
                :key="stage.label"
                type="button"
                class="row"
                :data-testid="`inspector-stage-${index}`"
                :aria-current="index === stageIndex"
                @click="stageIndex = index"
              >
                <span>{{ stage.label }}</span>
                <em>{{ stage.status }}</em>
              </button>
            </template>
            <template v-else-if="tool === 'routes'">
              <button
                v-for="item in routes"
                :key="item.to"
                type="button"
                class="row"
                :data-testid="`inspector-route-${item.name}`"
                :aria-current="route.name === item.name"
                @click="openRoute(item)"
              >
                <span>{{ item.label }}</span>
              </button>
            </template>
            <template v-else-if="tool === 'system'">
              <button type="button" class="row" aria-current="true">
                <span>boundary</span>
              </button>
            </template>
            <template v-else>
              <button type="button" class="row" aria-current="true">
                <span>inspector</span>
              </button>
            </template>
          </div>
        </div>
        <div class="handle" role="separator" aria-orientation="vertical" aria-label="Resize panes" @pointerdown="onResizeStart" />
        <div class="pane pane--detail">
          <p class="pane__label">
            {{
              tool === 'timeline'
                ? 'Event'
                : tool === 'pipeline'
                  ? 'Stage'
                  : tool === 'routes'
                    ? 'Route'
                    : tool === 'system'
                      ? 'Environment'
                      : tool === 'about'
                        ? 'Boundary'
                        : tool === 'state'
                          ? (activeGroup?.label ?? 'State')
                          : 'Selected crossing'
            }}
          </p>
          <div class="pane__scroll">
            <template v-if="tool === 'overview'">
              <div class="kv"><span>runtime mode</span><strong>{{ model.runtimeMode }}</strong></div>
              <div class="kv"><span>current page</span><strong>{{ model.routeLabel }}</strong></div>
              <div class="kv"><span>crossing</span><strong>{{ model.crossing }}</strong></div>
              <div class="kv"><span>decision</span><strong>{{ model.decision }}</strong></div>
              <div class="kv"><span>worker</span><strong>{{ model.worker }}</strong></div>
              <div class="kv"><span>delivery</span><strong>{{ model.delivery }}</strong></div>
              <div class="kv"><span>attestation</span><strong>{{ model.attestation }}</strong></div>
              <div v-for="(line, index) in model.selectedLog" :key="`${line.clock}-${index}`" class="kv">
                <span>{{ line.clock }}</span>
                <strong>{{ line.message }}</strong>
              </div>
            </template>
            <template v-else-if="tool === 'state' && activeGroup">
              <div v-for="field in activeGroup.fields" :key="field.key" class="kv">
                <span>{{ field.key }}</span>
                <strong>{{ field.value }}</strong>
              </div>
            </template>
            <template v-else-if="tool === 'timeline' && activeLine">
              <div class="kv"><span>clock</span><strong>{{ activeLine.clock }}</strong></div>
              <div class="kv"><span>message</span><strong>{{ activeLine.message }}</strong></div>
              <div class="kv"><span>crossing</span><strong>{{ model.crossing }}</strong></div>
              <div class="kv"><span>decision</span><strong>{{ model.decision }}</strong></div>
              <div class="kv"><span>boundary</span><strong>Synthetic public boundary</strong></div>
            </template>
            <template v-else-if="tool === 'pipeline' && activeStage">
              <div class="kv"><span>stage</span><strong>{{ activeStage.label }}</strong></div>
              <div class="kv"><span>status</span><strong>{{ activeStage.status }}</strong></div>
              <div class="kv"><span>input</span><strong>{{ activeStage.input }}</strong></div>
              <div class="kv"><span>result</span><strong>{{ activeStage.result }}</strong></div>
              <div class="kv"><span>impact</span><strong>{{ activeStage.impact }}</strong></div>
              <div class="kv"><span>boundary</span><strong>{{ activeStage.boundary }}</strong></div>
            </template>
            <template v-else-if="tool === 'routes'">
              <div class="kv"><span>name</span><strong>{{ currentRoute?.label ?? model.routeLabel }}</strong></div>
              <div class="kv"><span>path</span><strong>{{ currentRoute?.path ?? '/' }}</strong></div>
              <div class="kv"><span>public mode</span><strong>Synthetic demo</strong></div>
              <div class="kv"><span>auth required</span><strong>No</strong></div>
              <div class="kv"><span>private API</span><strong>Unavailable</strong></div>
            </template>
            <template v-else-if="tool === 'system'">
              <div class="kv"><span>product</span><strong>BoundaryMesh</strong></div>
              <div class="kv"><span>mode</span><strong>Public portfolio mode</strong></div>
              <div class="kv"><span>runtime</span><strong>Synthetic runtime</strong></div>
              <div class="kv"><span>authority</span><strong>Read only</strong></div>
              <div class="kv"><span>private API</span><strong>Private API unavailable</strong></div>
              <div class="kv"><span>operator</span><strong>Operator authority unavailable</strong></div>
              <div class="kv"><span>operator auth</span><strong>Firebase operator auth excluded</strong></div>
              <div class="kv"><span>mutation</span><strong>Cloud mutation unavailable</strong></div>
            </template>
            <template v-else>
              <p class="note">Inspect mode names the public section under the pointer.</p>
              <p class="note">This workspace shows synthetic state, events, pipeline execution, routes, and system boundaries.</p>
              <p class="note">Private APIs, operator authority, credentials, and production telemetry stay unavailable.</p>
            </template>
          </div>
        </div>
      </div>
    </div>
  </section>

  <div
    v-if="inspecting && activeId"
    class="tag bm-inspector"
    role="status"
    data-testid="public-inspect-card"
    :style="cardStyle"
  >
    <strong>{{ note.meta }}</strong>
    <span>{{ note.hint }}</span>
  </div>

  <div v-show="!open" class="pill bm-inspector" role="toolbar" aria-label="Public inspector">
    <button
      type="button"
      data-testid="system-console-anchor"
      :aria-expanded="open"
      aria-controls="system-console"
      title="Open public inspector"
      aria-label="Open public inspector"
      @click="toggleWorkspace"
    >
      <svg viewBox="0 0 16 16" aria-hidden="true">
        <path d="M8 1.6 12.8 3.5v4c0 2.9-1.9 4.9-4.8 6.2C5.1 12.4 3.2 10.4 3.2 7.5v-4L8 1.6z" fill="none" stroke="currentColor" stroke-width="1.3" />
      </svg>
    </button>
    <span class="pill__rule" aria-hidden="true" />
    <button
      type="button"
      data-testid="public-inspect-anchor"
      :aria-pressed="inspecting"
      title="Inspect page"
      aria-label="Inspect page"
      @click="toggleInspect"
    >
      <svg viewBox="0 0 16 16" aria-hidden="true">
        <circle cx="8" cy="8" r="2.6" fill="none" stroke="currentColor" stroke-width="1.3" />
        <path d="M8 1.6v2.1M8 12.3v2.1M1.6 8h2.1M12.3 8h2.1" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" />
      </svg>
    </button>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.workspace {
  @include reduced-motion;
  position: fixed;
  z-index: $z-drawer + 16;
  left: 0;
  right: 0;
  bottom: 0;
  height: 62vh;
  min-height: 320px;
  max-height: 68vh;
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  overflow: hidden;
  border-top: 1px solid #2a2a2e;
  background: #101012;
  color: #d5d5dc;
  font-family: ui-sans-serif, system-ui, sans-serif;
  font-size: 12px;
  line-height: 1.35;
  color-scheme: dark;
}

.rail {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 6px 4px;
  background: #09090b;
  border-right: 1px solid #242428;
}

.rail button,
.bar__close,
.row {
  border: 0;
  cursor: pointer;
}

.rail button {
  display: grid;
  place-items: center;
  width: 36px;
  height: 32px;
  border-radius: 6px;
  background: transparent;
  color: #9a9aa3;
}

.rail button[aria-selected='true'] {
  background: #1c1c22;
  color: #f5f5f7;
  box-shadow: inset 2px 0 0 #3ddc97;
}

.rail svg,
.bar__mark svg,
.bar__close svg,
.pill svg {
  width: 16px;
  height: 16px;
  display: block;
}

.main {
  min-width: 0;
  min-height: 0;
  display: grid;
  grid-template-rows: 34px minmax(0, 1fr);
}

.bar {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 0 8px 0 10px;
  border-bottom: 1px solid #2a2a2e;
  background: #141416;
}

.bar__mark {
  display: grid;
  place-items: center;
  color: #3ddc97;
}

.bar strong {
  font-size: 12px;
  font-weight: 600;
  color: #f2f2f4;
}

.bar__mode {
  color: #8d8d96;
  font-size: 11px;
}

.bar__close {
  margin-left: auto;
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 4px;
  background: transparent;
  color: #c8c8d0;
}

.bar__close:hover,
.rail button:hover,
.row:hover,
.pill button:hover {
  background: #1c1c22;
}

.split {
  min-height: 0;
  display: grid;
  grid-template-columns: var(--pane, 280px) 5px minmax(0, 1fr);
}

.pane {
  min-width: 0;
  min-height: 0;
  display: grid;
  grid-template-rows: 24px minmax(0, 1fr);
  background: #101012;
}

.pane--list {
  background: #0e0e11;
  border-right: 1px solid #242428;
}

.pane__label {
  margin: 0;
  padding: 5px 10px 0;
  color: #8b8b95;
  font-size: 11px;
}

.pane__scroll {
  min-height: 0;
  overflow: auto;
}

.handle {
  cursor: col-resize;
  background: #1a1a1e;
}

.handle:hover {
  background: #3ddc97;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  height: 26px;
  padding: 0 10px;
  background: transparent;
  color: #c9c9d1;
  font-size: 12px;
  text-align: left;
}

.row span,
.row em {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row em {
  flex: none;
  max-width: 46%;
  color: #8e8e98;
  font-style: normal;
  font-family: $font-mono;
  font-size: 11px;
}

.row--mono span,
.row--mono em {
  font-family: $font-mono;
}

.row--mono em {
  flex: 1;
  max-width: none;
  color: #d0d0d6;
}

.row[aria-current='true'] {
  background: #173128;
  color: #f3fff8;
  box-shadow: inset 2px 0 0 #3ddc97;
}

.kv {
  display: grid;
  grid-template-columns: 132px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-bottom: 1px solid #1c1c21;
}

.kv span {
  color: #8d8d96;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kv strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
  font-family: $font-mono;
  font-size: 12px;
  color: #e7e7ee;
}

.note,
.empty {
  margin: 0;
  padding: 8px 10px;
  color: #b7b7c0;
  font-size: 12px;
}

.tag {
  position: fixed;
  z-index: $z-drawer + 24;
  display: grid;
  gap: 1px;
  padding: 3px 7px 4px;
  border: 1px solid #3ddc97;
  border-radius: 2px;
  background: #111113;
  color: #f4f4f5;
  font-size: 11px;
  line-height: 1.25;
  pointer-events: none;
}

.tag strong {
  color: #3ddc97;
  font-weight: 600;
}

.tag span {
  color: #d0d0d6;
}

.pill {
  @include reduced-motion;
  position: fixed;
  z-index: $z-drawer + 18;
  left: 50%;
  bottom: 16px;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 2px;
  height: 36px;
  padding: 3px;
  border: 1px solid #2a2a2a;
  border-radius: 999px;
  background: #050505;
  box-shadow: 0 6px 16px rgb(0 0 0 / 45%);
}

.pill__rule {
  width: 1px;
  height: 14px;
  background: #333338;
}

.pill button {
  display: grid;
  place-items: center;
  width: 32px;
  height: 30px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #f2f2f2;
  cursor: pointer;
}

.pill button[aria-pressed='true'] {
  color: #3ddc97;
}

.rail button:focus-visible,
.bar__close:focus-visible,
.row:focus-visible,
.pill button:focus-visible {
  @include focus-ring;
}

@media (max-width: 48rem) {
  .workspace {
    height: 100dvh;
    max-height: none;
    min-height: 0;
  }

  .split {
    grid-template-columns: minmax(0, 1fr) !important;
    grid-template-rows: minmax(0, 42%) minmax(0, 1fr);
  }

  .handle {
    display: none;
  }

  .pane--list {
    border-right: 0;
    border-bottom: 1px solid #242428;
  }
}
</style>

<style lang="scss">
html.bm-inspect,
html.bm-inspect body {
  cursor: crosshair;
}

html.bm-inspect [data-inspected='true'] {
  outline: 1px solid #3ddc97;
  outline-offset: 0;
  background-color: rgb(61 220 151 / 8%);
}

html.bm-inspect .bm-inspector,
html.bm-inspect .bm-inspector * {
  cursor: pointer;
}
</style>
