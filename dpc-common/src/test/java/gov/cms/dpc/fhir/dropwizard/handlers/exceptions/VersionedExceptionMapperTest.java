package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import gov.cms.dpc.fhir.annotations.FHIR;
import io.dropwizard.jersey.validation.JerseyViolationException;
import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.glassfish.jersey.server.model.Invocable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VersionedExceptionMapperTest {

    @ParameterizedTest
    @ValueSource(strings = {"v1/Patient", "r4/Patient"})
    void routesUnexpectedExceptions(String path) {
        Response response = new DefaultFHIRExceptionMapper(fhirResourceInfo(), uriInfo(path))
                .toResponse(new IllegalStateException("error"));
        assertOutcome(response, path, 500);
    }

    @ParameterizedTest
    @ValueSource(strings = {"v1/Patient", "r4/Patient"})
    void routesHapiExceptions(String path) {
        BaseServerResponseException exception = hapiException();
        Response response = new HAPIExceptionMapper(fhirResourceInfo(), uriInfo(path)).toResponse(exception);
        assertOutcome(response, path, 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"v1/Patient", "r4/Patient"})
    void routesValidationExceptions(String path) {
        JerseyViolationException exception = new JerseyViolationException(Set.of(), mock(Invocable.class));
        Response response = new JerseyExceptionMapper(fhirResourceInfo(), uriInfo(path)).toResponse(exception);
        assertOutcome(response, path, 422);
    }

    @ParameterizedTest
    @ValueSource(strings = {"v1/Patient", "r4/Patient"})
    void routesPersistenceExceptions(String path) {
        Response response = new PersistenceExceptionMapper(fhirResourceInfo(), uriInfo(path))
                .toResponse(new PersistenceException("database details"));
        assertOutcome(response, path, 500);
        if (response.getEntity() instanceof org.hl7.fhir.r4.model.OperationOutcome outcome) {
            assertEquals("Internal server error", outcome.getIssueFirstRep().getDetails().getText());
        } else {
            org.hl7.fhir.dstu3.model.OperationOutcome outcome =
                    (org.hl7.fhir.dstu3.model.OperationOutcome) response.getEntity();
            assertEquals("Internal server error", outcome.getIssueFirstRep().getDetails().getText());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"v1/Patient", "r4/Patient"})
    void routesParserExceptions(String path) {
        Response response = new DataFormatExceptionMapper(fhirResourceInfo(), uriInfo(path))
                .toResponse(new DataFormatException("invalid JSON"));
        assertOutcome(response, path, 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"r4/Patient", "/r4/Patient", "r4", "v1/Patient", "v1"})
    void handlesErrorsBeforeResourceMatching(String path) {
        ResourceInfo unmatched = mock(ResourceInfo.class);
        Response response = new DefaultFHIRExceptionMapper(unmatched, uriInfo(path))
                .toResponse(new WebApplicationException("missing", 404));
        assertOutcome(response, path.startsWith("/") ? path.substring(1) : path, 404);
    }

    @ParameterizedTest
    @ValueSource(strings = {"r4ish/Patient", "prefix/r4/Patient", "v1/Patient"})
    void matchesOnlyTheFirstCompletePathSegment(String path) {
        Response response = new DefaultFHIRExceptionMapper(fhirResourceInfo(), uriInfo(path))
                .toResponse(new IllegalStateException("error"));
        assertInstanceOf(org.hl7.fhir.dstu3.model.OperationOutcome.class, response.getEntity());
    }

    @ParameterizedTest
    @ValueSource(strings = {"v1/version", "r4/version", "admin/version"})
    void preservesNonFhirErrorResponses(String path) {
        ResourceInfo info = mock(ResourceInfo.class);
        when(info.getResourceClass()).thenAnswer(invocation -> NonFHIRResource.class);
        Response response = new DefaultFHIRExceptionMapper(info, uriInfo(path))
                .toResponse(new IllegalStateException("error"));
        assertEquals(500, response.getStatus());
        assertInstanceOf(io.dropwizard.jersey.errors.ErrorMessage.class, response.getEntity());
    }

    @ParameterizedTest
    @ValueSource(strings = {"v1/Patient", "r4/Patient"})
    void preservesRedirects(String path) {
        Response redirect = Response.status(302).header("Location", "/elsewhere").build();
        Response response = new DefaultFHIRExceptionMapper(fhirResourceInfo(), uriInfo(path))
                .toResponse(new WebApplicationException(redirect));
        assertSame(redirect, response);
    }

    @Test
    void preservesMatchingHapiOutcome() {
        BaseServerResponseException exception = hapiException();
        org.hl7.fhir.r4.model.OperationOutcome outcome = new org.hl7.fhir.r4.model.OperationOutcome();
        when(exception.getOperationOutcome()).thenReturn(outcome);
        Response response = new HAPIExceptionMapper(fhirResourceInfo(), uriInfo("r4/Patient"))
                .toResponse(exception);
        assertSame(outcome, response.getEntity());
    }

    @Test
    void handlesStu3OutcomeOnR4Request() {
        BaseServerResponseException exception = hapiException();
        when(exception.getOperationOutcome()).thenReturn(new org.hl7.fhir.dstu3.model.OperationOutcome());
        Response response = new HAPIExceptionMapper(fhirResourceInfo(), uriInfo("r4/Patient"))
                .toResponse(exception);
        assertOutcome(response, "r4/Patient", 400);
        org.hl7.fhir.r4.model.OperationOutcome outcome = (org.hl7.fhir.r4.model.OperationOutcome) response.getEntity();
        assertEquals("invalid request", outcome.getIssueFirstRep().getDiagnostics());
    }

    private BaseServerResponseException hapiException() {
        BaseServerResponseException exception = mock(BaseServerResponseException.class);
        when(exception.getStatusCode()).thenReturn(400);
        when(exception.getMessage()).thenReturn("invalid request");
        return exception;
    }

    private ResourceInfo fhirResourceInfo() {
        ResourceInfo info = mock(ResourceInfo.class);
        when(info.getResourceClass()).thenAnswer(invocation -> FHIRResource.class);
        return info;
    }

    private UriInfo uriInfo(String path) {
        UriInfo info = mock(UriInfo.class);
        when(info.getPath()).thenReturn(path);
        return info;
    }

    private void assertOutcome(Response response, String path, int status) {
        assertEquals(status, response.getStatus());
        if (path.equals("r4") || path.startsWith("r4/")) {
            assertInstanceOf(org.hl7.fhir.r4.model.OperationOutcome.class, response.getEntity());
        } else {
            assertInstanceOf(org.hl7.fhir.dstu3.model.OperationOutcome.class, response.getEntity());
        }
    }

    @FHIR
    static class FHIRResource {
    }

    static class NonFHIRResource {
    }
}
