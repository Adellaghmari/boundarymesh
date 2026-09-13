package dev.adellaghmari.boundarymesh.gcp.secret;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class FakeSecretAccessor implements SecretAccessor {

    private final Map<String, byte[]> store = new LinkedHashMap<>();
    private final List<String> requested = new ArrayList<>();
    private SecretAccessException failure;

    void put(SecretResourceName name, String value) {
        store.put(name.resourceName(), value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    void failWith(SecretAccessException failure) {
        this.failure = failure;
    }

    List<String> requested() {
        return List.copyOf(requested);
    }

    @Override
    public byte[] access(SecretResourceName name) {
        requested.add(name.resourceName());
        if (failure != null) {
            throw failure;
        }
        byte[] value = store.get(name.resourceName());
        if (value == null) {
            throw SecretAccessException.notFound(name.secretId());
        }
        return value.clone();
    }
}
