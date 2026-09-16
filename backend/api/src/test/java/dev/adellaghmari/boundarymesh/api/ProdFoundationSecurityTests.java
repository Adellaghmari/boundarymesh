package dev.adellaghmari.boundarymesh.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.adellaghmari.boundarymesh.api.config.BoundaryMeshProperties;
import dev.adellaghmari.boundarymesh.api.config.ProdFoundationSecurityConfig;
import dev.adellaghmari.boundarymesh.application.crossing.TransactionSubmissionService;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = dev.adellaghmari.boundarymesh.api.web.TransactionController.class)
@Import({ProdFoundationSecurityConfig.class, ProdFoundationSecurityTests.TestBeans.class})
@ImportAutoConfiguration({SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class})
@ActiveProfiles("prod")
class ProdFoundationSecurityTests {

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {
        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }

        @Bean
        BoundaryMeshProperties properties() {
            return new BoundaryMeshProperties(
                    "boundarymesh-api",
                    "GCP",
                    false,
                    false,
                    "boundary-enforcement.phase10.v1",
                    "",
                    new BoundaryMeshProperties.Security(
                            "http://127.0.0.1:8080/api/v1/dev/issuer",
                            "boundarymesh-api",
                            java.time.Duration.ofMinutes(15)),
                    new BoundaryMeshProperties.Webhook(java.time.Duration.ofMinutes(5), 65536, ""),
                    new BoundaryMeshProperties.Entra(null, null, null, null, null, "HARNESS"));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionSubmissionService submissions;

    @Test
    void transactionIngressIsAdmittedToApplicationValidation() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("REQUEST_INVALID"));
    }

    @Test
    void unrelatedProductionAndDevelopmentRoutesRemainDenied() throws Exception {
        mockMvc.perform(get("/api/v1/admin/session")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/dev/evaluate")).andExpect(status().isUnauthorized());
    }
}
