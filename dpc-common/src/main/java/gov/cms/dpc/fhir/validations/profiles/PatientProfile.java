package gov.cms.dpc.fhir.validations.profiles;

public class PatientProfile implements IProfileLoader {

    public static String PROFILE_STRING = "https://dpc.cms.gov/fhir/StructureDefinitions/dpc-patient";

    @Override
    public String getPath() {
        return "validations/DPCPatient.json";
    }

    @Override
    public String getURI() {
        return PROFILE_STRING;
    }
}
