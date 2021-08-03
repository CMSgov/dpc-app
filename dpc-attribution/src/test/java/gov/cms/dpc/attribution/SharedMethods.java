package gov.cms.dpc.attribution;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.FHIRExtractors;
import org.hl7.fhir.dstu3.model.*;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static gov.cms.dpc.common.utils.SeedProcessor.createBaseAttributionGroup;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SharedMethods {

    public static Bundle createAttributionBundle(String providerID, String patientID, String organizationID) {
        final Bundle bundle = new Bundle();
        bundle.setId(new IdType("Roster", "bundle-update"));
        bundle.setType(Bundle.BundleType.COLLECTION);

        // Create the provider with the necessary fields
        final Practitioner practitioner = new Practitioner();
        practitioner.addIdentifier().setValue(providerID).setSystem(DPCIdentifierSystem.NPPES.getSystem());
        practitioner.addName().addGiven("Test").setFamily("Provider");

        final Meta meta = new Meta();
        meta.addTag(DPCIdentifierSystem.DPC.getSystem(), organizationID, "Organization ID");
        practitioner.setMeta(meta);
        bundle.addEntry().setResource(practitioner).setFullUrl("http://something.gov/" + practitioner.getIdentifierFirstRep().getValue());

        // Add some random values to the patient
        final Patient patient = new Patient();
        final Identifier patientIdentifier = new Identifier();
        patientIdentifier.setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue(patientID);
        patient.addIdentifier(patientIdentifier);
        patient.addName().addGiven("New Test Patient");
        patient.setBirthDate(new GregorianCalendar(2019, Calendar.MARCH, 1).getTime());
        patient.setGender(Enumerations.AdministrativeGender.OTHER);
        patient.setManagingOrganization(new Reference("Organization/" + organizationID));
        final Bundle.BundleEntryComponent component = new Bundle.BundleEntryComponent();
        component.setResource(patient);
        component.setFullUrl("http://something.gov/" + patient.getIdentifierFirstRep().getValue());
        bundle.addEntry(component);

        return bundle;
    }

    public static Group submitAttributionBundle(IGenericClient client, Bundle bundle) {
        // Provider first, then patients
        final Practitioner practitioner = (Practitioner) bundle.getEntryFirstRep().getResource();
        final String providerID = practitioner.getIdentifierFirstRep().getValue();
        final String organizationID = FHIRExtractors.getOrganizationID(practitioner);

        final MethodOutcome createdPractitioner = client
                .create()
                .resource(practitioner)
                .encodedJson()
                .execute();

        assertTrue(createdPractitioner.getCreated(), "Should have created the practitioner");


        // Create a group and add Patients to it
        final Group rosterGroup = createBaseAttributionGroup(providerID, organizationID);

        bundle
                .getEntry()
                .stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource.getResourceType().getPath().equals(DPCResourceType.Patient.getPath()))
                .map(resource -> (Patient) resource)
                .forEach(patient -> {
                    final MethodOutcome created = client
                            .create()
                            .resource(patient)
                            .encodedJson()
                            .execute();
                    final Patient pr = (Patient) created.getResource();
                    // Add to group
                    rosterGroup
                            .addMember()
                            .setEntity(new Reference(pr.getIdElement()))
                            .setInactive(false);
                });

        final ICreateTyped groupCreation = client
                .create()
                .resource(rosterGroup)
                .encodedJson();

        final MethodOutcome groupCreated = groupCreation.execute();

        assertTrue(groupCreated.getCreated(), "Should have created the group");

        return (Group) groupCreated.getResource();
    }
}
