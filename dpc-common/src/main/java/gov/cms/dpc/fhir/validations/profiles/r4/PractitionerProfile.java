package gov.cms.dpc.fhir.validations.profiles.r4;

import gov.cms.dpc.fhir.validations.profiles.IProfileLoader;

/**
 * Custom {@link org.hl7.fhir.r4.model.Practitioner} profile which requires a small subset of data in order to fulfill our business functions.
 */
public class PractitionerProfile implements IProfileLoader {

    public static final String PROFILE_URI = "https://dpc.cms.gov/api/v3/StructureDefinition/dpc-profile-practitioner";

    @Override
    public String getPath() {
        return "validations/v3/DPCPractitioner.json";
    }

    @Override
    public String getURI() {
        return PROFILE_URI;
    }
}
