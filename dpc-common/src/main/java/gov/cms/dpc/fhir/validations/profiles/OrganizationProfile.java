package gov.cms.dpc.fhir.validations.profiles;

public class OrganizationProfile implements IProfileLoader {

    public static final String PROFILE_URI = "https://dpc.cms.gov/api/v1/StructureDefinition/dpc-profile-organization";

    @Override
    public String getPath() {
        return "validations/DPCOrganization.json";
    }

    @Override
    public String getURI() {
        return PROFILE_URI;
    }
}
