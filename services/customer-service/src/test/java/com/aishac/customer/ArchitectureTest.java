package com.aishac.customer;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;
import org.springframework.web.bind.annotation.RestController;

/**
 * Regras da rule architecture-boundaries. Regras de camada aceitam pacotes ainda vazios: passam a valer
 * assim que a primeira feature criar domain/application/adapter.
 */
class ArchitectureTest {

    private static final String BASE = "com.aishac.customer";

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    void layersShouldRespectHexagonalDirection() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .withOptionalLayers(true)
                .layer("Domain")
                .definedBy(BASE + "..domain..")
                .layer("Application")
                .definedBy(BASE + "..application..")
                .layer("Adapter")
                .definedBy(BASE + "..adapter..")
                .layer("Infrastructure")
                .definedBy(BASE + ".infrastructure..")
                .whereLayer("Adapter")
                .mayNotBeAccessedByAnyLayer()
                .whereLayer("Application")
                .mayOnlyBeAccessedByLayers("Adapter", "Infrastructure")
                .whereLayer("Domain")
                .mayOnlyBeAccessedByLayers("Application", "Adapter", "Infrastructure")
                .check(classes);
    }

    @Test
    void domainShouldNotDependOnFrameworks() {
        noClasses()
                .that()
                .resideInAPackage(BASE + "..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "tools.jackson..",
                        "com.fasterxml.jackson..",
                        "lombok..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void inboundAndOutboundAdaptersShouldNotDependOnEachOther() {
        noClasses()
                .that()
                .resideInAPackage(BASE + "..adapter.in..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage(BASE + "..adapter.out..")
                .allowEmptyShould(true)
                .check(classes);
        noClasses()
                .that()
                .resideInAPackage(BASE + "..adapter.out..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage(BASE + "..adapter.in..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void shouldNotUseFieldInjection() {
        NO_CLASSES_SHOULD_USE_FIELD_INJECTION.check(classes);
    }

    @Test
    void controllersShouldNotAccessRepositories() {
        noClasses()
                .that()
                .areAnnotatedWith(RestController.class)
                .should()
                .dependOnClassesThat()
                .areAssignableTo(Repository.class)
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void featurePackagesShouldBeFreeOfCycles() {
        slices().matching(BASE + ".(*)..")
                .should()
                .beFreeOfCycles()
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void controllersShouldLiveInInboundWebAdapters() {
        classes()
                .that()
                .areAnnotatedWith(RestController.class)
                .should()
                .resideInAPackage(BASE + "..adapter.in.web..")
                .allowEmptyShould(true)
                .check(classes);
    }
}
