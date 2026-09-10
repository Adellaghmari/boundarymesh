package dev.adellaghmari.boundarymesh.api.microsoft;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts allowlisted Graph error metadata. Never copies tokens or identifiers. */
public final class GraphErrorSanitizer {

    private static final Pattern CODE = Pattern.compile("\"code\"\\s*:\\s*\"([A-Za-z][A-Za-z0-9]*)\"");

    private GraphErrorSanitizer() {}

    public static String code(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        Matcher matcher = CODE.matcher(source);
        if (matcher.find()) {
            return matcher.group(1);
        }
        String trimmed = source.trim();
        if (trimmed.matches("[A-Za-z][A-Za-z0-9]*") && trimmed.length() <= 64) {
            return trimmed;
        }
        return null;
    }

    public static String category(int status, String code, boolean emptyBody) {
        if (code != null) {
            return switch (code) {
                case "InvalidAuthenticationToken" -> "INVALID_AUTHENTICATION_TOKEN";
                case "ErrorAccessDenied", "AccessDenied" -> "ACCESS_DENIED";
                case "MailboxNotEnabledForRESTAPI" -> "MAILBOX_NOT_ENABLED";
                default -> "OTHER";
            };
        }
        if (status == 401 && emptyBody) {
            return "EMPTY_BODY";
        }
        if (status == 401) {
            return "UNAUTHORIZED";
        }
        if (status == 403) {
            return "ACCESS_DENIED";
        }
        if (status == 429) {
            return "RATE_LIMITED";
        }
        if (status >= 500) {
            return "PROVIDER_ERROR";
        }
        if (status == 0) {
            return "TRANSPORT_FAILURE";
        }
        return "OTHER";
    }

    public static boolean wwwAuthenticatePresent(Iterable<String> headerNames) {
        if (headerNames == null) {
            return false;
        }
        for (String name : headerNames) {
            if (name != null && "www-authenticate".equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
}
