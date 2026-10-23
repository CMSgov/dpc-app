package gov.cms.dpc.fhir.validations.profiles.stu3;

import gov.cms.dpc.fhir.validations.profiles.IProfileLoader;

public class AttestationProfile implements IProfileLoader {

    public static final String PROFILE_URI = "https://dpc.cms.gov/api/v1/StructureDefinition/dpc-profile-attestation";

    @Override
    public String getPath() {
        return "validations/v1/DPCAttestation.json";
    }

    @Override
    public String getURI() {
        return PROFILE_URI;
    }
}
