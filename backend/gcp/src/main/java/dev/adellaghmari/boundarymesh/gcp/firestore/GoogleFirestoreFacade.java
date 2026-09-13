package dev.adellaghmari.boundarymesh.gcp.firestore;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.Transaction;
import dev.adellaghmari.boundarymesh.domain.exception.InvariantViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Production Firestore Java client. Emulator construction is delegated to
 * {@link EmulatorHttpFirestoreFacade} and never requires ADC.
 */
public final class GoogleFirestoreFacade implements FirestoreFacade {

    private final FirestoreSettings settings;
    private final Firestore firestore;

    GoogleFirestoreFacade(FirestoreSettings settings, Firestore firestore) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.firestore = Objects.requireNonNull(firestore, "firestore");
    }

    public static FirestoreFacade open(FirestoreSettings settings) {
        Objects.requireNonNull(settings, "settings");
        if (settings.usesEmulator()) {
            return EmulatorHttpFirestoreFacade.connect(settings);
        }
        return openProduction(settings);
    }

    /**
     * Official Java SDK client against a real named database. Refuses emulator host and {@code
     * (default)}. Never falls back to {@link EmulatorHttpFirestoreFacade}.
     */
    public static GoogleFirestoreFacade openProduction(FirestoreSettings settings) {
        Objects.requireNonNull(settings, "settings");
        if (settings.usesEmulator()) {
            throw new IllegalStateException("Production Firestore client refuses emulator host");
        }
        if ("(default)".equals(settings.databaseId()) || "default".equalsIgnoreCase(settings.databaseId())) {
            throw new IllegalStateException("Named database 'boundarymesh' is required; (default) is forbidden");
        }
        Firestore firestore = FirestoreOptions.newBuilder()
                .setProjectId(settings.projectId())
                .setDatabaseId(settings.databaseId())
                .build()
                .getService();
        return new GoogleFirestoreFacade(settings, firestore);
    }

    public static FirestoreFacade openEmulator(FirestoreSettings settings) {
        if (!settings.usesEmulator()) {
            throw new IllegalArgumentException("Emulator host is required");
        }
        return open(settings);
    }

    public String addressedDatabaseResource() {
        return "projects/" + firestore.getOptions().getProjectId() + "/databases/"
                + firestore.getOptions().getDatabaseId();
    }

    public Class<?> sdkClientType() {
        return firestore.getClass();
    }

    public List<String> listRootCollectionIds() {
        List<String> names = new ArrayList<>();
        for (CollectionReference collection : firestore.listCollections()) {
            names.add(collection.getId());
        }
        return List.copyOf(names);
    }

    @Override
    public FirestoreSettings settings() {
        return settings;
    }

    @Override
    public <T> T runTransaction(FirestoreTransactional<T> work) {
        try {
            return await(firestore.runTransaction(transaction -> work.apply(new GoogleTransaction(transaction))));
        } catch (RuntimeException ex) {
            throw translate(ex);
        }
    }

    @Override
    public Optional<Map<String, Object>> get(FirestoreDocumentPath path) {
        DocumentSnapshot snapshot = await(reference(path).get());
        return snapshot.exists() ? Optional.of(snapshot.getData()) : Optional.empty();
    }

    @Override
    public void set(FirestoreDocumentPath path, Map<String, Object> fields) {
        FirestoreSensitiveFieldGuard.reject(fields);
        await(reference(path).set(fields));
    }

    @Override
    public void create(FirestoreDocumentPath path, Map<String, Object> fields) {
        FirestoreSensitiveFieldGuard.reject(fields);
        await(reference(path).create(fields));
    }

    @Override
    public void delete(FirestoreDocumentPath path) {
        await(reference(path).delete());
    }

    @Override
    public List<Map<String, Object>> list(String collection, String orderBy, boolean descending, int limit) {
        Query query = firestore.collection(collection);
        if (orderBy != null) {
            query = query.orderBy(orderBy, descending ? Query.Direction.DESCENDING : Query.Direction.ASCENDING);
        }
        if (limit > 0) {
            query = query.limit(limit);
        }
        return documents(await(query.get()));
    }

    @Override
    public List<Map<String, Object>> listSubcollection(
            FirestoreDocumentPath parent, String subcollection, String orderBy, boolean descending, int limit) {
        Query query = reference(parent).collection(subcollection);
        if (orderBy != null) {
            query = query.orderBy(orderBy, descending ? Query.Direction.DESCENDING : Query.Direction.ASCENDING);
        }
        if (limit > 0) {
            query = query.limit(limit);
        }
        return documents(await(query.get()));
    }

    @Override
    public List<Map<String, Object>> listWhere(String collection, String field, String value, int limit) {
        Query query = firestore.collection(collection).whereEqualTo(field, value);
        if (limit > 0) {
            query = query.limit(limit);
        }
        return documents(await(query.get()));
    }

    @Override
    public List<String> listDocumentIds(String collection) {
        List<String> ids = new ArrayList<>();
        for (QueryDocumentSnapshot document :
                await(firestore.collection(collection).get()).getDocuments()) {
            ids.add(document.getId());
        }
        return List.copyOf(ids);
    }

    @Override
    public void close() {
        try {
            firestore.close();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to close Firestore client", ex);
        }
    }

    private DocumentReference reference(FirestoreDocumentPath path) {
        DocumentReference document = firestore.collection(path.collection()).document(path.documentId());
        if (path.nested()) {
            return document.collection(path.subcollection()).document(path.childId());
        }
        return document;
    }

    private static List<Map<String, Object>> documents(QuerySnapshot snapshot) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (QueryDocumentSnapshot document : snapshot.getDocuments()) {
            rows.add(document.getData());
        }
        return List.copyOf(rows);
    }

    private static <T> T await(ApiFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Firestore call interrupted");
        } catch (ExecutionException ex) {
            throw translate(ex.getCause());
        }
    }

    private static RuntimeException translate(Throwable cause) {
        Throwable current = cause;
        int depth = 0;
        while (current != null && depth < 8) {
            if (current instanceof InvariantViolationException invariant) {
                return invariant;
            }
            if (current instanceof IllegalArgumentException illegal) {
                return illegal;
            }
            if (current instanceof FirestoreAccessException access) {
                return access;
            }
            current = current.getCause();
            depth++;
        }
        return FirestoreAccessException.wrap(cause);
    }

    private final class GoogleTransaction implements FirestoreTransaction {

        private final Transaction transaction;

        private GoogleTransaction(Transaction transaction) {
            this.transaction = transaction;
        }

        @Override
        public Optional<Map<String, Object>> get(FirestoreDocumentPath path) {
            DocumentSnapshot snapshot = await(transaction.get(reference(path)));
            return snapshot.exists() ? Optional.of(snapshot.getData()) : Optional.empty();
        }

        @Override
        public void set(FirestoreDocumentPath path, Map<String, Object> fields) {
            FirestoreSensitiveFieldGuard.reject(fields);
            transaction.set(reference(path), fields);
        }

        @Override
        public void create(FirestoreDocumentPath path, Map<String, Object> fields) {
            FirestoreSensitiveFieldGuard.reject(fields);
            transaction.create(reference(path), fields);
        }

        @Override
        public void delete(FirestoreDocumentPath path) {
            transaction.delete(reference(path));
        }
    }
}
