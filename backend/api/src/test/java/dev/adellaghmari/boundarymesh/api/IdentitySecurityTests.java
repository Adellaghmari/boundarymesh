package dev.adellaghmari.boundarymesh.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.adellaghmari.boundarymesh.api.security.LocalSigningKeys;
import dev.adellaghmari.boundarymesh.api.security.LocalTokenService;
import dev.adellaghmari.boundarymesh.api.security.TokenScenario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class IdentitySecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalTokenService tokens;

    @Autowired
    private LocalSigningKeys keys;

    @Test
    void missingTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/security/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code").value("UNAUTHORIZED"));
    }

    @Test
    void malformedTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/security/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validMachineIdentityIsAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/security/me").header("Authorization", bearer(TokenScenario.VALID_MACHINE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("velora-quayforge-connector"))
                .andExpect(jsonPath("$.issuer").exists())
                .andExpect(jsonPath("$.scopes[0]").value("boundary.evaluate"));
    }

    @Test
    void expiredWrongIssuerWrongAudienceUnknownKidInvalidAndFutureNbfAreUnauthorized() throws Exception {
        for (TokenScenario scenario : new TokenScenario[] {
            TokenScenario.EXPIRED,
            TokenScenario.WRONG_ISSUER,
            TokenScenario.WRONG_AUDIENCE,
            TokenScenario.UNKNOWN_KID,
            TokenScenario.INVALID_SIGNATURE,
            TokenScenario.FUTURE_NBF
        }) {
            mockMvc.perform(get("/api/v1/security/me").header("Authorization", bearer(scenario)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void missingScopeIsForbiddenOnProtectedEvaluate() throws Exception {
        mockMvc.perform(post("/api/v1/dev/evaluate")
                        .header("Authorization", bearer(TokenScenario.MISSING_PERMISSION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error_code").value("FORBIDDEN"));
    }

    @Test
    void missingPermissionProducesDomainReject() throws Exception {
        mockMvc.perform(post("/api/v1/identity/evaluate")
                        .header("Authorization", bearer(TokenScenario.MISSING_PERMISSION)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("REJECT"))
                .andExpect(jsonPath("$.identity.status").value("PASS"))
                .andExpect(jsonPath("$.authorization.status").value("FAIL"))
                .andExpect(jsonPath("$.reason_codes[0].code").value("AUTHZ_MISSING_SCOPE"));
    }

    @Test
    void validMachineIdentityAllowsCrossing() throws Exception {
        mockMvc.perform(post("/api/v1/identity/evaluate").header("Authorization", bearer(TokenScenario.VALID_MACHINE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("ALLOW"))
                .andExpect(jsonPath("$.identity.status").value("PASS"))
                .andExpect(jsonPath("$.authorization.status").value("PASS"))
                .andExpect(jsonPath("$.ingress_kind").value("TOKEN_PROTECTED_API"));
    }

    @Test
    void rotatedKeyStillVerifies() throws Exception {
        keys.rotate();
        mockMvc.perform(get("/api/v1/security/me").header("Authorization", bearer(TokenScenario.VALID_MACHINE)))
                .andExpect(status().isOk());
    }

    @Test
    void securityResponsesDoNotEchoTokens() throws Exception {
        String token = tokens.issue(TokenScenario.VALID_MACHINE);
        String body = mockMvc.perform(get("/api/v1/security/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        org.assertj.core.api.Assertions.assertThat(body).doesNotContain(token);
        org.assertj.core.api.Assertions.assertThat(body).doesNotContain("eyJ");
        String denied = mockMvc.perform(get("/api/v1/security/me").header("Authorization", "Bearer " + token + "x"))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString();
        org.assertj.core.api.Assertions.assertThat(denied).doesNotContain(token);
        org.assertj.core.api.Assertions.assertThat(denied).doesNotContain("eyJ");
    }

    @Test
    void requestLogsDoNotContainBearerTokens(CapturedOutput output) throws Exception {
        String token = tokens.issue(TokenScenario.VALID_MACHINE);
        mockMvc.perform(post("/api/v1/identity/evaluate").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(output.getAll()).doesNotContain(token);
        org.assertj.core.api.Assertions.assertThat(output.getAll()).doesNotContain("Bearer ");
    }

    @Test
    void jwksIsPublicAndContainsKeys() throws Exception {
        mockMvc.perform(get("/api/v1/dev/jwks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kid").exists())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"));
    }

    private String bearer(TokenScenario scenario) {
        return "Bearer " + tokens.issue(scenario);
    }
}
