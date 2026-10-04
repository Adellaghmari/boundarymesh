import type { Component } from 'vue'
import {
  Activity,
  BadgeCheck,
  BookOpen,
  FileCheck,
  Home,
  KeyRound,
  Layers,
  LayoutGrid,
  Link,
  Network,
  PackageCheck,
  ScrollText,
  Share2,
  ShieldCheck,
  TriangleAlert,
  UserRoundCheck,
  Webhook,
} from '@lucide/vue'
import { ROUTE_NAMES } from '@/router'

export const NAV_ICONS: Record<string, Component> = {
  [ROUTE_NAMES.overview]: Home,
  [ROUTE_NAMES.boundaryEvents]: Activity,
  [ROUTE_NAMES.connections]: Link,
  [ROUTE_NAMES.architecture]: Network,
  [ROUTE_NAMES.boundaryGraph]: Share2,
  [ROUTE_NAMES.crossing]: ShieldCheck,
  [ROUTE_NAMES.identity]: UserRoundCheck,
  [ROUTE_NAMES.webhooks]: Webhook,
  [ROUTE_NAMES.contracts]: FileCheck,
  [ROUTE_NAMES.dataBoundary]: Layers,
  [ROUTE_NAMES.delivery]: PackageCheck,
  [ROUTE_NAMES.credentials]: KeyRound,
  [ROUTE_NAMES.quarantine]: TriangleAlert,
  [ROUTE_NAMES.audit]: ScrollText,
  [ROUTE_NAMES.attestations]: BadgeCheck,
  [ROUTE_NAMES.status]: Activity,
  [ROUTE_NAMES.about]: BookOpen,
  labs: LayoutGrid,
}

export function navIcon(name: string): Component {
  return NAV_ICONS[name] ?? Activity
}
