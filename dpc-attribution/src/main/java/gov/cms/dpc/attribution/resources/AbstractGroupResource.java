package gov.cms.dpc.attribution.resources;

import gov.cms.dpc.fhir.annotations.FHIR;

import javax.ws.rs.*;
import java.util.Set;

@Path("/Group")
@FHIR
public abstract class AbstractGroupResource {

    protected AbstractGroupResource() {
        // Not used
    }

    @GET
    @Path("/{groupID}")
    public abstract Set<String> getAttributedPatients(String groupID);

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
