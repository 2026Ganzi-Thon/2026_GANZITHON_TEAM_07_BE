package com.ganzithon.backend.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import com.ganzithon.backend.architecture.fixture.application.InvalidApplication;

import static org.assertj.core.api.Assertions.assertThat;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class HexagonalArchitectureTests {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.ganzithon.backend");

    private static final ArchRule INNER_RULE = noClasses()
            .that().resideInAnyPackage("..application..", "..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..adapter..", "..config..", "org.springframework..", "jakarta..",
                    "io.swagger..", "com.fasterxml.jackson..", "tools.jackson..",
                    "java.sql..", "javax.sql..")
            .allowEmptyShould(true);

    @Test
    void applicationAndDomainDoNotDependOnOuterTechnology() {
        INNER_RULE.check(CLASSES);
    }

    @Test
    void domainDoesNotDependOnApplication() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..application..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void inboundAdaptersUsePortsInsteadOfServiceImplementations() {
        noClasses().that().resideInAPackage("..adapter.in..")
                .should().dependOnClassesThat().resideInAPackage("..application.service..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void rejectsFrameworkDependencyInAnApplicationFixture() {
        var fixture = new ClassFileImporter().importClasses(InvalidApplication.class);
        assertThat(INNER_RULE.evaluate(fixture).hasViolation()).isTrue();
    }

}
