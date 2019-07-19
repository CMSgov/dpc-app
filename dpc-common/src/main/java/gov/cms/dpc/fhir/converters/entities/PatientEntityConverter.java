package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.PatientEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.hl7.fhir.dstu3.model.Patient;

public class PatientEntityConverter {

    private PatientEntityConverter() {
        // Not used
    }

    public static Patient convert(PatientEntity entity) {
        final Patient patient = new Patient();

        patient.setId(entity.getPatientID().toString());
        patient.addName()
                .setFamily(entity.getPatientLastName())
                .addGiven(entity.getPatientFirstName());

        patient.setBirthDate(PatientEntity.fromLocalDate(entity.getDob()));

        patient
                .addIdentifier()
                .setSystem(DPCIdentifierSystem.MBI.getSystem())
                .setValue(entity.getBeneficiaryID());

        return patient;
    }
}
