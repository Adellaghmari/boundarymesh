package dev.adellaghmari.boundarymesh.webedge;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** Keeps denied, non-allowlisted routes distinguishable without exposing authentication details. */
final class StructuredAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authenticationException)
            throws IOException {
        StructuredAccessDeniedHandler.writeForbidden(response, "ROUTE_NOT_ALLOWED");
    }
}
