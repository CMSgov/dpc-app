package gov.cms.dpc.fhir.validations.profiles;

public class EndpointProfile implements IProfileLoader {
    public static String PROFILE_URI = "https://dpc.cms.gov/api/v1/StructureDefinition/dpc-profile-endpoint";

    @Override
    public String getPath() {
        return "validations/DPCEndpoint.json";
    }

    @Override
    public String getURI() {
        return PROFILE_URI;
    }
}
