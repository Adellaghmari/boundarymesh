package dev.adellaghmari.boundarymesh.gcp;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "dev.adellaghmari.boundarymesh", importOptions = ImportOption.DoNotIncludeTests.class)
class GcpArchitectureTest {

    @ArchTest
    static final ArchRule domainMustNotDependOnGcp = noClasses()
            .that()
            .resideInAPackage("dev.adellaghmari.boundarymesh.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("dev.adellaghmari.boundarymesh.gcp..", "com.google.cloud..", "com.google.firestore..");

    @ArchTest
    static final ArchRule applicationMustNotDependOnGcp = noClasses()
            .that()
            .resideInAPackage("dev.adellaghmari.boundarymesh.application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("dev.adellaghmari.boundarymesh.gcp..", "com.google.cloud..", "com.google.firestore..");

    @ArchTest
    static final ArchRule gcpMustNotDependOnApiOrWorker = noClasses()
            .that()
            .resideInAPackage("dev.adellaghmari.boundarymesh.gcp..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("dev.adellaghmari.boundarymesh.api..", "dev.adellaghmari.boundarymesh.worker..");

    @ArchTest
    static final ArchRule secretManagerSdkStaysInGcpSecretPackage = noClasses()
            .that()
            .resideOutsideOfPackage("dev.adellaghmari.boundarymesh.gcp.secret..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.google.cloud.secretmanager..");
}
