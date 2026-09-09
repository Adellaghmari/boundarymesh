package dev.adellaghmari.boundarymesh.api.config;

import dev.adellaghmari.boundarymesh.api.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

final class JsonAccessDeniedWriter {

    private JsonAccessDeniedWriter() {}

    static AuthenticationEntryPoint unauthorized() {
        return (request, response, exception) -> write(response, 401, "UNAUTHORIZED", "Authentication required.");
    }

    static AccessDeniedHandler forbidden() {
        return (request, response, exception) -> write(response, 403, "FORBIDDEN", "Access denied.");
    }

    private static void write(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        if (response.isCommitted()) {
            return;
        }
        String requestId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (requestId == null) {
            requestId = "unknown";
        }
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String json =
                """
                {"timestamp":"%s","request_id":"%s","error_code":"%s","message":"%s"}
                """
                        .formatted(Instant.now().toString(), requestId, code, message);
        response.getWriter().write(json.strip());
    }
}
