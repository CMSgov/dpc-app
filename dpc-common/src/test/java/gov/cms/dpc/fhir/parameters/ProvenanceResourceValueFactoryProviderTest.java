package gov.cms.dpc.fhir.parameters;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Injector;
import gov.cms.dpc.fhir.annotations.ProvenanceHeader;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Parameter;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Provenance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
class ProvenanceResourceValueFactoryProviderTest {

    private static Injector injector = Mockito.mock(Injector.class);
    private static FhirContext ctx = FhirContext.forDstu3();
    private static ProvenanceResourceFactoryProvider factory;

    @BeforeAll
    static void setup() {
        factory = new ProvenanceResourceFactoryProvider(injector, ctx);
    }

    @Test
    void testCorrectFactory() {
        final Parameter parameter = Mockito.mock(Parameter.class);
        final ProvenanceHeader mockAnnotation = Mockito.mock(ProvenanceHeader.class);
        Mockito.when(parameter.getDeclaredAnnotation(ProvenanceHeader.class)).thenReturn(mockAnnotation);
        Mockito.when(parameter.getRawType()).thenAnswer(answer -> Patient.class);

        Provenance provenance = new Provenance();
        final String provString = ctx.newJsonParser().encodeResourceToString(provenance);
        final HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(httpRequest.getHeader(ProvenanceResourceValueFactory.PROVENANCE_HEADER)).thenReturn(provString);
        Mockito.when(injector.getInstance(HttpServletRequest.class)).thenReturn(httpRequest);

        final ContainerRequest request = Mockito.mock(ContainerRequest.class);
        final Function<ContainerRequest, Provenance> valueFunc = factory.getValueProvider(parameter);
        assertAll(() -> assertNotNull(valueFunc, "Should have factory"),
                () -> assertEquals(Provenance.class, valueFunc.apply(request).getClass(), "Should have provenance"));
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
}
