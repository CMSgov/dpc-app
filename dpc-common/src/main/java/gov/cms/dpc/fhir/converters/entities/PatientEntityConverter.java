package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.common.entities.PatientEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.converters.FHIRConverter;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import gov.cms.dpc.fhir.validations.profiles.PatientProfile;
import org.hl7.fhir.dstu3.model.*;

import java.util.UUID;

import static gov.cms.dpc.fhir.FHIRFormatters.INSTANT_FORMATTER;

public class PatientEntityConverter implements FHIRConverter<Patient, PatientEntity> {

    public PatientEntityConverter() {
        // Not used
    }

    @Override
    @SuppressWarnings("JdkObsolete") // Date class is used by FHIR stu3 Patient model
    public PatientEntity fromFHIR(FHIREntityConverter converter, Patient resource) {
        final PatientEntity patient = new PatientEntity();
        patient.setDob(PatientEntity.toLocalDate(resource.getBirthDate()));
        patient.setBeneficiaryID(FHIRExtractors.getPatientMBI(resource));
        final HumanName name = resource.getNameFirstRep();
        patient.setFirstName(name.getGivenAsSingleString());
        patient.setLastName(name.getFamily());
        patient.setGender(resource.getGender());

        // Set the managing organization
        final Reference managingOrganization = resource.getManagingOrganization();
        if (managingOrganization.getReference() != null) {
            final OrganizationEntity organizationEntity = new OrganizationEntity();
            organizationEntity.setId(FHIRExtractors.getEntityUUID(managingOrganization.getReference()));
            patient.setOrganization(organizationEntity);
        }

        // Set the ID, if one exists
        final String id = resource.getId();
        if (id != null) {
            patient.setID(UUID.fromString(new IdType(id).getIdPart()));
        }

        return patient;
    }

    @Override
    @SuppressWarnings("JdkObsolete") // Date class is used by FHIR stu3 Patient model
    public Patient toFHIR(FHIREntityConverter converter, PatientEntity entity) {
        final Patient patient = new Patient();

        // Add the patient metadata
        final Meta meta = new Meta();
        meta.addProfile(PatientProfile.PROFILE_URI);
        
        if (entity.getUpdatedAt() != null) {
            meta.setLastUpdatedElement(new InstantType(entity.getUpdatedAt().format(INSTANT_FORMATTER)));
        }
        
        patient.setMeta(meta);

        patient.setId(entity.getID().toString());
        patient.addName()
                .setFamily(entity.getLastName())
                .addGiven(entity.getFirstName());

        patient.setBirthDate(PatientEntity.fromLocalDate(entity.getDob()));
        patient.setGender(entity.getGender());

        patient
                .addIdentifier()
                .setSystem(DPCIdentifierSystem.MBI.getSystem())
                .setValue(entity.getBeneficiaryID());

        // Managing organization
        final Reference organization = new Reference(new IdType("Organization", entity.getOrganization().getId().toString()));
        patient.setManagingOrganization(organization);

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
