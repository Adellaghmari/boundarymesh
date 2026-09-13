/**
 * Google Cloud infrastructure adapters.
 *
 * <p>Phase 15F-A implements Firestore persistence adapters behind existing ports, plus the Phase
 * 15E Secret Manager adapters, the Phase 15F Firestore adapters, and the Phase 15G outbox / Pub/Sub
 * adapters. Cloud KMS is not implemented here. Domain and application must not depend on this
 * package. API and worker do not depend on this package in Phase 15G-A.
 */
package dev.adellaghmari.boundarymesh.gcp;
