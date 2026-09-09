package dev.adellaghmari.boundarymesh.api.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** In-process RSA keyset for the local test issuer. Not Entra. Not a production key store. */
@Component
@Profile({"local", "test"})
public class LocalSigningKeys {

    private final AtomicReference<State> state;

    public LocalSigningKeys() {
        RSAKey current = generate("bm-local-current");
        this.state = new AtomicReference<>(new State(current, List.of(current)));
    }

    public RSAKey current() {
        return state.get().current();
    }

    public JWKSet publicJwkSet() {
        List<RSAKey> publics =
                state.get().all().stream().map(RSAKey::toPublicJWK).toList();
        return new JWKSet(new ArrayList<>(publics));
    }

    public synchronized void rotate() {
        RSAKey next = generate("bm-local-" + System.currentTimeMillis());
        List<RSAKey> all = new ArrayList<>(state.get().all());
        all.add(next);
        state.set(new State(next, List.copyOf(all)));
    }

    private static RSAKey generate(String kid) {
        try {
            return new RSAKeyGenerator(2048).keyID(kid).generate();
        } catch (JOSEException ex) {
            throw new IllegalStateException("Unable to generate local RSA key", ex);
        }
    }

    private record State(RSAKey current, List<RSAKey> all) {}
}
