package gov.cms.dpc.fhir;

import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;

import java.util.List;
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
        return findMatchingIdentifier(patient.getIdentifier(), DPCIdentifierSystem.MBI).getValue();
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

    /**
     * Extracts an {@link Identifier} from a FHIR search parameter.
     *
     * @param queryParam - {@link String} query parameter with the input format: [system]|[value].
     * @return - {@link Identifier}
     */
    public static Identifier parseIDFromQueryParam(String queryParam) {

        final Pair<String, String> stringPair = parseTag(queryParam);
        final Identifier identifier = new Identifier();
        // Strip off any trailing '\' characters.
        // These might come in when parsing an ID that was generated with HAPI using the systemAndCode method
        String system = stringPair.getLeft();
        if (system.endsWith("\\")) {
            system = system.substring(0, system.length() - 1);
        }
        identifier.setSystem(system);
        identifier.setValue(stringPair.getRight());
        return identifier;
    }

    /**
     * Parses a given FHIR tag into a System/Code pair
     * Splits the tag based on the '|' character.
     *
     * @param tag - {@link String} tag to parse
     * @return - {@link Pair} of System {@link String} and Code {@link String}
     */
    private static Pair<String, String> parseTag(String tag) {
        final int idx = tag.indexOf('|');
        if (idx <= 0) {
            throw new IllegalArgumentException(String.format("Malformed tag: %s", tag));
        }

        return Pair.of(tag.substring(0, idx), tag.substring(idx + 1));
    }

    private static Identifier findMatchingIdentifier(List<Identifier> identifiers, DPCIdentifierSystem system) {
        return identifiers
                .stream()
                .filter(id -> id.getSystem().equals(DPCIdentifierSystem.MBI.getSystem()))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Cannot find identifier for system: %s", system.getSystem())));
    }
}
