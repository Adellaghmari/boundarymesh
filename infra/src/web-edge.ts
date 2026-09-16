import * as gcp from "@pulumi/gcp";
import * as pulumi from "@pulumi/pulumi";
import type { FoundationConfig } from "./config";

/**
 * 15J-B preview-only web edge. Before 15J-C apply, replace the pending image with
 * the pushed immutable digest and configure an authorized Firebase UID.
 */
export function defineWebEdge(
    foundation: FoundationConfig,
    coreApi: gcp.cloudrunv2.Service,
    runApi: gcp.projects.Service,
) {
    const config = new pulumi.Config();
    const previewImage =
        `${foundation.region}-docker.pkg.dev/${foundation.gcpProjectId}/boundarymesh/` +
        "boundarymesh-web-edge:phase15j-b-preview-not-pushed";
    const image = config.get("webEdgeImage") ?? previewImage;
    const operatorUids = config.get("webOperatorUids") ?? "";
    if (!pulumi.runtime.isDryRun() && (operatorUids.trim() === "" || !image.includes("@sha256:"))) {
        throw new Error("15J-C apply requires an authorized Firebase UID and an immutable pushed web-edge image");
    }

    const identity = new gcp.serviceaccount.Account("web-edge-service-account", {
        project: foundation.gcpProjectId,
        accountId: "boundarymesh-web",
        displayName: "BoundaryMesh application web edge",
        description: "Keyless identity for the public, application-authenticated web BFF",
    });
    const principal = identity.email.apply((email) => `serviceAccount:${email}`);

    const coreInvoker = new gcp.cloudrunv2.ServiceIamMember("web-edge-core-api-invoker", {
        project: foundation.gcpProjectId,
        location: foundation.region,
        name: coreApi.name,
        role: "roles/run.invoker",
        member: principal,
    });

    const authRole = new gcp.projects.IAMCustomRole("web-edge-firebase-session-role", {
        project: foundation.gcpProjectId,
        roleId: "boundarymeshWebSession",
        title: "BoundaryMesh web session operations",
        description: "Create and revoke Firebase Auth sessions; read user revocation state",
        permissions: [
            "firebaseauth.users.createSession",
            "firebaseauth.users.get",
            "firebaseauth.users.update",
        ],
    });
    const authMember = new gcp.projects.IAMMember("web-edge-firebase-session-member", {
        project: foundation.gcpProjectId,
        role: authRole.name,
        member: principal,
    });

    const service = new gcp.cloudrunv2.Service(
        "web-edge-cloud-run-service",
        {
            project: foundation.gcpProjectId,
            name: "boundarymesh-web",
            location: foundation.region,
            description: "Public transport edge with mandatory Firebase operator session authorization",
            deletionProtection: true,
            ingress: "INGRESS_TRAFFIC_ALL",
            invokerIamDisabled: false,
            scaling: { scalingMode: "AUTOMATIC", minInstanceCount: 0, maxInstanceCount: 1 },
            template: {
                serviceAccount: identity.email,
                maxInstanceRequestConcurrency: 10,
                containers: [{
                    name: "web-edge",
                    image,
                    ports: { name: "http1", containerPort: 8080 },
                    envs: [
                        { name: "SPRING_PROFILES_ACTIVE", value: "prod" },
                        { name: "BOUNDARYMESH_WEB_FIREBASE_PROJECT", value: foundation.gcpProjectId },
                        { name: "BOUNDARYMESH_WEB_OPERATOR_UIDS", value: operatorUids },
                        { name: "BOUNDARYMESH_WEB_CORE_API_URL", value: coreApi.uri },
                    ],
                    resources: {
                        limits: { cpu: "1", memory: "512Mi" },
                        cpuIdle: true,
                        startupCpuBoost: true,
                    },
                    startupProbe: {
                        httpGet: { path: "/actuator/health", port: 8080 },
                        periodSeconds: 5,
                        timeoutSeconds: 2,
                        failureThreshold: 24,
                    },
                }],
            },
            traffics: [{ type: "TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST", percent: 100 }],
            labels: { system: "boundarymesh", workload: "web-edge" },
        },
        { dependsOn: [runApi, coreInvoker, authMember] },
    );

    // Only the BFF is public at Cloud Run transport. The core API stays IAM-private.
    const publicEdgeInvoker = new gcp.cloudrunv2.ServiceIamMember("public-web-edge-invoker", {
        project: foundation.gcpProjectId,
        location: foundation.region,
        name: service.name,
        role: "roles/run.invoker",
        member: "allUsers",
    });

    return { identity, coreInvoker, authRole, authMember, service, publicEdgeInvoker };
}
