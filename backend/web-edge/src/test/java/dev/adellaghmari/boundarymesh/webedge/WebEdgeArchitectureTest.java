package dev.adellaghmari.boundarymesh.webedge;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "dev.adellaghmari.boundarymesh.webedge")
class WebEdgeArchitectureTest {
    @ArchTest
    static final ArchRule edgeNeverImportsCoreBusinessOrGcpPersistence = noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..boundarymesh.domain..", "..boundarymesh.application..", "..boundarymesh.gcp..");
}
