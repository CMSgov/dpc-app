package gov.cms.dpc.fhir.validations.profiles.r4;

import gov.cms.dpc.fhir.validations.profiles.IProfileLoader;

public class AttributionRosterProfile implements IProfileLoader {

    public static final String PROFILE_URI = "https://dpc.cms.gov/api/v3/StructureDefinition/dpc-profile-attribution-roster";

    @Override
    public String getPath() {
        return "validations/v3/DPCAttributionRoster.json";
    }

    @Override
    public String getURI() {
        return PROFILE_URI;
    }
}
