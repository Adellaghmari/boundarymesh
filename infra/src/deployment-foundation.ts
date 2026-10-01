import * as gcp from "@pulumi/gcp";
import type { FoundationConfig } from "./config";
import {
    GCP_PROJECT_NUMBER,
    GITHUB_HOSTING_DEPLOY_ENVIRONMENT,
    GITHUB_RUNTIME_DEPLOY_ENVIRONMENT,
    GITHUB_WIF_POOL_ID,
} from "./wif";

export const RUNTIME_DEPLOY_SERVICE_ACCOUNT_ID = "boundarymesh-deploy" as const;
export const HOSTING_DEPLOY_SERVICE_ACCOUNT_ID = "boundarymesh-hosting-deploy" as const;
export const CONTROLLED_RUN_DEPLOY_ROLE_ID = "boundarymeshControlledRunDeployer" as const;
export const RUN_OPERATION_READER_ROLE_ID = "boundarymeshRunOperationReader" as const;
export const HOSTING_RUN_READER_ROLE_ID = "boundarymeshHostingRunReader" as const;
export const HOSTING_QUOTA_CONSUMER_ROLE_ID = "boundarymeshHostingQuotaConsumer" as const;
export const DEPLOYMENT_EVIDENCE_REPOSITORY_ID = "boundarymesh-deployments" as const;

/**
 * Existing-resource deployment only. Create/delete, Jobs, source upload, SSH,
 * setIamPolicy, and project mutation permissions are deliberately absent.
 */
export const CONTROLLED_RUN_DEPLOY_PERMISSIONS = [
    "run.operations.get",
    "run.revisions.get",
    "run.revisions.list",
    "run.services.get",
    "run.services.getIamPolicy",
    "run.services.update",
    "run.workerpools.get",
    "run.workerpools.getIamPolicy",
    "run.workerpools.update",
] as const;
export const RUN_OPERATION_READER_PERMISSIONS = [
    "run.operations.get",
] as const;
export const HOSTING_RUN_READER_PERMISSIONS = [
    "run.services.get",
] as const;

export const FIREBASE_HOSTING_ROLE = "roles/firebasehosting.admin" as const;
export const ARTIFACT_READER_ROLE = "roles/artifactregistry.reader" as const;
export const ARTIFACT_WRITER_ROLE = "roles/artifactregistry.writer" as const;
export const HOSTING_QUOTA_CONSUMER_PERMISSIONS = [
    "serviceusage.services.use",
] as const;
export const HOSTING_WORKFLOW_FORBIDDEN_OPERATIONS = [
    "firebase hosting:sites:create",
    "firebase hosting:sites:delete",
    "firebase init",
    "project switching",
    "arbitrary Firebase site input",
    "arbitrary Firebase project input",
] as const;

interface DeploymentFoundationDependencies {
    readonly artifactRegistryApi: gcp.projects.Service;
    readonly dockerRepository: gcp.artifactregistry.Repository;
    readonly releaseRepository: gcp.artifactregistry.Repository;
    readonly iamApi: gcp.projects.Service;
    readonly wifProvider: gcp.iam.WorkloadIdentityPoolProvider;
    readonly apiService: gcp.cloudrunv2.Service;
    readonly webService: gcp.cloudrunv2.Service;
    readonly workerPool: gcp.cloudrunv2.WorkerPool;
    readonly apiServiceAccount: gcp.serviceaccount.Account;
    readonly webServiceAccount: gcp.serviceaccount.Account;
    readonly workerServiceAccount: gcp.serviceaccount.Account;
}

/**
 * Desired Phase 16E split-authority foundation. GitHub environments are
 * verified external prerequisites and are intentionally not managed here.
 */
export function defineDeploymentFoundation(
    foundation: FoundationConfig,
    dependencies: DeploymentFoundationDependencies,
) {
    const runtimeDeployServiceAccount = new gcp.serviceaccount.Account(
        "runtime-deploy-service-account",
        {
            project: foundation.gcpProjectId,
            accountId: RUNTIME_DEPLOY_SERVICE_ACCOUNT_ID,
            displayName: "BoundaryMesh Controlled Deployer",
            description: "Keyless controlled deployment of existing BoundaryMesh runtime workloads",
            createIgnoreAlreadyExists: false,
        },
        { dependsOn: [dependencies.iamApi] },
    );
    const hostingDeployServiceAccount = new gcp.serviceaccount.Account(
        "hosting-deploy-service-account",
        {
            project: foundation.gcpProjectId,
            accountId: HOSTING_DEPLOY_SERVICE_ACCOUNT_ID,
            displayName: "BoundaryMesh Firebase Hosting Deployer",
            description: "Keyless Firebase Hosting-only deployment identity",
            createIgnoreAlreadyExists: false,
        },
        { dependsOn: [dependencies.iamApi] },
    );
    const runtimePrincipal = runtimeDeployServiceAccount.email.apply(
        (email) => `serviceAccount:${email}`,
    );
    const hostingPrincipal = hostingDeployServiceAccount.email.apply(
        (email) => `serviceAccount:${email}`,
    );

    const runtimeEnvironmentPrincipal =
        `principalSet://iam.googleapis.com/projects/${GCP_PROJECT_NUMBER}/locations/global/` +
        `workloadIdentityPools/${GITHUB_WIF_POOL_ID}/attribute.environment/${GITHUB_RUNTIME_DEPLOY_ENVIRONMENT}`;
    const hostingEnvironmentPrincipal =
        `principalSet://iam.googleapis.com/projects/${GCP_PROJECT_NUMBER}/locations/global/` +
        `workloadIdentityPools/${GITHUB_WIF_POOL_ID}/attribute.environment/${GITHUB_HOSTING_DEPLOY_ENVIRONMENT}`;

    const runtimeEnvironmentImpersonation = new gcp.serviceaccount.IAMMember(
        "runtime-deploy-environment-impersonation",
        {
            serviceAccountId: runtimeDeployServiceAccount.name,
            role: "roles/iam.workloadIdentityUser",
            member: runtimeEnvironmentPrincipal,
        },
        { dependsOn: [dependencies.wifProvider] },
    );
    const hostingEnvironmentImpersonation = new gcp.serviceaccount.IAMMember(
        "hosting-deploy-environment-impersonation",
        {
            serviceAccountId: hostingDeployServiceAccount.name,
            role: "roles/iam.workloadIdentityUser",
            member: hostingEnvironmentPrincipal,
        },
        { dependsOn: [dependencies.wifProvider] },
    );

    const controlledRunDeployRole = new gcp.projects.IAMCustomRole(
        "controlled-run-deploy-role",
        {
            project: foundation.gcpProjectId,
            roleId: CONTROLLED_RUN_DEPLOY_ROLE_ID,
            title: "BoundaryMesh Controlled Run Deployer",
            description: "Update and verify only existing BoundaryMesh Cloud Run services and worker pool",
            permissions: [...CONTROLLED_RUN_DEPLOY_PERMISSIONS],
        },
    );
    const runOperationReaderRole = new gcp.projects.IAMCustomRole(
        "run-operation-reader-role",
        {
            project: foundation.gcpProjectId,
            roleId: RUN_OPERATION_READER_ROLE_ID,
            title: "BoundaryMesh Run Operation Reader",
            description:
                "Allows the isolated BoundaryMesh runtime deployment identity to poll Cloud Run " +
                "long-running operations created by controlled deployments.",
            permissions: [...RUN_OPERATION_READER_PERMISSIONS],
        },
    );
    const runtimeOperationReader = new gcp.projects.IAMMember(
        "runtime-deploy-operation-reader",
        {
            project: foundation.gcpProjectId,
            role: runOperationReaderRole.name,
            member: runtimePrincipal,
        },
    );

    const apiRuntimeDeployer = new gcp.cloudrunv2.ServiceIamMember("runtime-deploy-api-updater", {
        project: foundation.gcpProjectId,
        location: foundation.region,
        name: dependencies.apiService.name,
        role: controlledRunDeployRole.name,
        member: runtimePrincipal,
    });
    const webRuntimeDeployer = new gcp.cloudrunv2.ServiceIamMember("runtime-deploy-web-updater", {
        project: foundation.gcpProjectId,
        location: foundation.region,
        name: dependencies.webService.name,
        role: controlledRunDeployRole.name,
        member: runtimePrincipal,
    });
    const workerRuntimeDeployer = new gcp.cloudrunv2.WorkerPoolIamMember(
        "runtime-deploy-worker-updater",
        {
            project: foundation.gcpProjectId,
            location: foundation.region,
            name: dependencies.workerPool.name,
            role: controlledRunDeployRole.name,
            member: runtimePrincipal,
        },
    );

    const runtimeServiceAccountUsers = [
        { name: "runtime-deploy-api-act-as", serviceAccount: dependencies.apiServiceAccount },
        { name: "runtime-deploy-web-act-as", serviceAccount: dependencies.webServiceAccount },
        { name: "runtime-deploy-worker-act-as", serviceAccount: dependencies.workerServiceAccount },
    ].map(
        ({ name, serviceAccount }) =>
            new gcp.serviceaccount.IAMMember(name, {
                serviceAccountId: serviceAccount.name,
                role: "roles/iam.serviceAccountUser",
                member: runtimePrincipal,
            }),
    );

    const apiHealthInvoker = new gcp.cloudrunv2.ServiceIamMember("runtime-deploy-api-invoker", {
        project: foundation.gcpProjectId,
        location: foundation.region,
        name: dependencies.apiService.name,
        role: "roles/run.invoker",
        member: runtimePrincipal,
    });

    const runtimeDockerReader = new gcp.artifactregistry.RepositoryIamMember(
        "runtime-deploy-docker-reader",
        {
            project: foundation.gcpProjectId,
            location: foundation.region,
            repository: dependencies.dockerRepository.repositoryId,
            role: ARTIFACT_READER_ROLE,
            member: runtimePrincipal,
        },
    );
    const runtimeReleaseReader = new gcp.artifactregistry.RepositoryIamMember(
        "runtime-deploy-release-reader",
        {
            project: foundation.gcpProjectId,
            location: foundation.region,
            repository: dependencies.releaseRepository.repositoryId,
            role: ARTIFACT_READER_ROLE,
            member: runtimePrincipal,
        },
    );
    const hostingReleaseReader = new gcp.artifactregistry.RepositoryIamMember(
        "hosting-deploy-release-reader",
        {
            project: foundation.gcpProjectId,
            location: foundation.region,
            repository: dependencies.releaseRepository.repositoryId,
            role: ARTIFACT_READER_ROLE,
            member: hostingPrincipal,
        },
    );
    const hostingRunReaderRole = new gcp.projects.IAMCustomRole(
        "hosting-run-reader-role",
        {
            project: foundation.gcpProjectId,
            roleId: HOSTING_RUN_READER_ROLE_ID,
            title: "BoundaryMesh Hosting Run Reader",
            description:
                "Allows the isolated Firebase Hosting deployment identity to read only the " +
                "BoundaryMesh Web Cloud Run service required to validate Hosting rewrites.",
            permissions: [...HOSTING_RUN_READER_PERMISSIONS],
        },
    );
    const hostingWebRunReader = new gcp.cloudrunv2.ServiceIamMember(
        "hosting-deploy-web-run-reader",
        {
            project: foundation.gcpProjectId,
            location: foundation.region,
            name: dependencies.webService.name,
            role: hostingRunReaderRole.name,
            member: hostingPrincipal,
        },
    );

    // Firebase Hosting currently supports predefined roles rather than custom
    // roles. This project-level grant is isolated to the Hosting-only identity.
    // Site create/delete are platform-granted but BoundaryMesh-workflow-forbidden.
    const hostingAdmin = new gcp.projects.IAMMember("hosting-deploy-firebase-admin", {
        project: foundation.gcpProjectId,
        role: FIREBASE_HOSTING_ROLE,
        member: hostingPrincipal,
    });
    const hostingQuotaConsumerRole = new gcp.projects.IAMCustomRole(
        "hosting-quota-consumer-role",
        {
            project: foundation.gcpProjectId,
            roleId: HOSTING_QUOTA_CONSUMER_ROLE_ID,
            title: "BoundaryMesh Hosting Quota Consumer",
            description:
                "Allows the isolated BoundaryMesh Firebase Hosting deployment identity to designate " +
                "boundarymesh-dev as the quota project for Firebase Hosting API requests.",
            permissions: [...HOSTING_QUOTA_CONSUMER_PERMISSIONS],
        },
    );
    const hostingQuotaConsumer = new gcp.projects.IAMMember(
        "hosting-deploy-quota-consumer",
        {
            project: foundation.gcpProjectId,
            role: hostingQuotaConsumerRole.name,
            member: hostingPrincipal,
        },
    );

    const evidenceRepository = new gcp.artifactregistry.Repository(
        "boundarymesh-deployments",
        {
            project: foundation.gcpProjectId,
            location: foundation.region,
            repositoryId: DEPLOYMENT_EVIDENCE_REPOSITORY_ID,
            format: "GENERIC",
            mode: "STANDARD_REPOSITORY",
            description: "Durable BoundaryMesh deployment evidence only",
            deletionPolicy: "PREVENT",
        },
        { dependsOn: [dependencies.artifactRegistryApi], protect: true },
    );
    const evidenceWriter = new gcp.artifactregistry.RepositoryIamMember(
        "runtime-deploy-evidence-writer",
        {
            project: foundation.gcpProjectId,
            location: foundation.region,
            repository: evidenceRepository.repositoryId,
            role: ARTIFACT_WRITER_ROLE,
            member: runtimePrincipal,
        },
    );

    return {
        runtimeDeployServiceAccount,
        hostingDeployServiceAccount,
        runtimeEnvironmentImpersonation,
        hostingEnvironmentImpersonation,
        controlledRunDeployRole,
        runOperationReaderRole,
        runtimeOperationReader,
        apiRuntimeDeployer,
        webRuntimeDeployer,
        workerRuntimeDeployer,
        runtimeServiceAccountUsers,
        apiHealthInvoker,
        runtimeDockerReader,
        runtimeReleaseReader,
        hostingReleaseReader,
        hostingRunReaderRole,
        hostingWebRunReader,
        hostingAdmin,
        hostingQuotaConsumerRole,
        hostingQuotaConsumer,
        evidenceRepository,
        evidenceWriter,
    };
}
