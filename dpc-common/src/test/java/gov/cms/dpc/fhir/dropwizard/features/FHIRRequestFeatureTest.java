package gov.cms.dpc.fhir.dropwizard.features;

import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.FHIRAsync;
import gov.cms.dpc.fhir.dropwizard.filters.FHIRAsyncRequestFilter;
import gov.cms.dpc.fhir.dropwizard.filters.FHIRRequestFilter;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;

@ExtendWith(DropwizardExtensionsSupport.class)
@DisplayName("FHIR request feature annotation")
public class FHIRRequestFeatureTest {

    ResourceInfo info = Mockito.mock(ResourceInfo.class);
    FeatureContext context = Mockito.mock(FeatureContext.class);
    FHIRRequestFeature feature = new FHIRRequestFeature();

    private FHIRRequestFeatureTest() {
        // Not used
    }

    @AfterEach()
    void cleanup() {
        Mockito.reset(info);
        Mockito.reset(context);
    }

    @Test
@DisplayName("Annotate FHIR resource 🥳")

    void testFHIRResourceAnnotation() throws NoSuchMethodException {
        Method method = FHIRRequestFeatureTest.class.getMethod("testFhirAnnotationMethod");
        Mockito.when(info.getResourceMethod()).thenReturn(method);
        feature.configure(info, context);
        Mockito.verify(context, Mockito.times(1)).register(FHIRRequestFilter.class);
    }

    @Test
@DisplayName("Annotate FHIR Java class 🥳")

    void testFHIRClassAnnotation() throws NoSuchMethodException {
        Method method = FHIRRequestFeatureTest.class.getMethod("testNoAnnotationMethod");
        Mockito.when(info.getResourceClass()).thenAnswer(answer -> FHIRResourceClass.class);
        Mockito.when(info.getResourceMethod()).thenReturn(method);
        feature.configure(info, context);
        Mockito.verify(context, Mockito.times(1)).register(FHIRRequestFilter.class);
    }

    @Test
@DisplayName("Annotate Async FHIR resource 🥳")

    void testAsyncFHIRResourceAnnotation() throws NoSuchMethodException {
        Method method = FHIRRequestFeatureTest.class.getMethod("testFhirAsyncAnnotatedMethod");
        Mockito.when(info.getResourceClass()).thenAnswer(answer -> FHIRAsyncResourceClass.class);
        Mockito.when(info.getResourceMethod()).thenReturn(method);
        feature.configure(info, context);
        Mockito.verify(context, Mockito.times(1)).register(FHIRAsyncRequestFilter.class);
    }

    @Test
@DisplayName("Annotate Async FHIR Java class 🥳")

    void testAsyncFHIRClassAnnotation() throws NoSuchMethodException {
        Method method = FHIRRequestFeatureTest.class.getMethod("testFhirAsyncAnnotatedMethod");
        Mockito.when(info.getResourceClass()).thenAnswer(answer -> FHIRAsyncResourceClass.class);
        Mockito.when(info.getResourceMethod()).thenReturn(method);
        feature.configure(info, context);
        Mockito.verify(context, Mockito.times(1)).register(FHIRAsyncRequestFilter.class);
    }


    @Test
@DisplayName("Select no annotation method 🥳")

    void testNoAnnotation() throws NoSuchMethodException {
        Method method = FHIRRequestFeatureTest.class.getMethod("testNoAnnotationMethod");
        Mockito.when(info.getResourceClass()).thenAnswer(answer -> NoAnnotationClass.class);
        Mockito.when(info.getResourceMethod()).thenReturn(method);
        feature.configure(info, context);
        Mockito.verify(context, Mockito.never()).register(Mockito.any());
    }

    @FHIR
    static class FHIRResourceClass {

    }

    @FHIRAsync
    static class FHIRAsyncResourceClass {

    }

    static class NoAnnotationClass {

    }

    @FHIR
    public void testFhirAnnotationMethod() {}
    @FHIRAsync
    public void testFhirAsyncAnnotatedMethod() {}
    public void testNoAnnotationMethod() {}
}
