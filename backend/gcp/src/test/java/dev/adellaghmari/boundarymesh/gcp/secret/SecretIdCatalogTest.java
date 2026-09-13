package dev.adellaghmari.boundarymesh.gcp.secret;

import static org.assertj.core.api.Assertions.assertThat;

import dev.adellaghmari.boundarymesh.domain.credential.CredentialId;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialReference;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialStatus;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialVersion;
import org.junit.jupiter.api.Test;

class SecretIdCatalogTest {

    private final SecretIdCatalog catalog = new SecretIdCatalog("boundarymesh-dev");

    @Test
    void mapsKnownConnectorCredentialToVersionedResource() {
        CredentialReference reference = new CredentialReference(
                new CredentialId("covecrm-api"), new CredentialVersion("2"), CredentialStatus.ACTIVE);

        assertThat(catalog.forCredential(reference))
                .hasValue(new SecretResourceName("boundarymesh-dev", "covecrm-api", "2"));
        assertThat(catalog.forCredential(reference).orElseThrow().resourceName())
                .isEqualTo("projects/boundarymesh-dev/secrets/covecrm-api/versions/2");
    }

    @Test
    void mapsQuayForgeWebhookToLatestHmacSecret() {
        assertThat(catalog.forWebhookConnector("quayforge-orders"))
                .hasValue(SecretResourceName.latest("boundarymesh-dev", "hmac-quayforge"));
    }

    @Test
    void unknownCredentialOrWebhookIsEmpty() {
        CredentialReference unknown = new CredentialReference(
                new CredentialId("unknown-api"), new CredentialVersion("1"), CredentialStatus.ACTIVE);

        assertThat(catalog.forCredential(unknown)).isEmpty();
        assertThat(catalog.forCredential(null)).isEmpty();
        assertThat(catalog.forWebhookConnector("helix-365")).isEmpty();
        assertThat(catalog.forWebhookConnector(" ")).isEmpty();
    }

    @Test
    void inventoryDoesNotIncludeEntraOrGraphSecrets() {
        assertThat(SecretIdCatalog.PHASE_15E_SECRET_IDS)
                .containsExactlyInAnyOrder("hmac-quayforge", "covecrm-api", "lumenway-api", "rivertide-api")
                .doesNotContain("entra-helix-client", "graph-client-state", "BOUNDARYMESH_ENTRA_CLIENT_SECRET");
    }
}
