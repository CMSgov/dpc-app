package gov.cms.dpc.fhir.parameters;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Injector;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
class ProvenanceResourceValueFactoryTest {

    private static final FhirContext stu3Ctx = FhirContext.forDstu3();
    private static final FhirContext r4Ctx = FhirContext.forR4();

    private ProvenanceResourceValueFactoryTest() {
        // Not used
    }

    @Test
    void testValidProvenanceStu3() {
        final org.hl7.fhir.dstu3.model.Provenance provenance = new org.hl7.fhir.dstu3.model.Provenance();
        provenance.addTarget(new org.hl7.fhir.dstu3.model.Reference("Organization/nothing-real"));
        final String provString = stu3Ctx.newJsonParser().encodeResourceToString(provenance);

        final HttpServletRequest mock = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mock.getHeader(ProvenanceResourceValueFactory.PROVENANCE_HEADER)).thenReturn(provString);

        final Injector mockInjector = Mockito.mock(Injector.class);
        Mockito.when(mockInjector.getInstance(HttpServletRequest.class)).thenReturn(mock);

        final ProvenanceResourceValueFactory factory = new ProvenanceResourceValueFactory(mockInjector, stu3Ctx, provenance.getClass());

        final IBaseResource prov2 = factory.provide();
        assertInstanceOf(org.hl7.fhir.dstu3.model.Provenance.class, prov2, "Should return a STU3 Provenance");
        assertTrue(provenance.equalsDeep((org.hl7.fhir.dstu3.model.Provenance) prov2), "Should have matching provenance resource");
    }

    @Test
    void testMissingHeaderStu3() {
        final HttpServletRequest mock = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mock.getHeader(ProvenanceResourceValueFactory.PROVENANCE_HEADER)).thenReturn(null);

        final Injector mockInjector = Mockito.mock(Injector.class);
        Mockito.when(mockInjector.getInstance(HttpServletRequest.class)).thenReturn(mock);

        final ProvenanceResourceValueFactory factory = new ProvenanceResourceValueFactory(mockInjector, stu3Ctx, org.hl7.fhir.dstu3.model.Provenance.class);

        final WebApplicationException exception = assertThrows(WebApplicationException.class, factory::provide, "Should throw an exception");
        assertAll(() -> assertEquals(HttpStatus.BAD_REQUEST_400, exception.getResponse().getStatus(), "Should be a bad request"),
                () -> assertEquals(String.format("Must have %s header", ProvenanceResourceValueFactory.PROVENANCE_HEADER), exception.getMessage(), "Should show missing header"));
    }

    @Test
    void testInvalidProvenanceStu3() {
        final org.hl7.fhir.dstu3.model.Patient patient = new org.hl7.fhir.dstu3.model.Patient();
        final String provString = stu3Ctx.newJsonParser().encodeResourceToString(patient);

        final HttpServletRequest mock = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mock.getHeader(ProvenanceResourceValueFactory.PROVENANCE_HEADER)).thenReturn(provString);

        final Injector mockInjector = Mockito.mock(Injector.class);
        Mockito.when(mockInjector.getInstance(HttpServletRequest.class)).thenReturn(mock);

        final ProvenanceResourceValueFactory factory = new ProvenanceResourceValueFactory(mockInjector, stu3Ctx, org.hl7.fhir.dstu3.model.Provenance.class);

        final WebApplicationException exception = assertThrows(WebApplicationException.class, factory::provide, "Should throw an exception");
        assertAll(() -> assertEquals(HttpStatus.BAD_REQUEST_400, exception.getResponse().getStatus(), "Should be a bad request"),
                () -> assertEquals("Cannot parse FHIR `Provenance` resource", exception.getMessage(), "Should show missing header"));
    }

    @Test
    void testValidProvenanceR4() {
        final org.hl7.fhir.r4.model.Provenance provenance = new org.hl7.fhir.r4.model.Provenance();
        provenance.addTarget(new org.hl7.fhir.r4.model.Reference("Organization/nothing-real"));
        final String provString = r4Ctx.newJsonParser().encodeResourceToString(provenance);

        final HttpServletRequest mock = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mock.getHeader(ProvenanceResourceValueFactory.PROVENANCE_HEADER)).thenReturn(provString);

        final Injector mockInjector = Mockito.mock(Injector.class);
        Mockito.when(mockInjector.getInstance(HttpServletRequest.class)).thenReturn(mock);

        final ProvenanceResourceValueFactory factory = new ProvenanceResourceValueFactory(mockInjector, r4Ctx, provenance.getClass());

        final IBaseResource result = factory.provide();
        assertInstanceOf(org.hl7.fhir.r4.model.Provenance.class, result, "Should return an R4 Provenance");
        assertTrue(provenance.equalsDeep((org.hl7.fhir.r4.model.Provenance) result), "Should have matching provenance resource");
    }

    @Test
    void testMissingHeaderR4() {
        final HttpServletRequest mock = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mock.getHeader(ProvenanceResourceValueFactory.PROVENANCE_HEADER)).thenReturn(null);

        final Injector mockInjector = Mockito.mock(Injector.class);
        Mockito.when(mockInjector.getInstance(HttpServletRequest.class)).thenReturn(mock);

        final ProvenanceResourceValueFactory factory = new ProvenanceResourceValueFactory(mockInjector, r4Ctx, org.hl7.fhir.r4.model.Provenance.class);

        final WebApplicationException exception = assertThrows(WebApplicationException.class, factory::provide, "Should throw an exception");
        assertAll(() -> assertEquals(HttpStatus.BAD_REQUEST_400, exception.getResponse().getStatus(), "Should be a bad request"),
                () -> assertEquals(String.format("Must have %s header", ProvenanceResourceValueFactory.PROVENANCE_HEADER), exception.getMessage(), "Should show missing header"));
    }

    @Test
    void testInvalidProvenanceR4() {
        final org.hl7.fhir.r4.model.Patient patient = new org.hl7.fhir.r4.model.Patient();
        final String provString = r4Ctx.newJsonParser().encodeResourceToString(patient);

        final HttpServletRequest mock = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mock.getHeader(ProvenanceResourceValueFactory.PROVENANCE_HEADER)).thenReturn(provString);

        final Injector mockInjector = Mockito.mock(Injector.class);
        Mockito.when(mockInjector.getInstance(HttpServletRequest.class)).thenReturn(mock);

        final ProvenanceResourceValueFactory factory = new ProvenanceResourceValueFactory(mockInjector, r4Ctx, org.hl7.fhir.r4.model.Provenance.class);

        final WebApplicationException exception = assertThrows(WebApplicationException.class, factory::provide, "Should throw an exception");
        assertAll(() -> assertEquals(HttpStatus.BAD_REQUEST_400, exception.getResponse().getStatus(), "Should be a bad request"),
                () -> assertEquals("Cannot parse FHIR `Provenance` resource", exception.getMessage(), "Should show missing header"));
    }
}
