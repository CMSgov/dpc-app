package gov.cms.dpc.fhir.parameters;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.fhir.annotations.FHIRParameter;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Parameter;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Provenance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(BufferedLoggerHandler.class)
class FHIRParamValueFactoryTest {

    private static FhirContext ctx = FhirContext.forDstu3();

    private static FHIRParamValueFactory factory;

    @BeforeAll
    static void setup() {
        factory = new FHIRParamValueFactory(ctx);
    }

    @Test
    void testCorrectFactory() throws IOException {
        final Parameter parameter = Mockito.mock(Parameter.class);
        final FHIRParameter mockAnnotation = Mockito.mock(FHIRParameter.class);
        Mockito.when(parameter.getDeclaredAnnotation(FHIRParameter.class)).thenReturn(mockAnnotation);
        Mockito.when(parameter.getRawType()).thenAnswer(answer -> Patient.class);

        Provenance provenance = new Provenance();
        final String provString = ctx.newJsonParser().encodeResourceToString(provenance);
        final HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);
        final ServletInputStream inputStream = Mockito.mock(ServletInputStream.class);
        Mockito.when(httpRequest.getHeader(ProvenanceResourceValueFactory.PROVENANCE_HEADER)).thenReturn(provString);
        Mockito.when(httpRequest.getInputStream()).thenReturn(inputStream);

        final ContainerRequest request = Mockito.mock(ContainerRequest.class);
        final Function<ContainerRequest, Object> valueFunc = factory.getValueProvider(parameter);
//        TODO: debug
//        assertAll(() -> assertNotNull(valueFunc, "Should have factory function"),
//                () -> assertEquals(Provenance.class, valueFunc.apply(request).getClass(), "Should have provenance"));
    }

    @Test
    void testMissingAnnotation() {
        final Parameter parameter = Mockito.mock(Parameter.class);
        Mockito.when(parameter.getDeclaredAnnotation(FHIRParameter.class)).thenReturn(null);
        Mockito.when(parameter.getRawType()).thenAnswer(answer -> Patient.class);

        assertNull(factory.getValueProvider(parameter), "Should not have factory");
    }

    @Test
    void testIncorrectParameter() {
        final Parameter parameter = Mockito.mock(Parameter.class);
        final FHIRParameter mockAnnotation = Mockito.mock(FHIRParameter.class);
        Mockito.when(parameter.getDeclaredAnnotation(FHIRParameter.class)).thenReturn(mockAnnotation);
        Mockito.when(parameter.getRawType()).thenAnswer(answer -> Mockito.class);

        assertNull(factory.getValueProvider(parameter), "Should not have factory");
    }
}
