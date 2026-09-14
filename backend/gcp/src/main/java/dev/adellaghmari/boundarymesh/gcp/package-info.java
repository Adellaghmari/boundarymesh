/**
 * Google Cloud infrastructure adapters.
 *
 * <p>Phase 15H-A adds the Cloud KMS attestation signer behind existing ports. Secret Manager,
 * Firestore, and Pub/Sub adapters remain implemented and not default-wired. Domain and application
 * must not depend on this package. API and worker do not depend on this package in Phase 15H-A.
 */
package dev.adellaghmari.boundarymesh.gcp;
