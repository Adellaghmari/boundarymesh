package dev.adellaghmari.boundarymesh.gcp.secret;

import static org.assertj.core.api.Assertions.assertThat;

import dev.adellaghmari.boundarymesh.application.credential.SecretMaterial;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialId;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialReference;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialStatus;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialVersion;
import org.junit.jupiter.api.Test;

class SecretManagerSecretValueProviderTest {

    private static final String FAKE = "TEST-SM-ADAPTER-FAKE-VALUE-NOT-A-SECRET";

    private final SecretIdCatalog catalog = new SecretIdCatalog("boundarymesh-dev");
    private final FakeSecretAccessor accessor = new FakeSecretAccessor();
    private final SecretManagerSecretValueProvider provider = new SecretManagerSecretValueProvider(catalog, accessor);

    @Test
    void resolveReturnsMaterialThroughPortOnly() {
        CredentialReference reference = active("covecrm-api", "2");
        accessor.put(catalog.forCredential(reference).orElseThrow(), FAKE);

        try (SecretMaterial material = provider.resolve(reference).orElseThrow()) {
            assertThat(material.reveal()).isEqualTo(FAKE);
            assertThat(material.toString())
                    .isEqualTo("SecretMaterial[redacted]")
                    .doesNotContain(FAKE);
        }
        assertThat(accessor.requested()).containsExactly("projects/boundarymesh-dev/secrets/covecrm-api/versions/2");
    }

    @Test
    void missingDeniedAndUnmappedReferencesFailClosed() {
        accessor.failWith(SecretAccessException.notFound("covecrm-api"));
        assertThat(provider.resolve(active("covecrm-api", "2"))).isEmpty();

        accessor.failWith(SecretAccessException.denied("lumenway-api"));
        assertThat(provider.resolve(active("lumenway-api", "1"))).isEmpty();

        assertThat(provider.resolve(active("unknown-api", "1"))).isEmpty();
        assertThat(provider.resolve(new CredentialReference(
                        new CredentialId("covecrm-api"), new CredentialVersion("2"), CredentialStatus.RETIRED)))
                .isEmpty();
        assertThat(provider.resolve(null)).isEmpty();
    }

    @Test
    void providerUnavailableFailsClosedWithoutLeakingPayload() {
        SecretAccessException unavailable = SecretAccessException.unavailable("rivertide-api");
        accessor.failWith(unavailable);

        assertThat(provider.resolve(active("rivertide-api", "1"))).isEmpty();
        assertThat(unavailable.getMessage()).doesNotContain(FAKE);
        assertThat(unavailable.toString()).doesNotContain(FAKE);
    }

    private static CredentialReference active(String id, String version) {
        return new CredentialReference(new CredentialId(id), new CredentialVersion(version), CredentialStatus.ACTIVE);
    }
}
