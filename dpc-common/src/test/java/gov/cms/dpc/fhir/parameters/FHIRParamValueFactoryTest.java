package gov.cms.dpc.fhir.parameters;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Injector;
import gov.cms.dpc.fhir.annotations.FHIRParameter;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Parameter;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
class FHIRParamValueFactoryTest {

    private static Injector injector = Mockito.mock(Injector.class);
    private static FhirContext ctx = Mockito.mock(FhirContext.class);

    private static FHIRParamValueFactory factory;

    FHIRParamValueFactoryTest() {
        // Not used
    }

    @BeforeAll
    static void setup() {
        factory = new FHIRParamValueFactory(injector, ctx);
    }

    @Test
    void testCorrectFactory() {
        final Parameter parameter = Mockito.mock(Parameter.class);
        final FHIRParameter mockAnnotation = Mockito.mock(FHIRParameter.class);
        Mockito.when(parameter.getDeclaredAnnotation(FHIRParameter.class)).thenReturn(mockAnnotation);
        Mockito.when(parameter.getRawType()).thenAnswer(answer -> Patient.class);

        final ContainerRequest request = Mockito.mock(ContainerRequest.class);
        final Factory<?> valueFactory = factory.getValueProvider(parameter).apply(request);
        assertAll(() -> assertNotNull(valueFactory, "Should have factory"),
                () -> assertEquals(ParamResourceFactory.class, valueFactory.getClass(), "Should have provenance factory"));
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
