package gov.cms.dpc.fhir.validations.profiles;

/**
 * Custom patient profile that requires specific data points in order to support patient matching.
 */
public class PatientProfile implements IProfileLoader {

    public static String PROFILE_URI = "https://dpc.cms.gov/fhir/StructureDefinitions/dpc-patient";

    @Override
    public String getPath() {
        return "validations/DPCPatient.json";
    }

    @Override
    public String getURI() {
        return PROFILE_URI;
    }
}
