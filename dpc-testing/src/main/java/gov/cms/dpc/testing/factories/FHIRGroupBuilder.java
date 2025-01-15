package gov.cms.dpc.testing.factories;

import org.hl7.fhir.dstu3.model.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

//This is now the official DPC FHIR Group Generator. To be used by any service for test purposes.
public class FHIRGroupBuilder {

    //This constant is found in DPCIdentifierSystem; but I did not want to introduce a circular dependency.
    //TODO revisit during test refactoring
    static final String NPPES_SYSTEM =  "http://hl7.org/fhir/sid/us-npi";
    static final String DPC_SYSTEM = "https://dpc.cms.gov/organization_id#";

    private final Group theGroup;

    private FHIRGroupBuilder(){
        theGroup = new Group();
    }

    public Group build(){
        return theGroup;
    }

    public static FHIRGroupBuilder newBuild() {
        return new FHIRGroupBuilder();
    }

    public FHIRGroupBuilder attributedTo(String providerNPI){
        final CodeableConcept attributionConcept = new CodeableConcept();
        attributionConcept.addCoding().setCode("attributed-to");

        final CodeableConcept NPIConcept = new CodeableConcept();
        NPIConcept.addCoding().setSystem(NPPES_SYSTEM).setCode(providerNPI);
        theGroup.setType(Group.GroupType.PERSON);
        theGroup.setActive(true);
        theGroup.addCharacteristic()
                .setExclude(false)
                .setCode(attributionConcept)
                .setValue(NPIConcept);

        return this;
    }

    public FHIRGroupBuilder withPatients(IdType... idTypes){
        Arrays.stream(idTypes)
                .map(Reference::new)
                .map(Group.GroupMemberComponent::new)
                .forEach(theGroup::addMember);
        return this;
    }

    public FHIRGroupBuilder withPatients(String... patientIds){
        List<IdType> patientIdTypes = Arrays.stream(patientIds)
                .map(IdType::new)
                .map(type -> new IdType("Patient", type.getIdPart()))
                .collect(Collectors.toList());

        return withPatients(patientIdTypes.toArray(IdType[]::new));
    }

    public FHIRGroupBuilder withPatients(UUID... patientUUIDs){
        List<IdType> patientIdTypes = Arrays.stream(patientUUIDs)
                .map(uuid -> new IdType("Patient", uuid.toString()))
                .collect(Collectors.toList());

        return withPatients(patientIdTypes.toArray(IdType[]::new));
    }

    public FHIRGroupBuilder withOrgTag(UUID orgId){
        Meta meta = new Meta();
        meta.addTag(DPC_SYSTEM, orgId.toString(), "Organization ID");
        theGroup.setMeta(meta);
        return this;
    }

    public FHIRGroupBuilder withUUID(){
        theGroup.setId(UUID.randomUUID().toString());
        return this;
    }

}




