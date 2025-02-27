package gov.cms.dpc.fhir.parameters;

import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import gov.cms.dpc.fhir.annotations.FHIRParameter;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Parameter;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import javax.ws.rs.WebApplicationException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
class ParamResourceFactoryTest {

    private ParamResourceFactoryTest() {
        // Not used
    }

    @Test
    void testNonParameter() {
        final IParser parser = Mockito.mock(IParser.class);
        final ContainerRequest mockRequest = Mockito.mock(ContainerRequest.class);
        Mockito.when(parser.parseResource(Parameters.class, mockRequest.getEntityStream())).thenThrow(DataFormatException.class);

        final ParamResourceFactory factory = new ParamResourceFactory(mockRequest, null, parser);
        final WebApplicationException exception = assertThrows(WebApplicationException.class, factory::provide, "Should throw exception");
        assertAll(() -> assertEquals(HttpStatus.BAD_REQUEST_400, exception.getResponse().getStatus(), "Should be a bad request"),
                () -> assertEquals("Resource type must be `Parameters`", exception.getMessage(), "Should have wrong resource message"));
    }

    @Test
    void testUnnamedResource() {
        final Parameters parameters = new Parameters();
        final Patient dummyPatient = new Patient();
        parameters.addParameter().setResource(dummyPatient);

        final Parameter parameter = Mockito.mock(Parameter.class);
        final FHIRParameter annotation = Mockito.mock(FHIRParameter.class);
        Mockito.when(annotation.name()).thenReturn("");
        Mockito.when(parameter.getAnnotation(FHIRParameter.class)).thenReturn(annotation);
        Mockito.when(parameter.getRawType()).thenAnswer(answer -> Patient.class);

        final IParser parser = Mockito.mock(IParser.class);
        final ContainerRequest mockRequest = Mockito.mock(ContainerRequest.class);
        Mockito.when(parser.parseResource(Parameters.class, mockRequest.getEntityStream())).thenReturn(parameters);
        final ParamResourceFactory factory = new ParamResourceFactory(mockRequest, parameter, parser);

        assertTrue(dummyPatient.equalsDeep((Patient) factory.provide()), "Should have returned dummy patient");
    }

    @Test
    void testNamedResource() {
        final Parameters parameters = new Parameters();
        final Patient unnamedPatient = new Patient();
        final Patient namedPatient = new Patient();
        namedPatient.addName().setFamily("Patient").addGiven("Test");

        parameters.addParameter().setResource(unnamedPatient);
        parameters.addParameter().setResource(namedPatient).setName("named");

        final Parameter parameter = Mockito.mock(Parameter.class);
        final FHIRParameter annotation = Mockito.mock(FHIRParameter.class);
        Mockito.when(annotation.name()).thenReturn("named");
        Mockito.when(parameter.getAnnotation(FHIRParameter.class)).thenReturn(annotation);
        Mockito.when(parameter.getRawType()).thenAnswer(answer -> Patient.class);

        final IParser parser = Mockito.mock(IParser.class);
        final ContainerRequest mockRequest = Mockito.mock(ContainerRequest.class);
        Mockito.when(parser.parseResource(Parameters.class, mockRequest.getEntityStream())).thenReturn(parameters);
        final ParamResourceFactory factory = new ParamResourceFactory(mockRequest, parameter, parser);
        assertAll(() -> assertTrue(namedPatient.equalsDeep((Patient) factory.provide()), "Should have returned dummy patient"),
                () -> assertFalse(unnamedPatient.equalsDeep((Patient) factory.provide()), "Should have returned dummy patient"));
    }

    @Test
    void testMismatchedParameterType() {
        final Parameters parameters = new Parameters();
        final Patient dummyPatient = new Patient();
        parameters.addParameter().setResource(dummyPatient);

        final Parameter parameter = Mockito.mock(Parameter.class);
        final FHIRParameter annotation = Mockito.mock(FHIRParameter.class);
        Mockito.when(annotation.name()).thenReturn("");
        Mockito.when(parameter.getAnnotation(FHIRParameter.class)).thenReturn(annotation);
        Mockito.when(parameter.getRawType()).thenAnswer(answer -> Practitioner.class);

        final IParser parser = Mockito.mock(IParser.class);
        final ContainerRequest mockRequest = Mockito.mock(ContainerRequest.class);
        Mockito.when(parser.parseResource(Parameters.class, mockRequest.getEntityStream())).thenReturn(parameters);
        final ParamResourceFactory factory = new ParamResourceFactory(mockRequest, parameter, parser);

        final WebApplicationException exception = assertThrows(WebApplicationException.class, factory::provide, "Should throw an exception");

        assertAll(() -> assertEquals(HttpStatus.BAD_REQUEST_400, exception.getResponse().getStatus(), "Should be a bad request"),
                () -> assertEquals("Provided resource must be: `Practitioner`, not `Patient`", exception.getMessage(), "Should have useful message"));
    }

    @Test
    void testMissingResource() {
        final Parameters parameters = new Parameters();
        final Patient dummyPatient = new Patient();
        parameters.addParameter().setResource(dummyPatient);

        final Parameter parameter = Mockito.mock(Parameter.class);
        final FHIRParameter annotation = Mockito.mock(FHIRParameter.class);
        Mockito.when(annotation.name()).thenReturn("missing");
        Mockito.when(parameter.getAnnotation(FHIRParameter.class)).thenReturn(annotation);
        Mockito.when(parameter.getRawType()).thenAnswer(answer -> Patient.class);

        final ContainerRequest mockRequest = Mockito.mock(ContainerRequest.class);
        final IParser parser = Mockito.mock(IParser.class);
        Mockito.when(parser.parseResource(Parameters.class, mockRequest.getEntityStream())).thenReturn(parameters);
        final ParamResourceFactory factory = new ParamResourceFactory(mockRequest, parameter, parser);

        final WebApplicationException exception = assertThrows(WebApplicationException.class, factory::provide, "Should throw an exception");
        assertAll(() -> assertEquals(HttpStatus.BAD_REQUEST_400, exception.getResponse().getStatus(), "Should be a bad request"),
                () -> assertEquals("Cannot find matching parameter named `missing`", exception.getMessage(), "Should output which parameter is missing"));
    }
}
