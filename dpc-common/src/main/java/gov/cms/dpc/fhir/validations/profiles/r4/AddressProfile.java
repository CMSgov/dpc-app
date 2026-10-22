package gov.cms.dpc.fhir.validations.profiles.r4;

import gov.cms.dpc.fhir.validations.profiles.IProfileLoader;

public class AddressProfile implements IProfileLoader {

    public static String PROFILE_URI = "https://dpc.cms.gov/api/v3/StructureDefinition/dpc-profile-address";

    @Override
    public String getPath() {
        return "validations/v3/DPCAddress.json";
    }

    @Override
    public String getURI() {
        return PROFILE_URI;
    }
}
