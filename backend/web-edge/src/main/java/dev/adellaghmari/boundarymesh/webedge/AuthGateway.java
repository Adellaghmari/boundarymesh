package dev.adellaghmari.boundarymesh.webedge;

/** Firebase identity operations are isolated from HTTP and can be faked in local tests. */
public interface AuthGateway {
    Principal verifyIdToken(String token);

    String createSessionCookie(String idToken, long lifetimeMillis);

    Principal verifySessionCookie(String cookie);

    void revokeSessions(String uid);

    record Principal(String uid, String email, long authTimeSeconds, boolean emailVerified) {}
}
