package gov.cms.dpc.api.resources;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.hl7.fhir.dstu3.model.Bundle;

import gov.cms.dpc.fhir.annotations.FHIR;

@Path("/Admin")
@FHIR
public abstract class AbstractAdminResource {
    @GET
    @Path("/Organization")
    public abstract Bundle getOrganizations(@NotNull @QueryParam(value="ids") String ids);
}
