package com.aishac.sales;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

/** Camadas por tipo (ADR-025): controller → service → repository, com dto, entity, mapper e exception de apoio. */
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
                .definedBy(BASE + ".controller..")
                .layer("Service")
                .definedBy(BASE + ".service..")
                .layer("Repository")
                .definedBy(BASE + ".repository..")
                .layer("Specification")
                .definedBy(BASE + ".specification..")
                .layer("Mapper")
                .definedBy(BASE + ".mapper..")
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
                .resideInAnyPackage(BASE + ".entity..", BASE + ".dto..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        BASE + ".controller..", BASE + ".service..", BASE + ".repository..", BASE + ".mapper..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void controllersShouldLiveInControllerPackage() {
        classes()
                .that()
                .areAnnotatedWith(RestController.class)
                .should()
                .resideInAPackage(BASE + ".controller..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void serviceImplementationsShouldBeHiddenBehindTheirInterfaces() {
        classes()
                .that()
                .resideInAPackage(BASE + ".service.impl..")
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
    }
}
