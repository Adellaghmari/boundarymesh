import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { ROUTE_NAMES } from '@/router'
import { EVIDENCE_NAV, LAB_NAV, OPERATIONS_NAV, PRIMARY_NAV, STATUS_NAV } from '@/config/navigation'

export function useActiveNav() {
  const route = useRoute()

  function isActive(name: string): boolean {
    if (name === ROUTE_NAMES.boundaryEvents) {
      return (
        route.name === ROUTE_NAMES.boundaryEvents || route.name === ROUTE_NAMES.boundaryEventDetail
      )
    }
    if (name === ROUTE_NAMES.quarantine) {
      return route.name === ROUTE_NAMES.quarantine || route.name === ROUTE_NAMES.quarantineDetail
    }
    if (name === ROUTE_NAMES.audit) {
      return route.name === ROUTE_NAMES.audit || route.name === ROUTE_NAMES.auditDetail
    }
    if (name === ROUTE_NAMES.attestations) {
      return route.name === ROUTE_NAMES.attestations || route.name === ROUTE_NAMES.attestationDetail
    }
    return route.name === name
  }

  function groupActive(names: readonly string[]): boolean {
    return names.some((name) => isActive(name))
  }

  const labsActive = computed(() => groupActive(LAB_NAV.map((item) => item.name)))
  const operationsActive = computed(() => groupActive(OPERATIONS_NAV.map((item) => item.name)))
  const evidenceActive = computed(() => groupActive(EVIDENCE_NAV.map((item) => item.name)))
  const primaryActive = computed(() => groupActive(PRIMARY_NAV.map((item) => item.name)))
  const statusActive = computed(() => isActive(STATUS_NAV.name))

  return {
    isActive,
    labsActive,
    operationsActive,
    evidenceActive,
    primaryActive,
    statusActive,
  }
}
