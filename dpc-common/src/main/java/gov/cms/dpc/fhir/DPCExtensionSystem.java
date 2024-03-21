package gov.cms.dpc.fhir;

/**
 * List of extension systems currently supported by DPC.
 */
public enum DPCExtensionSystem {
    IDENTIFIER_CURRENCY("https://bluebutton.cms.gov/resources/codesystem/identifier-currency");

    private final String system;

    DPCExtensionSystem(String system) { this.system = system; }
    public String getSystem() { return system; }

    public static final String CURRENT = "current";
    public static final String HISTORIC = "historic";
}
