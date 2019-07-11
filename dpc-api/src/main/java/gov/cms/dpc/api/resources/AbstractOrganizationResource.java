package gov.cms.dpc.api.resources;

import gov.cms.dpc.fhir.annotations.FHIR;
import org.hl7.fhir.dstu3.model.Organization;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.UUID;

@Path("/Organization")
@FHIR
public abstract class AbstractOrganizationResource {

    @GET
    @Path("/{organizationID}")
    public abstract Organization getOrganization(UUID organizationID);
}
