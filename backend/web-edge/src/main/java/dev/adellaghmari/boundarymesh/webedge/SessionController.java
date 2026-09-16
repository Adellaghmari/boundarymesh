package dev.adellaghmari.boundarymesh.webedge;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/session")
class SessionController {
    static final String SESSION_COOKIE = "__Host-bm_session";
    private static final Duration SESSION_LIFETIME = Duration.ofHours(1);

    private final AuthGateway auth;
    private final OperatorPolicy operators;
    private final CsrfGuard csrf;

    SessionController(AuthGateway auth, OperatorPolicy operators, CsrfGuard csrf) {
        this.auth = auth;
        this.operators = operators;
        this.csrf = csrf;
    }

    @GetMapping("/bootstrap")
    ResponseEntity<Map<String, String>> bootstrap() {
        String cookie = ResponseCookie.from(CsrfGuard.COOKIE, csrf.freshToken())
                .path("/")
                .secure(true)
                .httpOnly(false)
                .sameSite("Lax")
                .maxAge(SESSION_LIFETIME)
                .build()
                .toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie)
                .cacheControl(CacheControl.noStore())
                .body(Map.of("status", "READY"));
    }

    @PostMapping("/login")
    ResponseEntity<?> login(@RequestBody LoginRequest body, HttpServletRequest request) {
        if (!csrf.valid(request)) {
            return failure(HttpStatus.FORBIDDEN, "CSRF_REJECTED");
        }
        if (body == null
                || body.idToken() == null
                || body.idToken().isBlank()
                || body.idToken().length() > 8192) {
            return failure(HttpStatus.BAD_REQUEST, "TOKEN_REQUIRED");
        }
        try {
            AuthGateway.Principal principal = auth.verifyIdToken(body.idToken());
            long age = Instant.now().getEpochSecond() - principal.authTimeSeconds();
            if (age < -60 || age > 300) {
                return failure(HttpStatus.UNAUTHORIZED, "RECENT_SIGN_IN_REQUIRED");
            }
            if (!operators.allows(principal)) {
                return failure(HttpStatus.FORBIDDEN, "OPERATOR_NOT_AUTHORIZED");
            }
            String signed = auth.createSessionCookie(body.idToken(), SESSION_LIFETIME.toMillis());
            String cookie = ResponseCookie.from(SESSION_COOKIE, signed)
                    .path("/")
                    .secure(true)
                    .httpOnly(true)
                    .sameSite("Lax")
                    .maxAge(SESSION_LIFETIME)
                    .build()
                    .toString();
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie)
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of("status", "AUTHENTICATED"));
        } catch (InvalidSessionException ex) {
            return failure(HttpStatus.UNAUTHORIZED, "INVALID_IDENTITY");
        }
    }

    @GetMapping("/me")
    ResponseEntity<?> me(@CookieValue(name = SESSION_COOKIE, required = false) String cookie) {
        AuthGateway.Principal principal = authorized(cookie);
        if (principal == null) {
            return failure(HttpStatus.UNAUTHORIZED, "SESSION_INVALID");
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("status", "AUTHENTICATED", "email", principal.email() == null ? "" : principal.email()));
    }

    @PostMapping("/logout")
    ResponseEntity<?> logout(
            @CookieValue(name = SESSION_COOKIE, required = false) String cookie, HttpServletRequest request) {
        if (!csrf.valid(request)) {
            return failure(HttpStatus.FORBIDDEN, "CSRF_REJECTED");
        }
        AuthGateway.Principal principal = authorized(cookie);
        if (principal != null) {
            try {
                auth.revokeSessions(principal.uid());
            } catch (InvalidSessionException ex) {
                return failure(HttpStatus.SERVICE_UNAVAILABLE, "LOGOUT_RETRY_REQUIRED");
            }
        }
        String clear = ResponseCookie.from(SESSION_COOKIE, "")
                .path("/")
                .secure(true)
                .httpOnly(true)
                .sameSite("Lax")
                .maxAge(0)
                .build()
                .toString();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clear)
                .cacheControl(CacheControl.noStore())
                .build();
    }

    AuthGateway.Principal authorized(String cookie) {
        if (cookie == null || cookie.isBlank()) {
            return null;
        }
        try {
            AuthGateway.Principal principal = auth.verifySessionCookie(cookie);
            return operators.allows(principal) ? principal : null;
        } catch (InvalidSessionException ex) {
            return null;
        }
    }

    static ResponseEntity<Map<String, String>> failure(HttpStatus status, String code) {
        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .body(Map.of("error_code", code));
    }

    record LoginRequest(String idToken) {}
}
