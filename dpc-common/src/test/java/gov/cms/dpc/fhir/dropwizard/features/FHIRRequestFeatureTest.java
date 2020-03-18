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

@ExtendWith(DropwizardExtensionsSupport.class)
public class FHIRRequestFeatureTest {

    ResourceInfo info = Mockito.mock(ResourceInfo.class);
    FeatureContext context = Mockito.mock(FeatureContext.class);
    Method method = Mockito.mock(Method.class);
    FHIR fhirAnnotation = Mockito.mock(FHIR.class);
    FHIRAsync asyncAnnotation = Mockito.mock(FHIRAsync.class);
    FHIRRequestFeature feature = new FHIRRequestFeature();

    private FHIRRequestFeatureTest() {
        // Not used
    }

    @AfterEach()
    void cleanup() {
        Mockito.reset(info);
        Mockito.reset(context);
        Mockito.reset(method);
        Mockito.reset(fhirAnnotation);
    }

    @Test
    void testFHIRResourceAnnotation() {
        Mockito.when(method.getAnnotation(FHIR.class)).thenReturn(this.fhirAnnotation);
        Mockito.when(info.getResourceMethod()).thenReturn(method);
        feature.configure(info, context);
        Mockito.verify(context, Mockito.times(1)).register(FHIRRequestFilter.class);
    }

    @Test
    void testFHIRClassAnnotation() {
        Mockito.when(method.getAnnotation(FHIR.class)).thenReturn(null);
        Mockito.when(info.getResourceClass()).thenAnswer(answer -> FHIRResourceClass.class);
        Mockito.when(info.getResourceMethod()).thenReturn(method);
        feature.configure(info, context);
        Mockito.verify(context, Mockito.times(1)).register(FHIRRequestFilter.class);
    }

    @Test
    void testAsyncFHIRResourceAnnotation() {
        Mockito.when(info.getResourceClass()).thenAnswer(answer -> FHIRAsyncResourceClass.class);
        Mockito.when(method.getAnnotation(FHIRAsync.class)).thenReturn(this.asyncAnnotation);
        Mockito.when(info.getResourceMethod()).thenReturn(method);
        feature.configure(info, context);
        Mockito.verify(context, Mockito.times(1)).register(FHIRAsyncRequestFilter.class);
    }

    @Test
    void testAsyncFHIRClassAnnotation() {
        Mockito.when(info.getResourceClass()).thenAnswer(answer -> FHIRAsyncResourceClass.class);
        Mockito.when(info.getResourceMethod()).thenReturn(method);
        feature.configure(info, context);
        Mockito.verify(context, Mockito.times(1)).register(FHIRAsyncRequestFilter.class);
    }


    @Test
    void testNoAnnotation() {
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
}
