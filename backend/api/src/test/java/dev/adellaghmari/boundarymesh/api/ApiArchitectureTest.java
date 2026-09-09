package dev.adellaghmari.boundarymesh.api;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "dev.adellaghmari.boundarymesh", importOptions = ImportOption.DoNotIncludeTests.class)
class ApiArchitectureTest {

    @ArchTest
    static final ArchRule domainAndApplicationStayFreeOfSpring = noClasses()
            .that()
            .resideInAnyPackage("dev.adellaghmari.boundarymesh.domain..", "dev.adellaghmari.boundarymesh.application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "com.google.cloud..", "com.microsoft..", "org.apache.camel..");
}
