package gov.cms.dpc.api;

import com.google.common.collect.ImmutableSet;
import io.swagger.annotations.ApiOperation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import javax.ws.rs.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Ensure that all resources have the appropriate handlers and annotations
class APIResourceAnnotationTest {

    private static Set<Method> methods;

    @BeforeAll
    static void filterMethods() {
        final ConfigurationBuilder config = new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("gov.cms.dpc.api.resources"))
                .setScanners(new MethodAnnotationsScanner())
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
                .collect(Collectors.toUnmodifiableSet());

        assertFalse(methods.isEmpty(), "Should have annotated methods");
    }

    @Test
    void allResourcesHaveSwaggerAnnotations() {
        methods
                .forEach(method ->
                        assertTrue(method.isAnnotationPresent(ApiOperation.class), String.format("Method: %s in Class: %s must have Swagger annotation", method.getName(), method.getDeclaringClass())));
    }

}
