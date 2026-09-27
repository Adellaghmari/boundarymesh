# BoundaryMesh worker image. Build from the repository root:
#   docker build -f backend/worker/Dockerfile -t boundarymesh-worker:phase15d .
# Artifact: backend/worker/target/boundarymesh-worker-0.1.0-SNAPSHOT.jar
# Local HTTP listen port is 8081 for health/smoke only. Cloud Run worker remains
# a protected Pub/Sub pull path (not implemented in Phase 15D) and must not be
# treated as a public delivery endpoint.
# WireMock is part of the current local synthetic provider stub. It is not a secret.

FROM eclipse-temurin:25.0.4_7-jdk-noble@sha256:6ee50f6bf158f3f5bb43b8b52985448743b5d3bfbcb0e99f3780c83d58ffa287 AS build

WORKDIR /src
COPY backend/mvnw backend/mvnw
COPY backend/.mvn backend/.mvn
COPY backend/pom.xml backend/pom.xml
COPY backend/domain backend/domain
COPY backend/application backend/application
COPY backend/gcp backend/gcp
COPY backend/api backend/api
COPY backend/web-edge backend/web-edge
COPY backend/worker backend/worker
COPY contracts /src/contracts

WORKDIR /src/backend
RUN chmod +x mvnw \
    && ./mvnw -B -pl worker -am package -DskipTests \
    && cp worker/target/boundarymesh-worker-0.1.0-SNAPSHOT.jar /tmp/app.jar

FROM eclipse-temurin:25.0.4_7-jre-noble@sha256:ac58cdddfb2104865a2841af40dbf6e16f7473024519158aa0392cad33f9e00b AS runtime

RUN groupadd --system --gid 10001 boundarymesh \
    && useradd --system --uid 10001 --gid 10001 --home-dir /app --shell /usr/sbin/nologin boundarymesh

WORKDIR /app
COPY --from=build --chown=10001:10001 /tmp/app.jar /app/app.jar

USER 10001
EXPOSE 8081

ENV SPRING_PROFILES_ACTIVE=local
# Configuration is runtime-only. Do not set GOOGLE_APPLICATION_CREDENTIALS or connector secrets here.

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
