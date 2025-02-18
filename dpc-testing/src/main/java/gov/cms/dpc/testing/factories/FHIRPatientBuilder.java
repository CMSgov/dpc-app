package gov.cms.dpc.testing.factories;

import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;

import java.sql.Date;
import java.util.UUID;

public class FHIRPatientBuilder {

    //This constant is found in DPCIdentifierSystem; but I did not want to introduce a circular dependency.
    //TODO revisit during test refactoring
    static final String MBI_SYSTEM = "http://hl7.org/fhir/sid/us-mbi";

    private final Patient thePatient;

    private FHIRPatientBuilder(){
        thePatient = new Patient();
    }

    public static FHIRPatientBuilder newBuild(){
        return new FHIRPatientBuilder();
    }

    public Patient build(){
        return thePatient;
    }

    public FHIRPatientBuilder withId(String id){
        thePatient.setId(id);
        return this;
    }

    public FHIRPatientBuilder withId(UUID id){
        return withId(id.toString());
    }

    public FHIRPatientBuilder withMbi(String mbi){
        thePatient.addIdentifier()
                .setSystem(MBI_SYSTEM)
                .setValue(mbi);
        return this;
    }

    public FHIRPatientBuilder withName(String first, String last){
        thePatient.addName().setFamily(last).addGiven(first);
        return this;
    }

    public FHIRPatientBuilder withBirthDate(Date happyBDay){
        thePatient.setBirthDate(happyBDay);
        return this;
    }

    public FHIRPatientBuilder withBirthDate(String happyBDay){
        return withBirthDate(Date.valueOf(happyBDay));
    }

    public FHIRPatientBuilder withGender(Enumerations.AdministrativeGender gender){
        thePatient.setGender(gender);
        return this;
    }

    public FHIRPatientBuilder withGender(String gender){
        return withGender(Enumerations.AdministrativeGender.fromCode(gender));
    }

    public FHIRPatientBuilder managedBy(IdType orgId){
        thePatient.setManagingOrganization(new Reference(orgId));
        return this;
    }

    public FHIRPatientBuilder managedBy(String orgId){
        return managedBy(new IdType("Organization", new IdType(orgId).getIdPart()));
    }

    public FHIRPatientBuilder managedBy(UUID orgId){
        return managedBy(new IdType("Organization", orgId.toString()));
    }

    //add more test data if needed, this is just a start.
    public FHIRPatientBuilder withTestData(){
        withName("Salvadorito", "Nacho");
        withGender("other");
        withBirthDate("1991-12-01");
        return this;
    }

}
