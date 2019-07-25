package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.PatientEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.validations.profiles.PatientProfile;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;

public class PatientEntityConverter {

    private PatientEntityConverter() {
        // Not used
    }

    public static Patient convert(PatientEntity entity) {
        final Patient patient = new Patient();

        // Add the patient metadata
        final Meta meta = new Meta();
        meta.addProfile(PatientProfile.PROFILE_URI);
        patient.setMeta(meta);

        patient.setId(entity.getPatientID().toString());
        patient.addName()
                .setFamily(entity.getPatientLastName())
                .addGiven(entity.getPatientFirstName());

        patient.setBirthDate(PatientEntity.fromLocalDate(entity.getDob()));

        patient
                .addIdentifier()
                .setSystem(DPCIdentifierSystem.MBI.getSystem())
                .setValue(entity.getBeneficiaryID());

        // Managing organization
        final Reference organization = new Reference(new IdType("Organization", entity.getOrganization().getId().toString()));
        patient.setManagingOrganization(organization);

        return patient;
    }
}
