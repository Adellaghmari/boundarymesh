/**
 * Google Cloud infrastructure adapters.
 *
 * <p>Phase 15E implements Secret Manager adapters behind existing secret ports. Production
 * Firestore, Pub/Sub, and Cloud KMS clients are not implemented here. Domain and application must
 * not depend on this package. API and worker do not depend on this package in Phase 15E.
 */
package dev.adellaghmari.boundarymesh.gcp;
