package gov.cms.dpc.fhir.converters;

import gov.cms.dpc.common.entities.PatientEntity;
import org.hl7.fhir.dstu3.model.Patient;

import java.util.UUID;

public class TestPatientEntityConverter implements FHIRConverter<Patient, PatientEntity> {

    public TestPatientEntityConverter() {
        // Not used
    }

    @Override
    public PatientEntity fromFHIR(Patient resource) {
        final PatientEntity patientEntity = new PatientEntity();
        patientEntity.setGender(resource.getGender());
        patientEntity.setPatientID(UUID.fromString(resource.getIdElement().getIdPart()));

        return patientEntity;
    }

    @Override
    public Patient toFHIR(PatientEntity javaClass) {
        final Patient patient = new Patient();
        patient.setId(javaClass.getPatientID().toString());
        patient.setGender(javaClass.getGender());

        return patient;
    }

    @Override
    public Class<Patient> getFHIRResource() {
        return Patient.class;
    }

    @Override
    public Class<PatientEntity> getJavaClass() {
        return PatientEntity.class;
    }
}
