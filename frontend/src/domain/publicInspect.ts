import { show } from '@/domain/displayCopy'

export interface InspectNote {
  title: string
  meta: string
  hint: string
  summary: string
  boundary: string
}

const NOTES: Record<string, InspectNote> = {
  navigation: {
    title: 'Product navigation',
    meta: 'BoundaryMesh / Navigation',
    hint: 'Public pages only',
    summary: 'These links move through the public BoundaryMesh console. The same product pages stay available while you explore.',
    boundary: 'Public pages only. The operator environment stays on its own protected page.',
  },
  overview: {
    title: 'Overview',
    meta: 'BoundaryMesh / Overview',
    hint: 'Synthetic public demo',
    summary: 'This introduces the question BoundaryMesh answers: whether a transaction may cross from one system into another.',
    boundary: 'Synthetic public demo. Nothing here can change a real cloud system.',
  },
  pipeline: {
    title: 'Enforcement pipeline',
    meta: 'BoundaryMesh / Pipeline',
    hint: '11 enforcement stages',
    summary: 'Every crossing is checked in a fixed order, from identity through policy, evidence, and delivery.',
    boundary: 'The steps describe the product. This public page does not call the private API.',
  },
  outcomes: {
    title: 'Decision badges',
    meta: 'BoundaryMesh / Outcomes',
    hint: 'Four decision results',
    summary: 'Allow, allow with transformation, quarantine, and reject are the only outcomes a crossing can receive.',
    boundary: 'The badges explain the model. They are not live production decisions.',
  },
  scenarios: {
    title: 'Crossing chooser',
    meta: 'BoundaryMesh / Scenarios',
    hint: 'Deterministic sample crossings',
    summary: 'Pick a prepared crossing and BoundaryMesh evaluates it in the browser. The result updates the selected decision.',
    boundary: 'Deterministic sample scenarios. Evaluation does not send a private request.',
  },
  evaluation: {
    title: 'Evaluation lab',
    meta: 'BoundaryMesh / Evaluation',
    hint: 'Synthetic decision lab',
    summary: 'This panel runs a synthetic crossing and shows the decision, reason, and evidence that follow from it.',
    boundary: 'Read only public demo. Operator actions stay unavailable.',
  },
  events: {
    title: 'Crossing list',
    meta: 'Public Demo / Boundary Events',
    hint: 'Synthetic runtime view',
    summary: 'Each row is a synthetic crossing from this browser session, with its decision and the systems it tried to connect.',
    boundary: 'Session sample data. Refreshing the page restores the seeded set on the public demo.',
  },
  connections: {
    title: 'Connections',
    meta: 'BoundaryMesh / Connections',
    hint: 'Named system links',
    summary: 'This shows how named systems meet BoundaryMesh, including the local Microsoft calendar harness.',
    boundary: 'No live tenant and no production connector is used on the public site.',
  },
  architecture: {
    title: 'Architecture',
    meta: 'BoundaryMesh / Architecture',
    hint: 'Hosted enforcement path',
    summary: 'This explains the console, the enforcement path, and the hosted shape from Hosting to the worker.',
    boundary: 'A description of the system. It does not open the private API.',
  },
  graph: {
    title: 'Boundary graph',
    meta: 'BoundaryMesh / Boundary Graph',
    hint: 'Synthetic system graph',
    summary: 'Nodes are systems. Edges are the security boundaries BoundaryMesh evaluates before anything may cross.',
    boundary: 'Synthetic graph data. It is not a live map of production services.',
  },
  crossing: {
    title: 'Full crossing',
    meta: 'BoundaryMesh / Full Crossing',
    hint: 'One prepared transaction',
    summary: 'This follows one transaction from the incoming payload through policy, transformation, and delivery.',
    boundary: 'A prepared public scenario. It cannot mutate cloud state.',
  },
  delivery: {
    title: 'Delivery',
    meta: 'BoundaryMesh / Delivery',
    hint: 'Synthetic delivery state',
    summary: 'Delivery is attempted only after the policy decision allows the crossing. A rejection stops before the worker.',
    boundary: 'Synthetic delivery state. The public site cannot queue real work.',
  },
  quarantine: {
    title: 'Quarantine',
    meta: 'BoundaryMesh / Quarantine',
    hint: 'Held synthetic crossings',
    summary: 'Quarantine holds a crossing that can be reviewed. The public page cannot release it.',
    boundary: 'Synthetic holds only. Operator release stays protected.',
  },
  evidence: {
    title: 'Evidence',
    meta: 'BoundaryMesh / Evidence',
    hint: 'Sample attestations',
    summary: 'Attestations and the audit chain record what BoundaryMesh decided and the order of those checks.',
    boundary: 'Sample evidence only. Production logs and operator sessions are not shown.',
  },
  attestations: {
    title: 'Attestations',
    meta: 'BoundaryMesh / Attestations',
    hint: 'Sample signed records',
    summary: 'Each attestation records the decision for one synthetic crossing.',
    boundary: 'Sample evidence only. Production logs and operator sessions are not shown.',
  },
  audit: {
    title: 'Audit',
    meta: 'BoundaryMesh / Audit',
    hint: 'Sample audit chain',
    summary: 'The audit chain records the order of synthetic checks for a crossing.',
    boundary: 'Sample evidence only. Production logs and operator sessions are not shown.',
  },
  status: {
    title: 'Project status',
    meta: 'BoundaryMesh / Status',
    hint: 'Reviewed project evidence',
    summary: 'These labels summarize reviewed project evidence. They are not a live operations feed.',
    boundary: 'Public explanation. Cloud health and operator controls stay protected.',
  },
  about: {
    title: 'About the project',
    meta: 'BoundaryMesh / About',
    hint: 'Public inspector context',
    summary: 'This page explains the product, the security boundary, and the public tools at the bottom of the screen.',
    boundary: 'Recruiter facing context. It grants no operator authority.',
  },
}

const FALLBACK: InspectNote = {
  title: 'Public section',
  meta: 'BoundaryMesh / Section',
  hint: 'Synthetic public demo',
  summary: 'This part of the BoundaryMesh console explains one piece of the synthetic demo.',
  boundary: 'Synthetic and read only. Private API access and operator authority stay unavailable.',
}

export function inspectNote(id: string | null): InspectNote {
  const note = (id && NOTES[id]) || FALLBACK
  return {
    title: show(note.title),
    meta: show(note.meta),
    hint: show(note.hint),
    summary: show(note.summary),
    boundary: show(note.boundary),
  }
}
