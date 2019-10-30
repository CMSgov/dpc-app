package gov.cms.dpc.attribution.resources;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import gov.cms.dpc.attribution.AbstractAttributionTest;
import gov.cms.dpc.attribution.AttributionTestHelpers;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.testing.DataFactories;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.StringType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class OrganizationRegistrationTest extends AbstractAttributionTest {
    private final IGenericClient client;

    private OrganizationRegistrationTest() {
        this.client = AttributionTestHelpers.createFHIRClient(ctx, getServerURL());
    }

    @Test
    void testInvalidOrganization() {

        // Create a fake org
        final Organization resource = new Organization();
        resource.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-mbi");

        final Parameters parameters = new Parameters();
        parameters.addParameter().setResource(resource);

        final IOperationUntypedWithInput<Organization> operation = this.client
                .operation()
                .onType(Organization.class)
                .named("submit")
                .withParameters(parameters)
                .returnResourceType(Organization.class)
                .encodedJson();

        assertThrows(InternalErrorException.class, operation::execute, "Should fail with a 500 status");
    }

    @Test
    void testEmptyBundleSubmission() {

        final Parameters parameters = new Parameters();
        parameters.addParameter().setName("test").setValue(new StringType("nothing"));

        final IOperationUntypedWithInput<Organization> operation = this.client
                .operation()
                .onType(Organization.class)
                .named("submit")
                .withParameters(parameters)
                .returnResourceType(Organization.class)
                .encodedJson();

        assertThrows(UnprocessableEntityException.class, operation::execute, "Should be unprocessable");
    }
}
