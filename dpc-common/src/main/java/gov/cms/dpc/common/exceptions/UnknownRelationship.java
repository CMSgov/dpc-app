package gov.cms.dpc.common.exceptions;

import gov.cms.dpc.fhir.FHIRExtractors;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;

/**
 * DPC Exception which indicates that an attribution relationship does not exist between the given provider and patient.
 */
public class UnknownRelationship extends RuntimeException {

    public static final long serialVersionUID = 42L;

    private final String providerNPI;
    private final String patientMPI;

    /**
     * Constructs an {@link UnknownRelationship} exception using the NPI/MPIs directly
     *
     * @param provider - {@link String} provider NPI
     * @param patient  - {@link String} patient NPI
     */
    public UnknownRelationship(String provider, String patient) {
            super(String.format("Unknown attribution relationship between %s and %s.", provider, patient));
        this.providerNPI = provider;
        this.patientMPI = patient;
    }

    public String getProviderNPI() {
        return providerNPI;
    }

    public String getPatientMPI() {
        return patientMPI;
    }
}
