package gov.cms.dpc.fhir.validations.profiles;

/**
 * Custom {@link org.hl7.fhir.dstu3.model.Practitioner} profile which requires a small subset of data in order to fulfill our business functions.
 */
public class PractitionerProfile implements IProfileLoader {

    public static String PROFILE_URI = "https://dpc.cms.gov/fhir/StructureDefinition/dpc-profile-practitioner";

    @Override
    public String getPath() {
        return "validations/DPCPractitioner.json";
    }

    @Override
    public String getURI() {
        return PROFILE_URI;
    }
}
