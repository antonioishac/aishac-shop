package com.aishac.sales;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.repository.Repository;
import org.springframework.web.bind.annotation.RestController;

/**
 * Regras da rule architecture-boundaries. Regras de camada aceitam pacotes ainda vazios: passam a valer
 * assim que a primeira feature criar domain/application/adapter.
 */
class ArchitectureTest {

    private static final String BASE = "com.aishac.sales";

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
                .dependOnClassesThat(resideInAnyPackage(
                                "org.springframework..",
                                "jakarta.persistence..",
                                "tools.jackson..",
                                "com.fasterxml.jackson..",
                                "lombok..")
                        // @Getter é permitido no domínio; lombok.config grava @lombok.Generated no bytecode.
                        .and(not(name("lombok.Generated"))))
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

    // Isolamento entre módulos do sales-service (catalog, order, payment).

    @Test
    void catalogShouldNotDependOnOtherModules() {
        noClasses()
                .that()
                .resideInAPackage(BASE + ".catalog..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(BASE + ".order..", BASE + ".payment..")
                .allowEmptyShould(true)
                .check(classes);
    }

    /**
     * Um módulo usa outro só por um port próprio, implementado num adapter seu que chama a camada application
     * do outro. Nunca toca domain ou adapter alheios, e seu domain/application não conhecem o outro módulo.
     */
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({"order, catalog", "order, payment", "payment, order", "payment, catalog"})
    void moduleShouldReachAnotherOnlyThroughItsOwnAdapters(String consumer, String provider) {
        noClasses()
                .that()
                .resideInAPackage(BASE + "." + consumer + "..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(BASE + "." + provider + "..domain..", BASE + "." + provider + "..adapter..")
                .allowEmptyShould(true)
                .check(classes);
        noClasses()
                .that()
                .resideInAnyPackage(BASE + "." + consumer + "..domain..", BASE + "." + consumer + "..application..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage(BASE + "." + provider + "..")
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
