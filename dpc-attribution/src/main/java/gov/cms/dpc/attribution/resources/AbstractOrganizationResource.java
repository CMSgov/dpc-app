package gov.cms.dpc.attribution.resources;

import gov.cms.dpc.fhir.annotations.FHIR;
import io.dropwizard.jersey.params.BooleanParam;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/Organization")
public abstract class AbstractOrganizationResource {

    protected AbstractOrganizationResource() {
        // Not used
    }

    @POST
    @FHIR
    public abstract Response createOrganization(Bundle bundle);

    @GET
    @Path("/{organizationID}/token")
    public abstract Response getOrganizationToken(String organizationID, BooleanParam refresh);
}
