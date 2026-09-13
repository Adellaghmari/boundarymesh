/**
 * Cloud-agnostic transactional delivery outbox. Persist first, publish second. At-least-once
 * transport plus application idempotency. Google Pub/Sub types do not belong here.
 */
package dev.adellaghmari.boundarymesh.application.outbox;
