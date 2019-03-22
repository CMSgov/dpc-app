package gov.cms.dpc.fhir;

import org.hl7.fhir.dstu3.model.Practitioner;

/**
 * Helper class for extracting various features from FHIR resources
 */
public class FHIRExtractors {

    /**
     * Extract the National Provider ID from the given {@link Practitioner} resource
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
}
