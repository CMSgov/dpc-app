package gov.cms.dpc.fhir;

import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.instance.model.api.IBaseMetaType;
import org.hl7.fhir.instance.model.api.IBaseResource;

public class FHIRBuilders {

    private FHIRBuilders() {
        // Not used
    }

    /**
     * Generates a {@link Practitioner} resource which contains an associated National Provider ID (NPI)
     *
     * @param npi - {@link} NPI of provider to use
     * @return - {@link Practitioner} with associated NPI as an Identifier
     */
    public static Practitioner buildPractitionerFromNPI(String npi) {
        final Practitioner practitioner = new Practitioner();
        practitioner.addIdentifier().setValue(npi).setSystem(DPCIdentifierSystem.NPPES.getSystem());

        return practitioner;
    }

    /**
     * Generates a {@link Patient} resource which contains an associated Medicare Beneficiary ID (MBI)
     *
     * @param MPI - {@link} MPI of provider to use
     * @return - {@link Patient} with associated MPI as an Identifier
     */
    public static Patient buildPatientFromMBI(String MPI) {
        final Patient patient = new Patient();
        patient.addIdentifier().setValue(MPI).setSystem(DPCIdentifierSystem.MBI.getSystem());

        return patient;
    }
}
