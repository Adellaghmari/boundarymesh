package dev.adellaghmari.boundarymesh.api.web;

import dev.adellaghmari.boundarymesh.api.config.BoundaryMeshProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class BoundedRawBodyFilter extends OncePerRequestFilter {

    private final int maxBodyBytes;

    public BoundedRawBodyFilter(BoundaryMeshProperties properties) {
        this.maxBodyBytes = properties.webhook().maxBodyBytes();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/webhooks/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        int contentLength = request.getContentLength();
        if (contentLength > maxBodyBytes) {
            write(response, 413, "PAYLOAD_TOO_LARGE", "Webhook payload exceeds the configured maximum.");
            return;
        }
        byte[] body = readBounded(request.getInputStream(), maxBodyBytes);
        if (body == null) {
            write(response, 413, "PAYLOAD_TOO_LARGE", "Webhook payload exceeds the configured maximum.");
            return;
        }
        filterChain.doFilter(new CachedBodyRequest(request, body), response);
    }

    private static byte[] readBounded(InputStream input, int max) throws IOException {
        byte[] buffer = input.readNBytes(max + 1);
        if (buffer.length > max) {
            return null;
        }
        return buffer;
    }

    private static void write(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"error_code\":\"%s\",\"message\":\"%s\"}".formatted(code, message));
    }
}
