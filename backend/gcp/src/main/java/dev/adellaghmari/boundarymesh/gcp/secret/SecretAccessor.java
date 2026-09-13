package dev.adellaghmari.boundarymesh.gcp.secret;

/** Low-level Secret Manager read. Implementations must not log or stringify payload bytes. */
public interface SecretAccessor {

    byte[] access(SecretResourceName name);
}
