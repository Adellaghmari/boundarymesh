package dev.adellaghmari.boundarymesh.worker;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "dev.adellaghmari.boundarymesh", importOptions = ImportOption.DoNotIncludeTests.class)
class WorkerArchitectureTest {

    @ArchTest
    static final ArchRule domainMustNotDependOnWorkerAdapters = noClasses()
            .that()
            .resideInAPackage("dev.adellaghmari.boundarymesh.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "dev.adellaghmari.boundarymesh.worker..",
                    "dev.adellaghmari.boundarymesh.gcp..",
                    "com.google.cloud..",
                    "com.google.cloud.firestore..",
                    "com.google.firestore..",
                    "com.google.cloud.pubsub..",
                    "com.google.pubsub..",
                    "com.google.cloud.kms..",
                    "org.apache.camel..",
                    "io.github.resilience4j..",
                    "org.wiremock..");

    @ArchTest
    static final ArchRule workerMustNotDependOnGoogleCloudSdk = noClasses()
            .that()
            .resideInAPackage("dev.adellaghmari.boundarymesh.worker..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "com.google.cloud..",
                    "com.google.cloud.firestore..",
                    "com.google.firestore..",
                    "com.google.cloud.pubsub..",
                    "com.google.pubsub..",
                    "com.google.cloud.kms..");

    @ArchTest
    static final ArchRule onlyWorkerBootstrapMayReferenceGcpAdapters = noClasses()
            .that()
            .resideInAPackage("dev.adellaghmari.boundarymesh.worker..")
            .and()
            .resideOutsideOfPackage("dev.adellaghmari.boundarymesh.worker.bootstrap.gcp..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("dev.adellaghmari.boundarymesh.gcp..");
}
