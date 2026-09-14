package dev.adellaghmari.boundarymesh.domain;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "dev.adellaghmari.boundarymesh.domain", importOptions = ImportOption.DoNotIncludeTests.class)
class DomainArchitectureTest {

    @ArchTest
    static final ArchRule domainMustNotDependOnSpring = noClasses()
            .that()
            .resideInAPackage("dev.adellaghmari.boundarymesh.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..");

    @ArchTest
    static final ArchRule domainMustNotDependOnPersistenceAdapters = noClasses()
            .that()
            .resideInAPackage("dev.adellaghmari.boundarymesh.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "dev.adellaghmari.boundarymesh.persistence..",
                    "dev.adellaghmari.boundarymesh.gcp..",
                    "com.google.cloud..",
                    "com.google.firestore..",
                    "com.google.pubsub..",
                    "com.google.cloud.kms..");

    @ArchTest
    static final ArchRule domainMustNotDependOnMicrosoftOrGcpSdks = noClasses()
            .that()
            .resideInAPackage("dev.adellaghmari.boundarymesh.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "com.microsoft..",
                    "com.azure..",
                    "com.google.cloud..",
                    "org.apache.camel..",
                    "io.github.resilience4j..",
                    "com.github.tomakehurst.wiremock..",
                    "org.wiremock..",
                    "org.springframework.security..",
                    "com.nimbusds..",
                    "com.networknt..",
                    "tools.jackson..");

    @ArchTest
    static final ArchRule domainMustNotDependOnFrontend = noClasses()
            .that()
            .resideInAPackage("dev.adellaghmari.boundarymesh.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("vue..", "org.vue..");
}
