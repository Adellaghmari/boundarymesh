package dev.adellaghmari.boundarymesh.application;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "dev.adellaghmari.boundarymesh.application",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ApplicationArchitectureTest {

    @ArchTest
    static final ArchRule applicationMustNotDependOnSpring = noClasses()
            .that()
            .resideInAPackage("dev.adellaghmari.boundarymesh.application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "org.springframework..",
                    "org.springframework.security..",
                    "com.nimbusds..",
                    "com.google.cloud..",
                    "com.microsoft..",
                    "org.apache.camel..",
                    "com.networknt..");
}
