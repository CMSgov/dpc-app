package gov.cms.dpc.fhir.validations.profiles.stu3;

import gov.cms.dpc.fhir.validations.profiles.IProfileLoader;

public class AddressProfile implements IProfileLoader {

    public static String PROFILE_URI = "https://dpc.cms.gov/api/v1/StructureDefinition/dpc-profile-address";

    @Override
    public String getPath() {
        return "validations/v1/DPCAddress.json";
    }

    @Override
    public String getURI() {
        return PROFILE_URI;
    }
}
