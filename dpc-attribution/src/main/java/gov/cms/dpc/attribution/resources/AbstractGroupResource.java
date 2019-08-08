package gov.cms.dpc.attribution.resources;

import gov.cms.dpc.fhir.annotations.FHIR;
import org.hibernate.validator.constraints.NotEmpty;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Group;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Path("/Group")
@FHIR
public abstract class AbstractGroupResource {

    protected AbstractGroupResource() {
        // Not used
    }

    @POST
    public abstract Response createRoster(Group attributionRoster);

    @GET
    public abstract Bundle rosterSearch(@NotEmpty String organizationID, String providerNPI, String patientID);

    @GET
    @Path("/{rosterID}")
    public abstract Group getRoster(UUID rosterID);

    @GET
    @Path("/{rosterID}/$patients")
    public abstract Bundle getAttributedPatients(@NotNull UUID rosterID);

    @PUT
    @Path("/{rosterID}")
    public abstract Group updateRoster(UUID rosterID, Group groupUpdate);

    @DELETE
    @Path("/{rosterID}")
    public abstract Response deleteRoster(UUID rosterID);
}
