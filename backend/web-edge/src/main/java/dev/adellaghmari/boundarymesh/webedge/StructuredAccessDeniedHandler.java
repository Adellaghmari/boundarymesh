package dev.adellaghmari.boundarymesh.webedge;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;

/** Emits a safe, stable error code without revealing security exception details. */
final class StructuredAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException denied)
            throws IOException {
        String code = denied instanceof InvalidCsrfTokenException || denied instanceof MissingCsrfTokenException
                ? "CSRF_REJECTED"
                : "ACCESS_DENIED";
        writeForbidden(response, code);
    }

    static void writeForbidden(HttpServletResponse response, String code) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write("{\"error_code\":\"" + code + "\"}");
    }
}
