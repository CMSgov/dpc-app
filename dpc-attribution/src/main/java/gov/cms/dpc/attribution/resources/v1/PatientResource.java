package gov.cms.dpc.attribution.resources.v1;

import gov.cms.dpc.attribution.jdbi.PatientDAO;
import gov.cms.dpc.attribution.resources.AbstractPatientResource;
import gov.cms.dpc.common.entities.PatientEntity;
import gov.cms.dpc.fhir.converters.entities.PatientEntityConverter;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.UUID;

public class PatientResource extends AbstractPatientResource {

    private final PatientDAO dao;

    @Inject
    PatientResource(PatientDAO dao) {
        this.dao = dao;
    }

    @Override
    public Bundle searchPatients(String patientID, String organizationToken) {
        return null;
    }

    @Override
    public Response createPatient(Patient patient) {
        final PatientEntity entity = this.dao.persistPatient(PatientEntity.fromFHIR(patient));

        return Response.status(Response.Status.CREATED)
                .entity(PatientEntityConverter.convert(entity))
                .build();
    }

    @Override
    public Patient getPatient(UUID patientID) {
        return null;
    }

    @Override
    public Response deletePatient(UUID patientID) {
        return null;
    }

    @Override
    public Patient updatePatient(UUID patientID, Patient patient) {
        return null;
    }
}
