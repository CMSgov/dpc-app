package gov.cms.dpc.attribution.resources;

import gov.cms.dpc.fhir.annotations.FHIR;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.PractitionerRole;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Path("/PractitionerRole")
@FHIR
public abstract class AbstractPractitionerRoleResource {

    protected AbstractPractitionerRoleResource() {
        // Not used
    }

    @GET
    public abstract Bundle getPractitionerRoles(String organizationID, String providerID);

    @POST
    public abstract PractitionerRole submitPractitionerRole(PractitionerRole role);

    @GET
    @Path("/{roleID}")
    public abstract PractitionerRole getPractitionerRole(UUID roleID);

    @DELETE
    @Path("/{roleID}")
    public abstract Response deletePractitionerRole(UUID roleID);

    @PUT
    @Path("/{roleID}")
    public abstract PractitionerRole updatePractitionerRole(UUID roleID, PractitionerRole role);
}
