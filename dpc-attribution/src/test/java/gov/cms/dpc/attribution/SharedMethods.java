package gov.cms.dpc.attribution;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.apache.http.HttpEntity;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class SharedMethods {

    @SuppressWarnings("unchecked")
    public static <T> List<T> UnmarshallResponse(HttpEntity entity) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();

        return (List<T>) mapper.readValue(entity.getContent(), List.class);
    }

    public static Bundle createAttributionBundle(String providerID, String patientID) {
        final Bundle bundle = new Bundle();
        bundle.setId(new IdType("Roster", "bundle-update"));
        bundle.setType(Bundle.BundleType.COLLECTION);

        // Create the provider with the necessary fields
        final Practitioner practitioner = new Practitioner();
        practitioner.addIdentifier().setValue(providerID).setSystem(DPCIdentifierSystem.MBI.getSystem());
        practitioner.addName().addGiven("Test").setFamily("Provider");
        bundle.addEntry().setResource(practitioner).setFullUrl("http://something.gov/" + practitioner.getIdentifierFirstRep().getValue());

        // Add some random values to the patient
        final Patient patient = new Patient();
        patient.addIdentifier().setValue(patientID).setSystem(DPCIdentifierSystem.MBI.getSystem());
        patient.addName().addGiven("New Test Patient");
        patient.setBirthDate(new GregorianCalendar(2019, Calendar.MARCH, 1).getTime());
        final Bundle.BundleEntryComponent component = new Bundle.BundleEntryComponent();
        component.setResource(patient);
        component.setFullUrl("http://something.gov/" + patient.getIdentifierFirstRep().getValue());
        bundle.addEntry(component);

        return bundle;
    }
}
