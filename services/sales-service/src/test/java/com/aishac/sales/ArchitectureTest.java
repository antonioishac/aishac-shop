package com.aishac.sales;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.INTERFACES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

/**
 * Camadas por tipo (ADR-025), repetidas dentro de cada módulo ({@code catalog}, {@code order}, {@code payment}; ADR-026):
 * controller → service → repository, com dto, entity, mapper e exception de apoio.
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
    void layersShouldOnlyBeAccessedFromAbove() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .withOptionalLayers(true)
                .layer("Controller")
                .definedBy(BASE + "..controller..")
                .layer("Service")
                .definedBy(BASE + "..service..")
                .layer("Repository")
                .definedBy(BASE + "..repository..")
                .layer("Specification")
                .definedBy(BASE + "..specification..")
                .layer("Mapper")
                .definedBy(BASE + "..mapper..")
                .whereLayer("Controller")
                .mayNotBeAccessedByAnyLayer()
                .whereLayer("Service")
                .mayOnlyBeAccessedByLayers("Controller")
                .whereLayer("Repository")
                .mayOnlyBeAccessedByLayers("Service")
                .whereLayer("Specification")
                .mayOnlyBeAccessedByLayers("Service")
                .whereLayer("Mapper")
                .mayOnlyBeAccessedByLayers("Service")
                .check(classes);
    }

    @Test
    void entitiesAndDtosShouldNotDependOnUpperLayers() {
        noClasses()
                .that()
                .resideInAnyPackage(BASE + "..entity..", BASE + "..dto..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        BASE + "..controller..", BASE + "..service..", BASE + "..repository..", BASE + "..mapper..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void controllersShouldLiveInControllerPackage() {
        classes()
                .that()
                .areAnnotatedWith(RestController.class)
                .should()
                .resideInAPackage(BASE + "..controller..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void serviceImplementationsShouldBeHiddenBehindTheirInterfaces() {
        classes()
                .that()
                .resideInAPackage(BASE + "..service.impl..")
                .should()
                .notHaveModifier(JavaModifier.PUBLIC)
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void shouldNotUseFieldInjection() {
        NO_CLASSES_SHOULD_USE_FIELD_INJECTION.check(classes);
    }

    @Test
    void packagesShouldBeFreeOfCycles() {
        slices().matching(BASE + ".(*)..").should().beFreeOfCycles().check(classes);
        slices().matching(BASE + ".catalog.(*)..").should().beFreeOfCycles().check(classes);
    }

    /** ADR-026: o catálogo poderá virar serviço próprio (IF-010), então não conhece pedido nem pagamento. */
    @Test
    void catalogShouldNotDependOnOtherModules() {
        noClasses()
                .that()
                .resideInAPackage(BASE + ".catalog..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(BASE + ".order..", BASE + ".payment..")
                .check(classes);
    }

    /**
     * ADR-026: outros módulos usam o catálogo só pelas interfaces {@code *Service} dele, trafegando os DTOs e os
     * códigos de erro do catálogo. Helpers de regra, armazenamento e implementações ficam internos ao módulo.
     */
    @Test
    void otherModulesShouldUseCatalogOnlyThroughItsServiceInterfaces() {
        DescribedPredicate<JavaClass> catalogInternals = resideInAPackage(BASE + ".catalog..")
                .and(not(resideInAPackage(BASE + ".catalog.service")
                        .and(simpleNameEndingWith("Service"))
                        .and(INTERFACES)))
                .and(not(resideInAnyPackage(BASE + ".catalog.dto..", BASE + ".catalog.exception..")))
                .as("catalog internals");
        noClasses()
                .that()
                .resideInAnyPackage(BASE + ".order..", BASE + ".payment..")
                .should()
                .dependOnClassesThat(catalogInternals)
                .allowEmptyShould(true)
                .check(classes);
    }
}
