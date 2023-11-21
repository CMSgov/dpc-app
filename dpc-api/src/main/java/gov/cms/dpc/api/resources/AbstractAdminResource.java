package gov.cms.dpc.api.resources;

import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.hl7.fhir.dstu3.model.Bundle;

import gov.cms.dpc.fhir.annotations.FHIR;

@Path("/Admin")
@FHIR
public abstract class AbstractAdminResource {
    @GET
    @Path("/Organization")
    public abstract Bundle getOrganizations(@NotNull @QueryParam(value="ids") String ids);
}
