package dev.adellaghmari.boundarymesh.gcp.firestore;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Named-database emulator transport. Talks to the Firestore emulator REST API so adapter tests do
 * not require ADC or a working Java gRPC emulator channel.
 */
public final class EmulatorHttpFirestoreFacade implements FirestoreFacade {

    private static final int TRANSACTION_ATTEMPTS = 24;

    private final FirestoreSettings settings;
    private final HttpClient http;
    private final String documentsRoot;
    private final String resourceRoot;

    private EmulatorHttpFirestoreFacade(
            FirestoreSettings settings, HttpClient http, String documentsRoot, String resourceRoot) {
        this.settings = settings;
        this.http = http;
        this.documentsRoot = documentsRoot;
        this.resourceRoot = resourceRoot;
    }

    static EmulatorHttpFirestoreFacade connect(FirestoreSettings settings) {
        Objects.requireNonNull(settings, "settings");
        if (!settings.usesEmulator()) {
            throw new IllegalArgumentException("Emulator host is required");
        }
        String emulatorHost = settings.emulatorHost().orElseThrow();
        String documentsRoot = "http://"
                + emulatorHost
                + "/v1/projects/"
                + encode(settings.projectId())
                + "/databases/"
                + encode(settings.databaseId())
                + "/documents";
        String resourceRoot = "projects/" + settings.projectId() + "/databases/" + settings.databaseId() + "/documents";
        HttpClient http =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        return new EmulatorHttpFirestoreFacade(settings, http, documentsRoot, resourceRoot);
    }

    @Override
    public FirestoreSettings settings() {
        return settings;
    }

    @Override
    public <T> T runTransaction(FirestoreTransactional<T> work) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= TRANSACTION_ATTEMPTS; attempt++) {
            try {
                return runOnce(work);
            } catch (FirestoreTransactionConflictException conflict) {
                last = conflict;
                sleep(Math.min(2_000L, 80L * attempt));
            }
        }
        throw last == null ? new IllegalStateException("Firestore emulator transaction failed") : last;
    }

    @Override
    public Optional<Map<String, Object>> get(FirestoreDocumentPath path) {
        return get(path, null);
    }

    @Override
    public void set(FirestoreDocumentPath path, Map<String, Object> fields) {
        FirestoreSensitiveFieldGuard.reject(fields);
        JsonObject body = new JsonObject();
        body.add("fields", FirestoreRestValues.fieldsOf(fields));
        send("PATCH", documentUrl(path), body.toString(), 200, 201);
    }

    @Override
    public void create(FirestoreDocumentPath path, Map<String, Object> fields) {
        FirestoreSensitiveFieldGuard.reject(fields);
        JsonObject body = new JsonObject();
        body.add("fields", FirestoreRestValues.fieldsOf(fields));
        String url = collectionUrl(path) + "?documentId=" + encode(path.nested() ? path.childId() : path.documentId());
        HttpResponse<String> response = sendRaw("POST", url, body.toString());
        if (response.statusCode() == 409 || response.statusCode() == 400 && alreadyExists(response.body())) {
            throw new FirestoreTransactionConflictException("document already exists");
        }
        require(response, 200, 201);
    }

    @Override
    public void delete(FirestoreDocumentPath path) {
        send("DELETE", documentUrl(path), null, 200);
    }

    @Override
    public List<Map<String, Object>> list(String collection, String orderBy, boolean descending, int limit) {
        return sortAndLimit(listCollection(documentsRoot + "/" + encode(collection)), orderBy, descending, limit);
    }

    @Override
    public List<Map<String, Object>> listSubcollection(
            FirestoreDocumentPath parent, String subcollection, String orderBy, boolean descending, int limit) {
        String url = documentUrl(parent) + "/" + encode(subcollection);
        return sortAndLimit(listCollection(url), orderBy, descending, limit);
    }

    @Override
    public List<Map<String, Object>> listWhere(String collection, String field, String value, int limit) {
        JsonObject query = new JsonObject();
        JsonObject structured = new JsonObject();
        JsonArray from = new JsonArray();
        JsonObject collectionId = new JsonObject();
        collectionId.addProperty("collectionId", collection);
        from.add(collectionId);
        structured.add("from", from);
        JsonObject where = new JsonObject();
        JsonObject filter = new JsonObject();
        JsonObject fieldPath = new JsonObject();
        fieldPath.addProperty("fieldPath", field);
        filter.add("field", fieldPath);
        filter.addProperty("op", "EQUAL");
        filter.add("value", FirestoreRestValues.valueOf(value));
        where.add("fieldFilter", filter);
        structured.add("where", where);
        if (limit > 0) {
            structured.addProperty("limit", limit);
        }
        query.add("structuredQuery", structured);
        HttpResponse<String> response = sendRaw("POST", documentsRoot + ":runQuery", query.toString());
        if (response.statusCode() == 404) {
            return List.of();
        }
        require(response, 200);
        return documentsFromQuery(response.body());
    }

    @Override
    public List<String> listDocumentIds(String collection) {
        List<String> ids = new ArrayList<>();
        for (JsonObject document : rawDocuments(documentsRoot + "/" + encode(collection))) {
            ids.add(documentId(document.get("name").getAsString()));
        }
        return List.copyOf(ids);
    }

    @Override
    public void close() {
        // HttpClient has no required shutdown for this usage.
    }

    private <T> T runOnce(FirestoreTransactional<T> work) {
        String transaction = beginTransaction();
        HttpTransaction buffer = new HttpTransaction(transaction);
        try {
            T result = work.apply(buffer);
            commit(transaction, buffer.writes);
            return result;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Firestore emulator transaction failed", ex);
        }
    }

    private String beginTransaction() {
        JsonObject body = new JsonObject();
        JsonObject options = new JsonObject();
        options.add("readWrite", new JsonObject());
        body.add("options", options);
        HttpResponse<String> response = sendRaw("POST", documentsRoot + ":beginTransaction", body.toString());
        require(response, 200);
        return JsonParser.parseString(response.body())
                .getAsJsonObject()
                .get("transaction")
                .getAsString();
    }

    private void commit(String transaction, List<JsonObject> writes) {
        JsonObject body = new JsonObject();
        body.addProperty("transaction", transaction);
        JsonArray writeArray = new JsonArray();
        for (JsonObject write : writes) {
            writeArray.add(write);
        }
        body.add("writes", writeArray);
        HttpResponse<String> response = sendRaw("POST", documentsRoot + ":commit", body.toString());
        if (response.statusCode() == 409
                || response.statusCode() == 400 && (aborted(response.body()) || alreadyExists(response.body()))) {
            throw new FirestoreTransactionConflictException(response.body());
        }
        require(response, 200);
    }

    private Optional<Map<String, Object>> get(FirestoreDocumentPath path, String transaction) {
        if (transaction != null) {
            return getInTransaction(path, transaction);
        }
        HttpResponse<String> response = sendRaw("GET", documentUrl(path), null);
        if (response.statusCode() == 404) {
            return Optional.empty();
        }
        require(response, 200);
        JsonObject document = JsonParser.parseString(response.body()).getAsJsonObject();
        return Optional.of(FirestoreRestValues.fromFields(document.getAsJsonObject("fields")));
    }

    private Optional<Map<String, Object>> getInTransaction(FirestoreDocumentPath path, String transaction) {
        JsonObject body = new JsonObject();
        body.addProperty("transaction", transaction);
        JsonArray documents = new JsonArray();
        documents.add(resourceName(path));
        body.add("documents", documents);
        HttpResponse<String> response = sendRaw("POST", documentsRoot + ":batchGet", body.toString());
        require(response, 200);
        JsonElement parsed = JsonParser.parseString(response.body());
        JsonArray rows = parsed.isJsonArray() ? parsed.getAsJsonArray() : new JsonArray();
        if (parsed.isJsonObject()) {
            rows.add(parsed.getAsJsonObject());
        }
        for (JsonElement element : rows) {
            JsonObject row = element.getAsJsonObject();
            if (row.has("found")) {
                JsonObject found = row.getAsJsonObject("found");
                return Optional.of(FirestoreRestValues.fromFields(found.getAsJsonObject("fields")));
            }
        }
        return Optional.empty();
    }

    private List<Map<String, Object>> listCollection(String url) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (JsonObject document : rawDocuments(url)) {
            rows.add(FirestoreRestValues.fromFields(document.getAsJsonObject("fields")));
        }
        return rows;
    }

    private List<JsonObject> rawDocuments(String url) {
        HttpResponse<String> response = sendRaw("GET", url, null);
        if (response.statusCode() == 404) {
            return List.of();
        }
        require(response, 200);
        JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
        List<JsonObject> documents = new ArrayList<>();
        if (body.has("documents")) {
            for (JsonElement element : body.getAsJsonArray("documents")) {
                documents.add(element.getAsJsonObject());
            }
        }
        return documents;
    }

    private List<Map<String, Object>> documentsFromQuery(String body) {
        JsonElement parsed = JsonParser.parseString(body);
        JsonArray rows = parsed.isJsonArray() ? parsed.getAsJsonArray() : new JsonArray();
        if (parsed.isJsonObject()) {
            rows.add(parsed.getAsJsonObject());
        }
        List<Map<String, Object>> documents = new ArrayList<>();
        for (JsonElement element : rows) {
            JsonObject row = element.getAsJsonObject();
            if (row.has("document")) {
                JsonObject document = row.getAsJsonObject("document");
                documents.add(FirestoreRestValues.fromFields(document.getAsJsonObject("fields")));
            }
        }
        return documents;
    }

    private List<Map<String, Object>> sortAndLimit(
            List<Map<String, Object>> rows, String orderBy, boolean descending, int limit) {
        if (orderBy != null) {
            Comparator<Map<String, Object>> comparator =
                    Comparator.comparing(row -> String.valueOf(row.getOrDefault(orderBy, "")), String::compareTo);
            rows.sort(descending ? comparator.reversed() : comparator);
        }
        if (limit > 0 && rows.size() > limit) {
            return List.copyOf(rows.subList(0, limit));
        }
        return List.copyOf(rows);
    }

    private String documentUrl(FirestoreDocumentPath path) {
        String url = documentsRoot + "/" + encode(path.collection()) + "/" + encode(path.documentId());
        if (path.nested()) {
            url += "/" + encode(path.subcollection()) + "/" + encode(path.childId());
        }
        return url;
    }

    private String collectionUrl(FirestoreDocumentPath path) {
        if (path.nested()) {
            return documentsRoot
                    + "/"
                    + encode(path.collection())
                    + "/"
                    + encode(path.documentId())
                    + "/"
                    + encode(path.subcollection());
        }
        return documentsRoot + "/" + encode(path.collection());
    }

    private String resourceName(FirestoreDocumentPath path) {
        String name = resourceRoot + "/" + path.collection() + "/" + path.documentId();
        if (path.nested()) {
            name += "/" + path.subcollection() + "/" + path.childId();
        }
        return name;
    }

    private JsonObject updateWrite(FirestoreDocumentPath path, Map<String, Object> fields, boolean createOnly) {
        FirestoreSensitiveFieldGuard.reject(fields);
        JsonObject write = new JsonObject();
        JsonObject update = new JsonObject();
        update.addProperty("name", resourceName(path));
        update.add("fields", FirestoreRestValues.fieldsOf(fields));
        write.add("update", update);
        if (createOnly) {
            JsonObject current = new JsonObject();
            current.addProperty("exists", false);
            write.add("currentDocument", current);
        }
        return write;
    }

    private JsonObject deleteWrite(FirestoreDocumentPath path) {
        JsonObject write = new JsonObject();
        write.addProperty("delete", resourceName(path));
        return write;
    }

    private HttpResponse<String> send(String method, String url, String body, int... ok) {
        HttpResponse<String> response = sendRaw(method, url, body);
        require(response, ok);
        return response;
    }

    private HttpResponse<String> sendRaw(String method, String url, String body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer owner")
                    .header("Content-Type", "application/json");
            if (body == null) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.method(method, HttpRequest.BodyPublishers.ofString(body));
            }
            return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException ex) {
            throw new IllegalStateException("Firestore emulator HTTP " + method + " failed", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Firestore emulator HTTP interrupted", ex);
        }
    }

    private static void require(HttpResponse<String> response, int... ok) {
        for (int status : ok) {
            if (response.statusCode() == status) {
                return;
            }
        }
        throw new IllegalStateException("Firestore emulator HTTP " + response.statusCode() + " " + response.body());
    }

    private static boolean alreadyExists(String body) {
        return body != null && (body.contains("ALREADY_EXISTS") || body.contains("already exists"));
    }

    private static boolean aborted(String body) {
        return body != null && (body.contains("ABORTED") || body.contains("aborted"));
    }

    private static String documentId(String resourceName) {
        int slash = resourceName.lastIndexOf('/');
        return slash < 0 ? resourceName : resourceName.substring(slash + 1);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    private final class HttpTransaction implements FirestoreTransaction {

        private final String transaction;
        private final List<JsonObject> writes = new ArrayList<>();

        private HttpTransaction(String transaction) {
            this.transaction = transaction;
        }

        @Override
        public Optional<Map<String, Object>> get(FirestoreDocumentPath path) {
            return EmulatorHttpFirestoreFacade.this.get(path, transaction);
        }

        @Override
        public void set(FirestoreDocumentPath path, Map<String, Object> fields) {
            writes.add(updateWrite(path, fields, false));
        }

        @Override
        public void create(FirestoreDocumentPath path, Map<String, Object> fields) {
            writes.add(updateWrite(path, fields, true));
        }

        @Override
        public void delete(FirestoreDocumentPath path) {
            writes.add(deleteWrite(path));
        }
    }

    static final class FirestoreTransactionConflictException extends RuntimeException {
        FirestoreTransactionConflictException(String message) {
            super(message);
        }
    }
}
