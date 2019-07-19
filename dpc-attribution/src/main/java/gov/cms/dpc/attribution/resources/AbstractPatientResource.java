package gov.cms.dpc.attribution.resources;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;

import javax.ws.rs.core.Response;
import java.util.UUID;

public abstract class AbstractPatientResource {

    protected AbstractPatientResource() {
        // Not used
    }

    public abstract Bundle searchPatients(String patientID, String organizationToken);

    public abstract Response createPatient(Patient patient);

    public abstract Patient getPatient(UUID patientID);

    public abstract Response deletePatient(UUID patientID);

    public abstract Patient updatePatient(UUID patientID, Patient patient);
}
