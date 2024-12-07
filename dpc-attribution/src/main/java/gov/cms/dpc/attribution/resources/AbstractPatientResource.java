package gov.cms.dpc.attribution.resources;

import gov.cms.dpc.fhir.annotations.FHIR;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Patient;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Path("/Patient")
@FHIR
public abstract class AbstractPatientResource {

    protected AbstractPatientResource() {
        // Not used
    }

    @GET
    public abstract List<Patient> searchPatients(UUID resourceID, String patientMBI, String organizationReference);

    @POST
    public abstract Response createPatient(Patient patient);

    @POST
    @Path("/$submit")
    public abstract List<Patient> bulkSubmitPatients(Parameters params);

    @GET
    @Path("/{patientID}")
    public abstract Patient getPatient(UUID patientID);

    @DELETE
    @Path("/{patientID}")
    public abstract Response deletePatient(UUID patientID);

    @PUT
    @Path("/{patientID}")
    public abstract Response updatePatient(UUID patientID, Patient patient);
}
