package gov.cms.dpc.attribution.resources;

import gov.cms.dpc.fhir.annotations.FHIR;
import org.hl7.fhir.dstu3.model.Bundle;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/Group")
public abstract class AbstractGroupResource {

    protected AbstractGroupResource() {
        // Not used
    }

    @POST
    @Path("/$submit")
    @FHIR
    public abstract Response submitRoster(Bundle providerBundle);

    @GET
    @Path("/{groupID}")
    public abstract List<String> getAttributedPatients(String groupID);

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
