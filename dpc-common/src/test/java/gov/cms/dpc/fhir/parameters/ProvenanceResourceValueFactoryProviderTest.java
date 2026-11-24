package gov.cms.dpc.fhir.parameters;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Injector;
import gov.cms.dpc.fhir.annotations.ProvenanceHeader;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Parameter;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
class ProvenanceResourceValueFactoryProviderTest {

    private static final Injector injector = Mockito.mock(Injector.class);
    private static final FhirContext stu3Ctx = FhirContext.forDstu3();
    private static final FhirContext r4Ctx = FhirContext.forR4();
    private static ProvenanceResourceFactoryProvider factory;

    @BeforeAll
    static void setup() {
        factory = new ProvenanceResourceFactoryProvider(injector, stu3Ctx,  r4Ctx);
    }

    @Test
    void testCorrectFactoryStu3() {
        final Parameter parameter = Mockito.mock(Parameter.class);
        final ProvenanceHeader mockAnnotation = Mockito.mock(ProvenanceHeader.class);
        Mockito.when(parameter.getDeclaredAnnotation(ProvenanceHeader.class)).thenReturn(mockAnnotation);
        Mockito.when(parameter.getRawType()).thenAnswer(answer -> org.hl7.fhir.dstu3.model.Provenance.class);

        org.hl7.fhir.dstu3.model.Provenance provenance = new org.hl7.fhir.dstu3.model.Provenance();
        final String provString = stu3Ctx.newJsonParser().encodeResourceToString(provenance);
        final HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(httpRequest.getHeader(ProvenanceResourceValueFactory.PROVENANCE_HEADER)).thenReturn(provString);
        Mockito.when(injector.getInstance(HttpServletRequest.class)).thenReturn(httpRequest);

        final ContainerRequest request = Mockito.mock(ContainerRequest.class);
        final Function<ContainerRequest, IBaseResource> valueFunc = factory.getValueProvider(parameter);
        assertAll(() -> assertNotNull(valueFunc, "Should have factory"),
                () -> assertEquals(org.hl7.fhir.dstu3.model.Provenance.class, valueFunc.apply(request).getClass(), "Should have STU3 provenance"));
    }

    @Test
    void testMissingAnnotation() {
        final Parameter parameter = Mockito.mock(Parameter.class);
        Mockito.when(parameter.getDeclaredAnnotation(ProvenanceHeader.class)).thenReturn(null);

        assertNull(factory.getValueProvider(parameter), "Factory should be null");
    }

    @Test
    void testIncorrectParameter() {
        final Parameter parameter = Mockito.mock(Parameter.class);
        final ProvenanceHeader mockAnnotation = Mockito.mock(ProvenanceHeader.class);
        Mockito.when(parameter.getDeclaredAnnotation(ProvenanceHeader.class)).thenReturn(mockAnnotation);
        Mockito.when(parameter.getRawType()).thenAnswer(answer -> Mockito.class);

        assertNull(factory.getValueProvider(parameter), "Should not have factory for non-FHIR resource");
    }

    @Test
    void testCorrectFactoryR4() {
        final Parameter parameter = Mockito.mock(Parameter.class);
        final ProvenanceHeader mockAnnotation = Mockito.mock(ProvenanceHeader.class);
        Mockito.when(parameter.getDeclaredAnnotation(ProvenanceHeader.class)).thenReturn(mockAnnotation);
        Mockito.when(parameter.getRawType()).thenAnswer(answer -> org.hl7.fhir.r4.model.Provenance.class);

        org.hl7.fhir.r4.model.Provenance provenance = new org.hl7.fhir.r4.model.Provenance();
        final String provString = r4Ctx.newJsonParser().encodeResourceToString(provenance);
        final HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(httpRequest.getHeader(ProvenanceResourceValueFactory.PROVENANCE_HEADER)).thenReturn(provString);
        Mockito.when(injector.getInstance(HttpServletRequest.class)).thenReturn(httpRequest);

        final ContainerRequest request = Mockito.mock(ContainerRequest.class);
        final Function<ContainerRequest, IBaseResource> valueFunc = factory.getValueProvider(parameter);
        assertAll(() -> assertNotNull(valueFunc, "Should have factory"),
                () -> assertEquals(org.hl7.fhir.r4.model.Provenance.class, valueFunc.apply(request).getClass(), "Should have R4 provenance"));
    }
}
