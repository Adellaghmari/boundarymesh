import * as gcp from "@pulumi/gcp";
import type { FoundationConfig } from "./config";

/**
 * Phase 15H-B — Cloud KMS API, attestation key ring, and one asymmetric P-256
 * signing key. Applied. No IAM. No imported key
 * material. SOFTWARE protection is the portfolio/dev default. Asymmetric keys
 * keep a single primary version here; scheduled version cycling is deferred.
 */
export function defineKms(foundation: FoundationConfig) {
    const kmsApi = new gcp.projects.Service("kms-api", {
        project: foundation.gcpProjectId,
        service: "cloudkms.googleapis.com",
        disableOnDestroy: false,
    });

    const keyRing = new gcp.kms.KeyRing(
        "attestation-key-ring",
        {
            project: foundation.gcpProjectId,
            location: foundation.region,
            name: "boundarymesh-attestation",
        },
        { dependsOn: [kmsApi] },
    );

    const cryptoKey = new gcp.kms.CryptoKey(
        "attestation-signing-key",
        {
            name: "exchange-attestation",
            keyRing: keyRing.id,
            purpose: "ASYMMETRIC_SIGN",
            versionTemplate: {
                algorithm: "EC_SIGN_P256_SHA256",
                protectionLevel: "SOFTWARE",
            },
        },
        { dependsOn: [keyRing] },
    );

    return { kmsApi, keyRing, cryptoKey };
}
