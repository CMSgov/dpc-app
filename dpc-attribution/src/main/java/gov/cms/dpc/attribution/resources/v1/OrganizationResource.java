package gov.cms.dpc.attribution.resources.v1;

import gov.cms.dpc.attribution.resources.AbstractOrganizationResource;
import org.hl7.fhir.dstu3.model.Organization;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

public class OrganizationResource extends AbstractOrganizationResource {

    @Inject
    OrganizationResource() {
        // Not used
    }

    @Override
    public Response createOrganization(Organization organization) {
        return null;
    }
}
