package dev.adellaghmari.boundarymesh.webedge;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod")
class WebEdgeRuntimeConfig {

    @Bean(destroyMethod = "delete")
    FirebaseApp firebaseApp(@Value("${boundarymesh.web.firebase-project}") String project) throws IOException {
        return FirebaseApp.initializeApp(FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .setProjectId(project)
                .build());
    }

    @Bean
    AuthGateway authGateway(FirebaseApp app) {
        return new FirebaseAuthGateway(FirebaseAuth.getInstance(app));
    }

    @Bean
    OperatorPolicy operatorPolicy(@Value("${boundarymesh.web.operator-uids:}") String uids) {
        return new OperatorPolicy(uids);
    }

    @Bean
    CsrfGuard csrfGuard() {
        return new CsrfGuard();
    }

    @Bean
    CoreApiClient coreApiClient(@Value("${boundarymesh.web.core-api-url}") String url) throws IOException {
        URI base = URI.create(url);
        if (!"https".equals(base.getScheme())
                || base.getHost() == null
                || base.getPath().length() > 1) {
            throw new IllegalArgumentException("core API URL must be an HTTPS service origin");
        }
        GoogleCredentials adc = GoogleCredentials.getApplicationDefault();
        if (!(adc instanceof IdTokenProvider provider)) {
            throw new IllegalStateException("keyless workload identity cannot issue an ID token");
        }
        var credentials = IdTokenCredentials.newBuilder()
                .setIdTokenProvider(provider)
                .setTargetAudience(url)
                .build();
        return new GoogleCoreApiClient(
                base,
                credentials,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build());
    }
}
