package gov.cms.dpc.api;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableSet;
import gov.cms.dpc.api.auth.annotations.AdminOperation;
import gov.cms.dpc.api.auth.annotations.Authorizer;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.auth.annotations.Public;
import gov.cms.dpc.api.resources.v1.BaseResource;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import io.swagger.annotations.ApiOperation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import javax.ws.rs.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

// Ensure that all resources have the appropriate handlers and annotations
@ExtendWith(BufferedLoggerHandler.class)
class APIResourceAnnotationTest {

    private static Set<Method> methods;

    @BeforeAll
    static void filterMethods() {
        final ConfigurationBuilder config = new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("gov.cms.dpc.api.resources"))
                .setScanners(Scanners.MethodsAnnotated)
                .filterInputsBy(new FilterBuilder().includePackage("gov.cms.dpc.api.resources"));

        final Reflections reflections = new Reflections(config);

        methods = ImmutableSet.<Method>builder()
                .addAll(reflections.getMethodsAnnotatedWith(GET.class))
                .addAll(reflections.getMethodsAnnotatedWith(POST.class))
                .addAll(reflections.getMethodsAnnotatedWith(PUT.class))
                .addAll(reflections.getMethodsAnnotatedWith(HEAD.class))
                .addAll(reflections.getMethodsAnnotatedWith(DELETE.class))
                .addAll(reflections.getMethodsAnnotatedWith(PATCH.class))
                .build();

        methods = methods.stream()
                .filter(method -> !Modifier.isAbstract(method.getModifiers()))
                .filter(method -> !BaseResource.class.equals(method.getDeclaringClass()))
                .collect(Collectors.toUnmodifiableSet());

        assertFalse(methods.isEmpty(), "Should have annotated methods");
    }

    @Test
    void allResourcesHaveMonitoringAnnotations() {
        methods.forEach(method -> assertAll(
                () -> assertTrue(method.isAnnotationPresent(Timed.class), String.format("Method: %s in Class: %s must have @Timed annotation", method.getName(), method.getDeclaringClass())),
                () -> assertTrue(method.isAnnotationPresent(ExceptionMetered.class), String.format("Method: %s in Class: %s must have @ExceptionMetered annotation", method.getName(), method.getDeclaringClass()))
        ));
    }

    @Test
    void allResourcesHaveSwaggerAnnotations() {
        methods
                .forEach(method ->
                        assertTrue(method.isAnnotationPresent(ApiOperation.class), String.format("Method: %s in Class: %s must have Swagger annotation", method.getName(), method.getDeclaringClass())));
    }// iterate over parameters (at least one should have Auth)

    @Test
    void allResourcesHaveSecurityAnnotations() {
        methods.forEach(APIResourceAnnotationTest::assertMethodHasValidAuthAnnotations);
    }

    /**
     * Asserts that the method has valid auth-annotations
     * To pass, the method must either have a parameter with an Authorized, PathAuthorized, or Public annotation on the method
     *
     * @param method - The Method to check for valid annotations
     */
    private static void assertMethodHasValidAuthAnnotations(Method method) {
        assertTrue((method.isAnnotationPresent(Authorizer.class) || method.isAnnotationPresent(PathAuthorizer.class) || method.isAnnotationPresent(Public.class) || method.isAnnotationPresent(AdminOperation.class)),
                String.format("Method: %s in Class: %s must either have a parameter with an Authorized, PathAuthorizer, or Public annotation on the method.", method.getName(), method.getDeclaringClass())
        );
    }
}
