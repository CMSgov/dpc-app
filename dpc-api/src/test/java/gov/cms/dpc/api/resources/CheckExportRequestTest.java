package gov.cms.dpc.api.resources;

import gov.cms.dpc.api.resources.v1.GroupResource;
import org.eclipse.jetty.http.HttpHeader;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;

import static gov.cms.dpc.fhir.FHIRMediaTypes.FHIR_JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckExportRequestTest {
    @Test
    void testCheckExportRequestWithValid() {
        final var headers = new MultivaluedHashMap<String, String>();
        headers.add(HttpHeader.ACCEPT.toString(), FHIR_JSON);
        headers.add(GroupResource.PREFER_HEADER, GroupResource.PREFER_RESPOND_ASYNC);

        // A valid request
        final var optionalOperationOutcome = GroupResource.checkExportRequest(headers, ResourceType.Patient.toString(), null, null);
        assertTrue(optionalOperationOutcome.isEmpty(), "Expected all parameters to be valid ");
    }

    @Test
    void testCheckExportRequestWithInvalidOutputFormat() {
        final var headers = new MultivaluedHashMap<String, String>();
        headers.add(HttpHeader.ACCEPT.toString(), FHIR_JSON);
        headers.add(GroupResource.PREFER_HEADER, GroupResource.PREFER_RESPOND_ASYNC);

        // A valid request
        final var optionalOperationOutcome = GroupResource.checkExportRequest(headers, ResourceType.Patient.toString(), "foo", null);
        assertTrue(optionalOperationOutcome.isPresent(), "Expected an operationOutcome");
    }

    @Test
    void testCheckExportRequestWithoutTypes() {
        final var headers = new MultivaluedHashMap<String, String>();
        headers.add(HttpHeader.ACCEPT.toString(), FHIR_JSON);
        headers.add(GroupResource.PREFER_HEADER, GroupResource.PREFER_RESPOND_ASYNC);

        // A valid request
        final var optionalOperationOutcome = GroupResource.checkExportRequest(headers, null, null, null);
        assertTrue(optionalOperationOutcome.isEmpty(), "Expected all parameters to be valid ");
    }

    @Test
    void testCheckExportRequestWithMultiple() {
        final var headers = new MultivaluedHashMap<String, String>();
        headers.add(HttpHeader.ACCEPT.toString(), FHIR_JSON);
        headers.add(HttpHeader.ACCEPT.toString(), MediaType.APPLICATION_JSON);
        headers.add(GroupResource.PREFER_HEADER, GroupResource.PREFER_RESPOND_ASYNC);
        final var resourceTypes = ResourceType.Patient.toString() + GroupResource.LIST_DELIM + ResourceType.ExplanationOfBenefit.toString();

        // A valid request with multiple
        final var optionalOperationOutcome = GroupResource.checkExportRequest(headers, resourceTypes, null,null);
        assertTrue(optionalOperationOutcome.isEmpty(), "Expected all parameters to be valid ");
    }

    @Test
    void testCheckExportRequestWithInvalidParams() {
        final var headers = new MultivaluedHashMap<String, String>();

        // A valid request
        final var optionalResponse = GroupResource.checkExportRequest(headers, ResourceType.Account.toString(), null,null);
        assertTrue(optionalResponse.isPresent(), "Expected a response");
        optionalResponse.ifPresent(outcome -> {
            assertEquals(3, outcome.getIssue().size());
        });
    }
}
