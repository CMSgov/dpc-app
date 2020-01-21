package gov.cms.dpc.fhir.validations.profiles;

public class AttestationProfile implements IProfileLoader {

    public static final String PROFILE_URI = "https://dpc.cms.gov/api/v1/StructureDefinition/dpc-profile-attestation";

    @Override
    public String getPath() {
        return "validations/DPCAttestation.json";
    }

    @Override
    public String getURI() {
        return PROFILE_URI;
    }
}
