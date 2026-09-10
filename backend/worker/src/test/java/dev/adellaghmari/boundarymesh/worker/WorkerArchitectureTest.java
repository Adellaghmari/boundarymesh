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
                    "org.apache.camel..",
                    "io.github.resilience4j..",
                    "org.wiremock..");
}
