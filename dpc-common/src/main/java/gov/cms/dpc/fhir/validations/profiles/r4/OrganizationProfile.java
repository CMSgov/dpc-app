package gov.cms.dpc.fhir.validations.profiles.r4;

import gov.cms.dpc.fhir.validations.profiles.IProfileLoader;

public class OrganizationProfile implements IProfileLoader {

    public static final String PROFILE_URI = "https://dpc.cms.gov/api/v3/StructureDefinition/dpc-profile-organization";

    @Override
    public String getPath() {
        return "validations/v3/DPCOrganization.json";
    }

    @Override
    public String getURI() {
        return PROFILE_URI;
    }
}
