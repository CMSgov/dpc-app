package gov.cms.dpc.testing.factories;

import org.hl7.fhir.dstu3.model.*;

public class FHIRPractitionerBuilder {

    //This constant is found in DPCIdentifierSystem; but I did not want to introduce a circular dependency.
    //TODO revisit during test refactoring
    final static String DPC_SYSTEM = "https://dpc.cms.gov/organization_id";
    final static String NPPES_SYSTEM =  "http://hl7.org/fhir/sid/us-npi";


    private final  Practitioner thePractitioner;

    private FHIRPractitionerBuilder (){
        thePractitioner = new Practitioner();
    }

    public static FHIRPractitionerBuilder newBuilder(){
        return new FHIRPractitionerBuilder();
    }

    public Practitioner build(){
        return thePractitioner;
    }

    public FHIRPractitionerBuilder withNpi(String npi){
        thePractitioner.addIdentifier().setValue(npi).setSystem(NPPES_SYSTEM);
        return this;
    }

    public FHIRPractitionerBuilder withName(String first, String last){
        thePractitioner.addName().setFamily(last).addGiven(first);
        return this;
    }
    public FHIRPractitionerBuilder withOrgTag(String orgID){
        thePractitioner.getMeta().addTag(DPC_SYSTEM, orgID, "OrganizationID");
        return this;
    }
}
