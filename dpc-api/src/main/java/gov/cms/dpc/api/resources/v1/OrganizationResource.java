package gov.cms.dpc.api.resources.v1;

import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.resources.AbstractOrganizationResource;
import gov.cms.dpc.fhir.FHIRExtractors;
import io.dropwizard.auth.Auth;
import org.hl7.fhir.dstu3.model.Organization;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.UUID;

public class OrganizationResource extends AbstractOrganizationResource {

    @Inject
    OrganizationResource() {
        // Not used
    }

    @GET
    @Path("/{organizationID}")
    @RolesAllowed({"*"})
    @Override
    public Organization getOrganization(@Auth OrganizationPrincipal principal, @PathParam("organizationID") UUID organizationID) {
        final Organization organization = principal.getOrganization();
        if (!FHIRExtractors.getEntityUUID(organization.getId()).equals(organizationID)) {
            throw new WebApplicationException("Unauthorized", Response.Status.UNAUTHORIZED);
        }
        return organization;
    }
}
