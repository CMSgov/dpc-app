package gov.cms.dpc.fhir;

import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for extracting various features from FHIR resources
 */
public class FHIRExtractors {

    private static final Pattern idExtractor = Pattern.compile("/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})/?");

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
     * Extracts the UUID from the ID of a given resource.
     *
     * @param idString - {@link String} ID to parse
     * @return - {@link UUID} of resource
     */
    public static UUID getEntityUUID(String idString) {
        final Matcher matcher = idExtractor.matcher(idString);
        if (!matcher.find() && !(matcher.groupCount() == 1)) {
            throw new IllegalArgumentException(String.format("Cannot extract string from '%s'", idString));
        }

        final String id = Objects.requireNonNull(matcher.group(1));

        return UUID.fromString(id);
    }
}
