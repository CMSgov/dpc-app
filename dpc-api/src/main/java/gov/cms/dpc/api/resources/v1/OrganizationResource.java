package gov.cms.dpc.api.resources.v1;

import gov.cms.dpc.api.resources.AbstractOrganizationResource;
import org.hl7.fhir.dstu3.model.Organization;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.UUID;

public class OrganizationResource extends AbstractOrganizationResource {

    @Inject
    OrganizationResource() {
        // Not used
    }

    @GET
    @Path("/{organizationID}")
    @Override
    public Organization getOrganization(Organization organization, @PathParam("organizationID") UUID organizationID) {
        return organization;
    }
}
