package dev.adellaghmari.boundarymesh.gcp.firestore;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Narrow Firestore boundary. Repository adapters depend on this type, not the Google SDK. */
public interface FirestoreFacade extends AutoCloseable {

    FirestoreSettings settings();

    <T> T runTransaction(FirestoreTransactional<T> work);

    Optional<Map<String, Object>> get(FirestoreDocumentPath path);

    void set(FirestoreDocumentPath path, Map<String, Object> fields);

    void create(FirestoreDocumentPath path, Map<String, Object> fields);

    void delete(FirestoreDocumentPath path);

    List<Map<String, Object>> list(String collection, String orderBy, boolean descending, int limit);

    List<Map<String, Object>> listSubcollection(
            FirestoreDocumentPath parent, String subcollection, String orderBy, boolean descending, int limit);

    List<Map<String, Object>> listWhere(String collection, String field, String value, int limit);

    List<String> listDocumentIds(String collection);

    @Override
    void close();
}
