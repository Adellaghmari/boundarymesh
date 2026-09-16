package dev.adellaghmari.boundarymesh.webedge;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.SessionCookieOptions;

final class FirebaseAuthGateway implements AuthGateway {
    private final FirebaseAuth auth;

    FirebaseAuthGateway(FirebaseAuth auth) {
        this.auth = auth;
    }

    @Override
    public Principal verifyIdToken(String token) {
        try {
            return principal(auth.verifyIdToken(token, true));
        } catch (FirebaseAuthException ex) {
            throw new InvalidSessionException();
        }
    }

    @Override
    public String createSessionCookie(String idToken, long lifetimeMillis) {
        try {
            return auth.createSessionCookie(
                    idToken,
                    SessionCookieOptions.builder().setExpiresIn(lifetimeMillis).build());
        } catch (FirebaseAuthException ex) {
            throw new InvalidSessionException();
        }
    }

    @Override
    public Principal verifySessionCookie(String cookie) {
        try {
            return principal(auth.verifySessionCookie(cookie, true));
        } catch (FirebaseAuthException ex) {
            throw new InvalidSessionException();
        }
    }

    @Override
    public void revokeSessions(String uid) {
        try {
            auth.revokeRefreshTokens(uid);
        } catch (FirebaseAuthException ex) {
            throw new InvalidSessionException();
        }
    }

    private static Principal principal(FirebaseToken token) {
        Object raw = token.getClaims().get("auth_time");
        long authTime = raw instanceof Number number ? number.longValue() : 0;
        return new Principal(token.getUid(), token.getEmail(), authTime, token.isEmailVerified());
    }
}
