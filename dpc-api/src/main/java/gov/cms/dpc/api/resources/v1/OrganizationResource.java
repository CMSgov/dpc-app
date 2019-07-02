package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.resources.AbstractOrganizationResource;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.ResourceType;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.UUID;

public class OrganizationResource extends AbstractOrganizationResource {

    private final IGenericClient client;

    @Inject
    OrganizationResource(IGenericClient client) {
        this.client = client;
    }

    @GET
    @Path("/{organizationID}")
    @PathAuthorizer(type = ResourceType.Organization, pathParam = "organizationID")
    @Override
    public Organization getOrganization(@PathParam("organizationID") UUID organizationID) {
        return this.client
                .read()
                .resource(Organization.class)
                .withId(organizationID.toString())
                .encodedJson()
                .execute();
    }
}
