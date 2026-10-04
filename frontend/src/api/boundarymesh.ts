/**
 * Local development imports the Java API through the Vite alias in vite.config.ts.
 * A cloud build aliases this module path to syntheticPortfolio.ts instead.
 * This file stays the lab implementation for tools that resolve the path directly.
 */
export {
  evaluateBoundary,
  evaluateCrossing,
  evaluateIdentity,
  fetchAttestation,
  fetchAttestationTamperDemo,
  fetchAttestations,
  fetchAuditChain,
  fetchAuditChains,
  fetchAuditTamperDemo,
  fetchBoundaryGraph,
  fetchCredentialCatalog,
  fetchHealth,
  fetchQuarantineItem,
  fetchQuarantineList,
  fetchReady,
  issueLocalToken,
  reprocessQuarantine,
  rotateCoveCredential,
  runContractLab,
  runCredentialLab,
  runDeliveryLab,
  runEgressLab,
  runWebhookLab,
  verifyAttestation,
  verifyAuditChain,
} from './boundarymeshLive'
