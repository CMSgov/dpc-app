package gov.cms.dpc.fhir;

/**
 * List of identifier authorities supported by DPC.
 * These can be used to identify providers, organization and patients.
 */
public enum DPCIdentifierSystem {
    // I think I'm making this up.
    // Need to see if we actually have a PECOS ID URI.
    PECOS("http://bluebutton.cms.hhs.gov/identifier#pecos"),
    NPPES("http://hl7.org/fhir/sid/us-npi"),
    BENE_ID("https://bluebutton.cms.gov/resources/variables/bene_id"),
    MBI("http://hl7.org/fhir/sid/us-mbi"),
    MBI_HASH("https://bluebutton.cms.gov/resources/identifier/mbi-hash"),
    DPC("https://dpc.cms.gov/organization_id"),
    HICN("https://bluebutton.cms.gov/resources/identifier/hicn-hash"),
    UNKNOWN ("");

    private final String system;

    DPCIdentifierSystem(String system) {
        this.system = system;
    }

    public String getSystem() {
        return system;
    }

    /**
     * Enum from string representation of System value.
     * @param system - {@link String} representation of Identifier system
     * @return - {@link DPCIdentifierSystem} matching input string
     * @throws IllegalArgumentException if input cannot be matched against known systems
     */
    public static DPCIdentifierSystem fromString(String system) {
        for(final DPCIdentifierSystem id : DPCIdentifierSystem.values()) {
            if (id.system.equalsIgnoreCase(system)) {
                return id;
            }
        }
        throw new IllegalArgumentException(String.format("Cannot find Identifier system for \"%s\"", system));
    }
}
