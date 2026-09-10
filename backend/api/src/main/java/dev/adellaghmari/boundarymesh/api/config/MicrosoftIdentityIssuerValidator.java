package dev.adellaghmari.boundarymesh.api.config;

import java.util.regex.Pattern;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Accepts Microsoft identity platform v2 issuers issued through the {@code common} authority.
 *
 * <p>Tokens use a tenant GUID issuer, including organizational directories and the consumer MSA
 * tenant. Placeholders such as {@code common} / {@code consumers} and non-Microsoft hosts are
 * rejected.
 */
public final class MicrosoftIdentityIssuerValidator implements OAuth2TokenValidator<Jwt> {

    static final Pattern ISSUER = Pattern.compile(
            "^https://login\\.microsoftonline\\.com/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/v2\\.0$");

    private static final OAuth2Error UNTRUSTED =
            new OAuth2Error("invalid_id_token", "Issuer is not a Microsoft identity platform v2 tenant", null);

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (jwt == null || jwt.getIssuer() == null) {
            return OAuth2TokenValidatorResult.failure(UNTRUSTED);
        }
        String issuer = jwt.getIssuer().toString();
        if (ISSUER.matcher(issuer).matches()) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(UNTRUSTED);
    }
}
