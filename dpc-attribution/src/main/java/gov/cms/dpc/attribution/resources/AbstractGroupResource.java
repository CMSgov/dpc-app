package gov.cms.dpc.attribution.resources;

import gov.cms.dpc.fhir.annotations.FHIR;
import org.hibernate.validator.constraints.NotEmpty;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Group;

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
    public abstract Bundle rosterSearch(@NotEmpty UUID organizationID, String providerNPI, String patientID);


    @POST
    @Path("/$submit")
    public abstract Response submitRoster(Bundle providerBundle);

    @GET
    @Path("/{rosterID}")
    public abstract Group getRoster(UUID rosterID);

    @GET
    @Path("/{groupID}/{patientID}")
    public abstract boolean isAttributed(String groupID, String patientID);


    @PUT
    @Path("/{groupID}/{patientID}")
    public abstract void attributePatient(String groupID, String patientID);

    @DELETE
    @Path("/{groupID}/{patientID}")
    public abstract void removeAttribution(String groupID, String patientID);
}
