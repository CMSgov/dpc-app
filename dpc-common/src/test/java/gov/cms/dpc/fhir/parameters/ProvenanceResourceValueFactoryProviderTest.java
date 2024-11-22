package gov.cms.dpc.fhir.parameters;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Injector;
import gov.cms.dpc.fhir.annotations.ProvenanceHeader;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import jakarta.inject.Provider;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Parameter;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Provenance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import jakarta.servlet.http.HttpServletRequest;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith(BufferedLoggerHandler.class)
@DisplayName("Provenance resource value factory for Provider")
class ProvenanceResourceValueFactoryProviderTest {

    private static Provider<ProvenanceResourceValueFactory> provider = Mockito.mock(Provider.class);
    private static FhirContext ctx = FhirContext.forDstu3();
    private static ProvenanceResourceFactoryProvider factory;

    @BeforeAll
    static void setup() {
        factory = new ProvenanceResourceFactoryProvider(provider);
    }

    @Test
    @DisplayName("Create provenance factory 🥳")
    void testCorrectFactory() {
        final Parameter parameter = Mockito.mock(Parameter.class);
        final ProvenanceHeader mockAnnotation = Mockito.mock(ProvenanceHeader.class);
        Mockito.when(parameter.getDeclaredAnnotation(ProvenanceHeader.class)).thenReturn(mockAnnotation);
        Mockito.when(parameter.getRawType()).thenAnswer(answer -> Patient.class);  // Ensure correct type

        Provenance provenance = new Provenance();
        final String provString = ctx.newJsonParser().encodeResourceToString(provenance);

        final HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(httpRequest.getHeader(ProvenanceResourceValueFactory.PROVENANCE_HEADER)).thenReturn(provString);

        final ContainerRequest request = Mockito.mock(ContainerRequest.class);
        Mockito.when(request.getProperty(HttpServletRequest.class.getName())).thenReturn(httpRequest);  // Link request to HttpServletRequest

        // Set up the factory provider with the correct behavior
        ProvenanceResourceValueFactory valueFactory = new ProvenanceResourceValueFactory(() -> httpRequest, ctx);
        Mockito.when(provider.get()).thenReturn(valueFactory);

        // Now get the value provider function
        final Function<ContainerRequest, Provenance> valueFunc = factory.getValueProvider(parameter);

        assertAll(
            () -> assertNotNull(valueFunc, "Should have factory"),
            () -> assertEquals(Provenance.class, valueFunc.apply(request).getClass(), "Should have provenance")
        );
    }

    @Test
    @DisplayName("Create provenance factory with missing annotation 🤮")
    void testMissingAnnotation() {
        final Parameter parameter = Mockito.mock(Parameter.class);
        Mockito.when(parameter.getDeclaredAnnotation(ProvenanceHeader.class)).thenReturn(null);

        assertNull(factory.getValueProvider(parameter), "Factory should be null");
    }

    @Test
    @DisplayName("Create provenance factory with incorrect parameter 🤮")
    void testIncorrectParameter() {
        final Parameter parameter = Mockito.mock(Parameter.class);
        final ProvenanceHeader mockAnnotation = Mockito.mock(ProvenanceHeader.class);
        Mockito.when(parameter.getDeclaredAnnotation(ProvenanceHeader.class)).thenReturn(mockAnnotation);
        Mockito.when(parameter.getRawType()).thenAnswer(answer -> Mockito.class);

        assertNull(factory.getValueProvider(parameter), "Should not have factory for non-FHIR resource");
    }
}
