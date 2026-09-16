package dev.adellaghmari.boundarymesh.webedge;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Explicit product-route proxy. Browser Authorization and forwarding headers are never copied. */
@RestController
class ApiProxyController {
    private final SessionController sessions;
    private final CsrfGuard csrf;
    private final CoreApiClient core;

    ApiProxyController(SessionController sessions, CsrfGuard csrf, CoreApiClient core) {
        this.sessions = sessions;
        this.csrf = csrf;
        this.core = core;
    }

    @PostMapping(path = "/api/v1/transactions", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> submit(
            @CookieValue(name = SessionController.SESSION_COOKIE, required = false) String cookie,
            HttpServletRequest request,
            @RequestBody byte[] body) {
        if (sessions.authorized(cookie) == null) {
            return SessionController.failure(HttpStatus.UNAUTHORIZED, "SESSION_INVALID");
        }
        if (!csrf.valid(request)) {
            return SessionController.failure(HttpStatus.FORBIDDEN, "CSRF_REJECTED");
        }
        if (body == null || body.length == 0 || body.length > 16384) {
            return SessionController.failure(HttpStatus.BAD_REQUEST, "REQUEST_SIZE_INVALID");
        }
        return forward("POST", "/api/v1/transactions", body);
    }

    @GetMapping("/api/v1/deliveries/{instructionId}")
    ResponseEntity<?> delivery(
            @CookieValue(name = SessionController.SESSION_COOKIE, required = false) String cookie,
            @PathVariable String instructionId) {
        if (sessions.authorized(cookie) == null) {
            return SessionController.failure(HttpStatus.UNAUTHORIZED, "SESSION_INVALID");
        }
        if (!instructionId.matches("dlv-[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            return SessionController.failure(HttpStatus.BAD_REQUEST, "INSTRUCTION_ID_INVALID");
        }
        return forward("GET", "/api/v1/deliveries/" + instructionId, null);
    }

    @GetMapping("/api/v1/attestations/{id}/summary")
    ResponseEntity<?> attestation(
            @CookieValue(name = SessionController.SESSION_COOKIE, required = false) String cookie,
            @PathVariable String id) {
        return readUuid(cookie, id, "/api/v1/attestations/", "/summary");
    }

    @GetMapping("/api/v1/attestations/{id}/verify")
    ResponseEntity<?> attestationVerification(
            @CookieValue(name = SessionController.SESSION_COOKIE, required = false) String cookie,
            @PathVariable String id) {
        return readUuid(cookie, id, "/api/v1/attestations/", "/verify");
    }

    @GetMapping("/api/v1/audit/chains/{chainId}/verify")
    ResponseEntity<?> auditVerification(
            @CookieValue(name = SessionController.SESSION_COOKIE, required = false) String cookie,
            @PathVariable String chainId) {
        if (sessions.authorized(cookie) == null) {
            return SessionController.failure(HttpStatus.UNAUTHORIZED, "SESSION_INVALID");
        }
        if (!chainId.matches("[a-zA-Z0-9][a-zA-Z0-9_-]{0,63}")) {
            return SessionController.failure(HttpStatus.BAD_REQUEST, "CHAIN_ID_INVALID");
        }
        return forward("GET", "/api/v1/audit/chains/" + chainId + "/verify", null);
    }

    private ResponseEntity<?> readUuid(String cookie, String id, String prefix, String suffix) {
        if (sessions.authorized(cookie) == null) {
            return SessionController.failure(HttpStatus.UNAUTHORIZED, "SESSION_INVALID");
        }
        if (!id.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            return SessionController.failure(HttpStatus.BAD_REQUEST, "EVIDENCE_ID_INVALID");
        }
        return forward("GET", prefix + id + suffix, null);
    }

    private ResponseEntity<?> forward(String method, String path, byte[] body) {
        try {
            CoreApiClient.CoreResponse result = core.call(method, path, body);
            if (result.status() < 200 || result.status() > 599) {
                return SessionController.failure(HttpStatus.BAD_GATEWAY, "CORE_RESPONSE_INVALID");
            }
            return ResponseEntity.status(result.status())
                    .contentType(MediaType.APPLICATION_JSON)
                    .cacheControl(CacheControl.noStore())
                    .body(result.body());
        } catch (CoreUnavailableException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of("error_code", "CORE_UNAVAILABLE"));
        }
    }
}
