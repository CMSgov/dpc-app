package gov.cms.dpc.fhir.validations.profiles;

public class AddressProfile implements IProfileLoader {

    public static String PROFILE_URI = "https://dpc.cms.gov/fhir/v1/StructureDefinition/dpc-profile-address";

    @Override
    public String getPath() {
        return "validations/DPCAddress.json";
    }

    @Override
    public String getURI() {
        return PROFILE_URI;
    }
}
