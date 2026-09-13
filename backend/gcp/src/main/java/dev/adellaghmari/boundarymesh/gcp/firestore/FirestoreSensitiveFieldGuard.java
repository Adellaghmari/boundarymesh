package dev.adellaghmari.boundarymesh.gcp.firestore;

import dev.adellaghmari.boundarymesh.domain.exception.InvariantViolationException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Rejects secret material before any Firestore write. */
public final class FirestoreSensitiveFieldGuard {

    private static final Set<String> FORBIDDEN_KEYS = Set.of(
            "access_token",
            "accesstoken",
            "client_secret",
            "clientsecret",
            "private_key",
            "privatekey",
            "refresh_token",
            "id_token",
            "password",
            "cookie",
            "adc_json",
            "service_account_key",
            "bearer",
            "authorization_header");

    private FirestoreSensitiveFieldGuard() {}

    public static void reject(Map<String, Object> fields) {
        walk("", fields);
    }

    @SuppressWarnings("unchecked")
    private static void walk(String prefix, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                String path = prefix.isBlank() ? key : prefix + "." + key;
                if (FORBIDDEN_KEYS.contains(key.toLowerCase(Locale.ROOT))) {
                    throw new InvariantViolationException(
                            "FIRESTORE_SENSITIVE_FIELD", "forbidden Firestore field: " + path);
                }
                walk(path, entry.getValue());
            }
            return;
        }
        if (value instanceof List<?> list) {
            int index = 0;
            for (Object item : list) {
                walk(prefix + "[" + index + "]", item);
                index++;
            }
            return;
        }
        if (value instanceof String text) {
            String upper = text.toUpperCase(Locale.ROOT);
            if (upper.contains("BEGIN PRIVATE")
                    || upper.contains("BEGIN RSA PRIVATE")
                    || upper.contains("BEGIN EC PRIVATE")
                    || upper.contains("AUTHORIZATION: BEARER")
                    || upper.contains("BEARER ")) {
                throw new InvariantViolationException(
                        "FIRESTORE_SENSITIVE_VALUE", "forbidden secret material in field " + prefix);
            }
        }
    }
}
