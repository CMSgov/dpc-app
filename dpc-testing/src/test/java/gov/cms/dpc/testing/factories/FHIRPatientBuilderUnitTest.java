package gov.cms.dpc.testing.factories;

import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FHIRPatientBuilderUnitTest {

    @Test
    public void testBuildWithMultiple() {
        String gender =  "other";
        UUID orgId =  UUID.randomUUID();

        Patient patient = FHIRPatientBuilder.newBuild()
                .withGender(gender)
                .managedBy(orgId)
                .build();
        assertEquals(Enumerations.AdministrativeGender.OTHER, patient.getGender());
        assertEquals("Organization/"+orgId,patient.getManagingOrganization().getReference());
    }

    @Test
    public void withId() {
        UUID id = UUID.randomUUID();
        Patient patient = FHIRPatientBuilder.newBuild().withId(id).build();
        assertEquals(id.toString(),patient.getId());

        patient = FHIRPatientBuilder.newBuild().withId(id.toString()).build();
        assertEquals(id.toString(),patient.getId());

    }

    @Test
    public void withMbi() {
        String mbi =  "4S41C00AA00";
        Patient patient = FHIRPatientBuilder.newBuild().withMbi(mbi).build();
        assertEquals(1,patient.getIdentifier().size());
        assertEquals(mbi,patient.getIdentifier().get(0).getValue());
        assertEquals(FHIRPatientBuilder.MBI_SYSTEM,patient.getIdentifier().get(0).getSystem());
    }

    @Test
    public void testWithName() {
        String first =  "Salvadorito";
        String last = "Burger";
        Patient patient = FHIRPatientBuilder.newBuild().withName(first,last).build();
        assertEquals(1,patient.getName().size());
        assertEquals(first,patient.getName().get(0).getGivenAsSingleString());
        assertEquals(last,patient.getName().get(0).getFamily());
    }

    @Test
    public void testWithBirthDate() {
        String dob =  "1990-10-10";
        Patient patient = FHIRPatientBuilder.newBuild().withBirthDate(dob).build();
        assertEquals(Date.valueOf(dob), patient.getBirthDate());

        patient = FHIRPatientBuilder.newBuild().withBirthDate(Date.valueOf(dob)).build();
        assertEquals(Date.valueOf(dob), patient.getBirthDate());
    }


    @Test
    public void testWithGender() {
        String gender =  "other";
        Patient patient = FHIRPatientBuilder.newBuild().withGender(gender).build();
        assertEquals(Enumerations.AdministrativeGender.OTHER, patient.getGender());

        patient = FHIRPatientBuilder.newBuild().withGender(Enumerations.AdministrativeGender.OTHER).build();
        assertEquals(Enumerations.AdministrativeGender.OTHER, patient.getGender());
    }

    @Test
    public void testManagedBy() {
        UUID orgId =  UUID.randomUUID();
        Patient patient = FHIRPatientBuilder.newBuild().managedBy(orgId).build();
        assertEquals("Organization/"+orgId,patient.getManagingOrganization().getReference());

        patient = FHIRPatientBuilder.newBuild().managedBy(orgId.toString()).build();
        assertEquals("Organization/"+orgId,patient.getManagingOrganization().getReference());

        patient = FHIRPatientBuilder.newBuild().managedBy("Organization/"+ orgId).build();
        assertEquals("Organization/"+orgId,patient.getManagingOrganization().getReference());

        patient = FHIRPatientBuilder.newBuild().managedBy(new IdType("Organization", orgId.toString())).build();
        assertEquals("Organization/"+orgId,patient.getManagingOrganization().getReference());
    }
}
