package dev.adellaghmari.boundarymesh.gcp.firestore;

import java.util.Objects;

public record FirestoreDocumentPath(String collection, String documentId, String subcollection, String childId) {

    public FirestoreDocumentPath {
        Objects.requireNonNull(collection, "collection");
        Objects.requireNonNull(documentId, "documentId");
        if (collection.isBlank() || documentId.isBlank()) {
            throw new IllegalArgumentException("collection and documentId are required");
        }
        if ((subcollection == null) != (childId == null)) {
            throw new IllegalArgumentException("subcollection and childId must both be present or both absent");
        }
    }

    public static FirestoreDocumentPath of(String collection, String documentId) {
        return new FirestoreDocumentPath(collection, documentId, null, null);
    }

    public static FirestoreDocumentPath nested(
            String collection, String documentId, String subcollection, String childId) {
        return new FirestoreDocumentPath(collection, documentId, subcollection, childId);
    }

    public boolean nested() {
        return subcollection != null;
    }
}
