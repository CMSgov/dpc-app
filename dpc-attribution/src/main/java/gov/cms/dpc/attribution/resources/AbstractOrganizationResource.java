package gov.cms.dpc.attribution.resources;

import gov.cms.dpc.fhir.annotations.FHIR;
import io.dropwizard.jersey.params.BooleanParam;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Path("/Organization")
public abstract class AbstractOrganizationResource {

    protected AbstractOrganizationResource() {
        // Not used
    }

    @POST
    @FHIR
    public abstract Response createOrganization(Bundle bundle);

    @GET
    @Path("/{organizationID}/token/create")
    public abstract String getOrganizationToken(UUID organizationID, Optional<BooleanParam> refresh);

    @GET
    @Path("/{organizationID}/token/verify")
    public abstract boolean verifyOrganizationToken(@PathParam("organizationID") UUID organizationID, @QueryParam("token") String token);
}
