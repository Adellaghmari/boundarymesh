package dev.adellaghmari.boundarymesh.gcp.firestore;

import java.util.Map;
import java.util.Optional;

public interface FirestoreTransaction {

    Optional<Map<String, Object>> get(FirestoreDocumentPath path);

    void set(FirestoreDocumentPath path, Map<String, Object> fields);

    void create(FirestoreDocumentPath path, Map<String, Object> fields);

    void delete(FirestoreDocumentPath path);
}
