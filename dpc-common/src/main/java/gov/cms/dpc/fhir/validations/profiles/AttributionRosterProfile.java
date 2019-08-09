package gov.cms.dpc.fhir.validations.profiles;

public class AttributionRosterProfile implements IProfileLoader {

    public static final String PROFILE_URI = "https://dpc.cms.gov/api/v1/StructureDefinition/dpc-profile-attribution-roster";

    @Override
    public String getPath() {
        return "validations/DPCAttributionRoster.json";
    }

    @Override
    public String getURI() {
        return PROFILE_URI;
    }
}
