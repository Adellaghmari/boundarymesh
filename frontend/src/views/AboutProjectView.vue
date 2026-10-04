<script setup lang="ts">
import TruthBanner from '@/components/TruthBanner.vue'
</script>

<template>
  <div class="stack" data-testid="about-project">
    <header class="stack">
      <div class="cluster">
        <h1>About the project</h1>
        <TruthBanner />
      </div>
      <p>This page sits inside the BoundaryMesh console. It explains the product that the rest of the navigation explores.</p>
    </header>

    <section class="card stack">
      <h2>What BoundaryMesh is</h2>
      <p>
        BoundaryMesh decides whether one system may exchange data or an action with another. A successful HTTP status is not that decision. The product is a boundary enforcement console, not an assistant and not a generic API client.
      </p>
    </section>

    <section class="card stack">
      <h2>The problem it solves</h2>
      <p>
        A payload that leaves one trust domain is untrusted when it enters another. Identity, a contract, and a destination policy can disagree. BoundaryMesh holds that decision in one place so a connector is not the security boundary.
      </p>
    </section>

    <section class="card stack">
      <h2>What was built</h2>
      <p>
        The console, the enforcement pipeline, local labs, quarantine, audit, attestations, a boundary graph, and a hosted operator path. The same navigation is what you are using now. Public pages are filled with synthetic crossings. The operator console is a separate protected document.
      </p>
    </section>

    <section class="card stack">
      <h2>How a system crossing works</h2>
      <ol>
        <li>An incoming request is authenticated.</li>
        <li>Authorization checks that the identity may perform the action.</li>
        <li>Authenticity, freshness, and replay defense run before the payload is trusted.</li>
        <li>The contract is validated.</li>
        <li>Policy decides which fields may leave, including a required transformation.</li>
        <li>The outcome is allow, allow with transformation, quarantine, or reject.</li>
        <li>Evidence is recorded. A delivery instruction exists only after the checks pass.</li>
      </ol>
    </section>

    <section class="card stack">
      <h2>Architecture</h2>
      <p>
        Vue 3 and TypeScript render this console. Pinia holds session state. The local lab talks to a Java 25 and Spring Boot API. The hosted path is Firebase Hosting, a web edge, a private API, Firestore, Pub/Sub, and a worker pool. External connectors stay disabled.
      </p>
    </section>

    <section class="card stack">
      <h2>Security model</h2>
      <p>
        Anonymous visitors can explore this synthetic console. They cannot call the private API, open a real session, or change production. Credential values stay out of the browser. Reprocess and rotation stay in the protected operator console. Operator actions require Google sign in and a web edge session.
      </p>
    </section>

    <section class="card stack">
      <h2>Technology stack</h2>
      <p>
        Vue 3, TypeScript, Pinia, Vite, Java 25, Spring Boot, Spring Security, Firebase Hosting, Cloud Run, Firestore, Pub/Sub, and Cloud KMS. Local evidence uses SHA256 and ECDSA P256. Hosted evidence uses Cloud KMS on the operator path.
      </p>
    </section>

    <section class="card stack">
      <h2>Cloud architecture</h2>
      <p>
        Hosting serves this console and a separate operator document. The web edge is the only public route to the private API, and this page does not use it. The worker consumes delivery work from Pub/Sub. The portfolio project runs in Europe west 4.
      </p>
    </section>

    <section class="card stack">
      <h2>CI/CD and immutable deployment</h2>
      <p>
        Publication builds immutable artifacts once. Staging and promotion deploy those exact artifacts. A frozen release lock records the identities. Pulumi preview must show zero drift before promotion. The live channel changes only through that release path.
      </p>
    </section>

    <section class="card stack">
      <h2>Synthetic demo explanation</h2>
      <p>
        Every crossing, policy result, attestation, delivery instruction, quarantine item, and audit chain on this public console is deterministic sample data. It lives in the browser. It is not customer traffic. Refreshing the page restores the same sample set.
      </p>
    </section>

    <section class="card stack">
      <h2>What is public versus protected</h2>
      <p>
        This console is public. No sign in is required. The operator page is protected. Private API routes, session bootstrap, and cloud mutations are not available to an anonymous visitor.
      </p>
    </section>

    <section class="card stack">
      <h2>Source on GitHub</h2>
      <p>
        <a href="https://github.com/Adellaghmari/boundarymesh">Source on GitHub</a>
      </p>
    </section>

    <section class="card stack">
      <h2>About the builder</h2>
      <p>
        BoundaryMesh is a portfolio engineering project. It demonstrates full stack development, cloud architecture, security, infrastructure as code, automation, continuous integration and deployment, APIs, and product thinking.
      </p>
    </section>
  </div>
</template>
