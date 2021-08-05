package gov.cms.dpc.fhir;

import gov.cms.dpc.common.entities.PatientEntity;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for extracting various features from FHIR resources
 */
public class FHIRExtractors {

    private static final Logger logger = LoggerFactory.getLogger(FHIRExtractors.class);

    private static final Pattern idExtractor = Pattern.compile("/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})/?");
    static final String ENTITY_ID_ERROR = "Cannot extract string from '%s'";

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
        return findMatchingIdentifier(provider.getIdentifier(), DPCIdentifierSystem.NPPES).getValue();
    }

    /**
     * Extract the Medicare Beneficiary ID (MBI) from the given {@link org.hl7.fhir.dstu3.model.Patient} resource
     *
     * @param patient - {@link Patient} provider to get MBI from
     * @return - {@link String} patient MBI
     */
    public static String getPatientMBI(Patient patient) {
        String identifier = findMatchingIdentifier(patient.getIdentifier(), DPCIdentifierSystem.MBI).getValue();
        Pattern mbiPattern = Pattern.compile(PatientEntity.MBI_FORMAT);
        if (mbiPattern.matcher(identifier).matches()) {
            return identifier;
        }

        logger.error("Invalid MBI");
        throw new IllegalArgumentException("Patient Identifier for system " + DPCIdentifierSystem.MBI.getSystem() + " must match MBI format");
    }

    /**
     * Extract the CCW Beneficiary ID (bene_id) from the given {@link org.hl7.fhir.dstu3.model.Patient} resource
     *
     * @param patient - {@link Patient} provider to get bene ID from
     * @return - {@link String} patient bene ID
     */
    public static String getPatientBeneId(Patient patient) {
        return findMatchingIdentifier(patient.getIdentifier(), DPCIdentifierSystem.BENE_ID).getValue();
    }

    /**
     * Extracts the UUID from the ID of a given resource.
     * <p>
     * The ID could be in one of two forms: 1. {DPCResourceType}/{identifier}. 2. {identifier}
     * In the first case, we need to strip off the Resource Type and then encode to {@link UUID}.
     * In the second, we can try to encode directly.
     *
     * @param idString - {@link String} ID to parse
     * @return - {@link UUID} of resource
     */
    public static UUID getEntityUUID(String idString) {
        logger.trace("Extracting from Entity ID {}", idString);
        // Figure out if we need to split the Entity UUID, look for the '/' delimiter
        final int delimIndex = idString.indexOf('/');
        if (delimIndex < 0) {
            return UUID.fromString(idString);
        }

        final Matcher matcher = idExtractor.matcher(idString);
        if (!matcher.find() || !(matcher.groupCount() == 1)) {
            throw new IllegalArgumentException(String.format(ENTITY_ID_ERROR, idString));
        }

        try {
            final String id = Objects.requireNonNull(matcher.group(1));
            return UUID.fromString(id);
        } catch (Exception e) {
            logger.error("Cannot extract ID from entity", e);
            throw new IllegalArgumentException(String.format(ENTITY_ID_ERROR, idString));
        }
    }

    public static Provenance.ProvenanceAgentComponent getProvenancePerformer(Provenance provenance) {
        return provenance
                .getAgent()
                .stream()
                .filter(comp -> comp.getRoleFirstRep().getCodingFirstRep().getCode().equals("AGNT"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cannot find Provenance performer"));
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
    public static Pair<String, String> parseTag(String tag) {
        final int idx = tag.indexOf('|');
        if (idx < 0) {
            return Pair.of("", tag);
        }

        return Pair.of(tag.substring(0, idx), tag.substring(idx + 1));
    }

    public static Identifier findMatchingIdentifier(List<Identifier> identifiers, DPCIdentifierSystem system) {
        return identifiers
                .stream()
                .filter(id -> id.getSystem().equals(system.getSystem()))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Cannot find identifier for system: %s", system.getSystem())));
    }

    /**
     * Gets the Organization ID from the {@link ca.uhn.fhir.model.api.Tag} sections of the {@link IBaseResource}
     * This searches through the tags for one that has the {@link DPCIdentifierSystem#DPC} system and returns the first one.
     *
     * @param resource - {@link IBaseResource} to
     * @return - {@link String} Organization ID
     * @throws IllegalArgumentException - if there is not at least one Organization tag
     */
    public static String getOrganizationID(IBaseResource resource) {
        return resource.getMeta().getTag()
                .stream()
                .filter(tag -> tag.getSystem().equals(DPCIdentifierSystem.DPC.getSystem()))
                .map(IBaseCoding::getCode)
                .map(code -> new IdType(code).getIdPart())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Roster MUST have DPC organization tag"));
    }

    public static String getAttributedNPI(Group group) {
        final Group.GroupCharacteristicComponent component = group
                .getCharacteristic()
                .stream()
                .filter(concept -> concept.getCode().getCodingFirstRep().getCode().equals("attributed-to"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Must have 'attributed-to' concept"));

        if (component.getValue() == null) {
            throw new IllegalArgumentException("Roster MUST have attributed Provider");
        }

        return ((CodeableConcept) component.getValue()).getCoding()
                .stream()
                .filter(code -> code.getSystem().equals(DPCIdentifierSystem.NPPES.getSystem()))
                .map(Coding::getCode)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Roster MUST have attributed Provider"));
    }
}
