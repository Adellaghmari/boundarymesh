/**
 * Google Cloud infrastructure adapters.
 *
 * <p>Phase 15F-A implements Firestore persistence adapters behind existing ports, plus the Phase
 * 15E Secret Manager adapters. Pub/Sub, Cloud KMS, and the delivery outbox are not implemented
 * here. Domain and application must not depend on this package. API and worker do not depend on
 * this package in Phase 15F-A.
 */
package dev.adellaghmari.boundarymesh.gcp;
