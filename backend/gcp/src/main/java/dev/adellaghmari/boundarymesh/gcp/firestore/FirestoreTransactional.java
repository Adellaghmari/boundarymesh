package dev.adellaghmari.boundarymesh.gcp.firestore;

@FunctionalInterface
public interface FirestoreTransactional<T> {

    T apply(FirestoreTransaction transaction) throws Exception;
}
