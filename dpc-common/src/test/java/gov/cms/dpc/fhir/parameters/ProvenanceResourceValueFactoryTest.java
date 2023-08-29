package gov.cms.dpc.fhir.parameters;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Injector;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Provenance;
import org.hl7.fhir.dstu3.model.Reference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
class ProvenanceResourceValueFactoryTest {

    private static final FhirContext ctx = FhirContext.forDstu3();

    private ProvenanceResourceValueFactoryTest() {
        // Not used
    }

    @Test
    void testValidProvenance() {
        final Provenance provenance = new Provenance();
        provenance.addTarget(new Reference("Organization/nothing-real"));
        final String provString = ctx.newJsonParser().encodeResourceToString(provenance);

        final HttpServletRequest mock = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mock.getHeader(ProvenanceResourceValueFactory.PROVENANCE_HEADER)).thenReturn(provString);

        final Injector mockInjector = Mockito.mock(Injector.class);
        Mockito.when(mockInjector.getInstance(HttpServletRequest.class)).thenReturn(mock);

        final ProvenanceResourceValueFactory factory = new ProvenanceResourceValueFactory(mockInjector, ctx);

        final Provenance prov2 = factory.provide();
        assertTrue(provenance.equalsDeep(prov2), "Should have matching provenance resource");
    }

    @Test
    void testMissingHeader() {
        final HttpServletRequest mock = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mock.getHeader(ProvenanceResourceValueFactory.PROVENANCE_HEADER)).thenReturn(null);

        final Injector mockInjector = Mockito.mock(Injector.class);
        Mockito.when(mockInjector.getInstance(HttpServletRequest.class)).thenReturn(mock);

        final ProvenanceResourceValueFactory factory = new ProvenanceResourceValueFactory(mockInjector, ctx);

        final WebApplicationException exception = assertThrows(WebApplicationException.class, factory::provide, "Should throw an exception");
        assertAll(() -> assertEquals(HttpStatus.BAD_REQUEST_400, exception.getResponse().getStatus(), "Should be a bad request"),
                () -> assertEquals(String.format("Must have %s header", ProvenanceResourceValueFactory.PROVENANCE_HEADER), exception.getMessage(), "Should show missing header"));
    }

    @Test
    void testInvalidProvenance() {

        final Patient patient = new Patient();
        final String provString = ctx.newJsonParser().encodeResourceToString(patient);

        final HttpServletRequest mock = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mock.getHeader(ProvenanceResourceValueFactory.PROVENANCE_HEADER)).thenReturn(provString);

        final Injector mockInjector = Mockito.mock(Injector.class);
        Mockito.when(mockInjector.getInstance(HttpServletRequest.class)).thenReturn(mock);

        final ProvenanceResourceValueFactory factory = new ProvenanceResourceValueFactory(mockInjector, ctx);

        final WebApplicationException exception = assertThrows(WebApplicationException.class, factory::provide, "Should throw an exception");
        assertAll(() -> assertEquals(HttpStatus.BAD_REQUEST_400, exception.getResponse().getStatus(), "Should be a bad request"),
                () -> assertEquals("Cannot parse FHIR `Provenance` resource", exception.getMessage(), "Should show missing header"));
    }
}
