package gov.cms.dpc.fhir.validations.profiles.stu3;

import gov.cms.dpc.fhir.validations.profiles.IProfileLoader;

/**
 * Custom patient profile that requires specific data points in order to support patient matching.
 */
public class PatientProfile implements IProfileLoader {

    public static final String PROFILE_URI = "https://dpc.cms.gov/api/v1/StructureDefinition/dpc-profile-patient";

    @Override
    public String getPath() {
        return "validations/v1/DPCPatient.json";
    }

    @Override
    public String getURI() {
        return PROFILE_URI;
    }
}
