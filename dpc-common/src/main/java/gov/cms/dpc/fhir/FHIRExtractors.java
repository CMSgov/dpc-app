package gov.cms.dpc.fhir;

import org.hl7.fhir.dstu3.model.Group;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;

/**
 * Helper class for extracting various features from FHIR resources
 */
public class FHIRExtractors {

    private FHIRExtractors() {
        // Not used
    }

    /**
     * Extract the National Provider ID (NPI) from the given {@link Practitioner} resource
     * This currently assumes that the NPI is the first ID associated to the resource,
     * but eventually we'll need to do a more thorough check
     *
     * @param provider - {@link Practitioner} provider to get NPI from
     * @return - {@link String} provider NPI
     */
    public static String getProviderNPI(Practitioner provider) {
        // This should probably find the ID with the correct URI, instead of just pulling the first value
        return provider.getIdentifierFirstRep().getValue();
    }

    /**
     * Extract the National Provider ID (NPI) from the given {@link Group} resource
     * This should be the {@link Group#getIdentifierFirstRep()} for the given resource
     * This currently assumes that the NPI is the first ID associated to the resource,
     * but eventually we'll need to do a more thorough check
     *
     * @param group - {@link Group} to extract provider NPI from
     * @return - {@link String} provider NPI
     */
    public static String getProviderNPIFromGroup(Group group) {
        return group.getIdentifierFirstRep().getValue();
    }

    /**
     * Extract the Medicare Beneficiary ID (MPI) from the given {@link org.hl7.fhir.dstu3.model.Patient} resource
     * This currently assumes that the MPI is the first ID associated to the resource,
     * but eventually we'll need to do a more thorough check
     *
     * @param patient - {@link Patient} provider to get MPI from
     * @return - {@link String} patient MBI
     */
    public static String getPatientMPI(Patient patient) {
        // This should probably find the ID with the correct URI, instead of just pulling the first value
        return patient.getIdentifierFirstRep().getValue();
    }

    /**
     * Extract the Medicare Beneficiary ID (MPI) from the given {@link org.hl7.fhir.dstu3.model.Group.GroupMemberComponent}, which is part of a larger {@link Group} resource.
     * This currently assumes that the {@link Group.GroupMemberComponent#getIdentifierFirstRep()} is the MPI of the associated patient
     * but eventually we'll need to do a more thorough check
     *
     * @param groupMember - {@link org.hl7.fhir.dstu3.model.Group.GroupMemberComponent} group member to get ID from
     * @return - {@link} patient MBI
     */
    public static String getPatientMPIFromGroup(Group.GroupMemberComponent groupMember) {
        return groupMember.getEntity().getIdentifier().getValue();
    }


}
