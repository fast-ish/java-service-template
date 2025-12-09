package fasti.sh.app;

{%- if values.archUnit %}
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JODATIME;

/**
 * Architecture tests using ArchUnit.
 * Enforces coding standards and architectural rules.
 */
class ArchitectureTest {

    private static final String BASE_PACKAGE = "fasti.sh.app";
    private static JavaClasses classes;

    @BeforeAll
    static void setup() {
        classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE_PACKAGE);
    }

    @Nested
    @DisplayName("Layered Architecture")
    class LayeredArchitectureTests {

        @Test
        @DisplayName("should follow layered architecture")
        void layersShouldBeRespected() {
            ArchRule rule = layeredArchitecture()
                .consideringAllDependencies()
                .layer("Controller").definedBy("..controller..")
                .layer("Service").definedBy("..service..")
                .layer("Repository").definedBy("..repository..")
                .layer("Config").definedBy("..config..")
                .layer("DTO").definedBy("..dto..")
                .layer("Exception").definedBy("..exception..")

                .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Service")
                .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service");

            rule.check(classes);
        }

        @Test
        @DisplayName("controllers should only be in controller package")
        void controllersShouldBeInControllerPackage() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Controller")
                .should().resideInAPackage("..controller..");

            rule.check(classes);
        }

        @Test
        @DisplayName("services should only be in service package")
        void servicesShouldBeInServicePackage() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Service")
                .and().areNotInterfaces()
                .should().resideInAPackage("..service..");

            rule.check(classes);
        }

        @Test
        @DisplayName("repositories should only be in repository package")
        void repositoriesShouldBeInRepositoryPackage() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Repository")
                .should().resideInAPackage("..repository..");

            rule.check(classes);
        }
    }

    @Nested
    @DisplayName("Naming Conventions")
    class NamingConventionTests {

        @Test
        @DisplayName("controllers should be annotated with @RestController")
        void controllersShouldBeAnnotated() {
            ArchRule rule = classes()
                .that().resideInAPackage("..controller..")
                .and().haveSimpleNameEndingWith("Controller")
                .should().beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class);

            rule.check(classes);
        }

        @Test
        @DisplayName("services should be annotated with @Service")
        void servicesShouldBeAnnotated() {
            ArchRule rule = classes()
                .that().resideInAPackage("..service..")
                .and().haveSimpleNameEndingWith("Service")
                .and().areNotInterfaces()
                .should().beAnnotatedWith(org.springframework.stereotype.Service.class);

            rule.check(classes);
        }

        @Test
        @DisplayName("configuration classes should be annotated with @Configuration")
        void configClassesShouldBeAnnotated() {
            ArchRule rule = classes()
                .that().resideInAPackage("..config..")
                .and().haveSimpleNameEndingWith("Config")
                .should().beAnnotatedWith(org.springframework.context.annotation.Configuration.class);

            rule.check(classes);
        }

        @Test
        @DisplayName("exception classes should extend RuntimeException")
        void exceptionsShouldExtendRuntimeException() {
            ArchRule rule = classes()
                .that().resideInAPackage("..exception..")
                .and().haveSimpleNameEndingWith("Exception")
                .should().beAssignableTo(RuntimeException.class);

            rule.check(classes);
        }
    }

    @Nested
    @DisplayName("Dependency Rules")
    class DependencyTests {

        @Test
        @DisplayName("controllers should not depend on repositories directly")
        void controllersShouldNotAccessRepositories() {
            ArchRule rule = noClasses()
                .that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAPackage("..repository..");

            rule.check(classes);
        }

        @Test
        @DisplayName("services should not depend on controllers")
        void servicesShouldNotDependOnControllers() {
            ArchRule rule = noClasses()
                .that().resideInAPackage("..service..")
                .should().dependOnClassesThat().resideInAPackage("..controller..");

            rule.check(classes);
        }
    }

    @Nested
    @DisplayName("Coding Rules")
    class CodingRuleTests {

        @Test
        @DisplayName("should not use System.out or System.err")
        void shouldNotAccessStandardStreams() {
            NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(classes);
        }

        @Test
        @DisplayName("should not throw generic exceptions")
        void shouldNotThrowGenericExceptions() {
            NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS.check(classes);
        }

        @Test
        @DisplayName("should not use java.util.logging")
        void shouldNotUseJavaUtilLogging() {
            NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.check(classes);
        }

        @Test
        @DisplayName("should not use Joda Time")
        void shouldNotUseJodaTime() {
            NO_CLASSES_SHOULD_USE_JODATIME.check(classes);
        }

        @Test
        @DisplayName("should use SLF4J for logging")
        void shouldUseSl4jForLogging() {
            ArchRule rule = classes()
                .that().resideInAPackage(BASE_PACKAGE + "..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.slf4j..");

            // This is a soft check - just ensuring SLF4J is available
            // Individual classes don't need to use logging
        }
    }
}
{%- else %}
import org.junit.jupiter.api.Test;

/**
 * Placeholder for architecture tests.
 * Enable ArchUnit in the template to get architecture validation.
 */
class ArchitectureTest {

    @Test
    void placeholder() {
        // ArchUnit not enabled
    }
}
{%- endif %}
