package dev.adellaghmari.boundarymesh.webedge;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

final class CsrfGuard {
    static final String COOKIE = "__Host-bm_csrf";
    static final String HEADER = "X-BM-CSRF";
    private final SecureRandom random = new SecureRandom();

    String freshToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    boolean valid(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header == null || header.length() != 43 || request.getCookies() == null) {
            return false;
        }
        for (var cookie : request.getCookies()) {
            if (COOKIE.equals(cookie.getName())) {
                byte[] expected = cookie.getValue().getBytes(StandardCharsets.US_ASCII);
                byte[] actual = header.getBytes(StandardCharsets.US_ASCII);
                return MessageDigest.isEqual(expected, actual);
            }
        }
        return false;
    }
}
