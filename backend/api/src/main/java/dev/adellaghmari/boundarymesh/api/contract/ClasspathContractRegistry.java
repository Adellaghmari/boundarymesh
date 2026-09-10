package dev.adellaghmari.boundarymesh.api.contract;

import dev.adellaghmari.boundarymesh.application.contract.ContractEvaluator;
import dev.adellaghmari.boundarymesh.application.contract.ContractRegistry;
import dev.adellaghmari.boundarymesh.application.contract.RegisteredContract;
import dev.adellaghmari.boundarymesh.domain.contract.ContractId;
import dev.adellaghmari.boundarymesh.domain.contract.ContractReference;
import dev.adellaghmari.boundarymesh.domain.contract.ContractStatus;
import dev.adellaghmari.boundarymesh.domain.contract.ContractType;
import dev.adellaghmari.boundarymesh.domain.contract.ContractVersion;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class ClasspathContractRegistry implements ContractRegistry {

    private final Map<String, RegisteredContract> contracts = new ConcurrentHashMap<>();

    public ClasspathContractRegistry() {
        register(
                ContractEvaluator.QUAYFORGE_ORDERS,
                "v1",
                ContractStatus.SUPPORTED,
                read("contracts/json-schema/quayforge-order.v1.schema.json"));
        register(
                ContractEvaluator.QUAYFORGE_ORDERS,
                "v2",
                ContractStatus.SUPPORTED,
                read("contracts/json-schema/quayforge-order.v2.schema.json"));
        register(
                ContractEvaluator.QUAYFORGE_ORDERS,
                "v3",
                ContractStatus.UNSUPPORTED,
                "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\"}");
        register(
                "m365-calendar",
                "v1",
                ContractStatus.SUPPORTED,
                read("contracts/json-schema/m365-calendar.v1.schema.json"));
    }

    @Override
    public Optional<RegisteredContract> find(String integrationId, String version) {
        return Optional.ofNullable(contracts.get(key(integrationId, version)));
    }

    @Override
    public List<RegisteredContract> list(String integrationId) {
        return contracts.values().stream()
                .filter(item -> item.integrationId().equals(integrationId))
                .toList();
    }

    private void register(String integrationId, String version, ContractStatus status, String schemaJson) {
        contracts.put(
                key(integrationId, version),
                new RegisteredContract(
                        integrationId,
                        new ContractReference(
                                new ContractId(integrationId),
                                new ContractVersion(version),
                                ContractType.JSON_SCHEMA,
                                status),
                        schemaJson));
    }

    private static String key(String integrationId, String version) {
        return integrationId + ":" + version;
    }

    private static String read(String classpath) {
        ClassPathResource resource = new ClassPathResource(classpath);
        try (InputStream input = resource.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Missing contract artifact " + classpath, ex);
        }
    }
}
