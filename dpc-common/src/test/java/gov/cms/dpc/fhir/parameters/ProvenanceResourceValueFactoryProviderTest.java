package gov.cms.dpc.fhir.parameters;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Injector;
import gov.cms.dpc.fhir.annotations.ProvenanceHeader;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Parameter;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
class ProvenanceResourceValueFactoryProviderTest {

    private static Injector injector = Mockito.mock(Injector.class);
    private static FhirContext ctx = Mockito.mock(FhirContext.class);
    private static ProvenanceResourceFactoryProvider factory;

    private ProvenanceResourceValueFactoryProviderTest() {
        // Not used
    }

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

        final ContainerRequest request = Mockito.mock(ContainerRequest.class);
        final Supplier<?> valueFactory = factory.getValueProvider(parameter).apply(request);
        assertAll(() -> assertNotNull(valueFactory, "Should have factory"),
                () -> assertEquals(ProvenanceResourceValueFactory.class, valueFactory.getClass(), "Should have provenance factory"));
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
