package gov.cms.dpc.api.resources;

import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.Profiled;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Path("/Organization")
@FHIR
public abstract class AbstractOrganizationResource {

    @POST
    @Path("/$submit")
    public abstract Organization submitOrganization(@NotNull Bundle organizationBundle);

    @GET
    @Path("/{organizationID}")
    public abstract Organization getOrganization(@NotNull UUID organizationID);

    @DELETE
    @Path("/{organizationID}")
    public abstract Response deleteOrganization(@NotNull UUID organizationID);

    @PUT
    @Path("/{organizationID}")
    public abstract Organization updateOrganization(@NotNull UUID organizationID, @Valid @Profiled Organization organization);
}
