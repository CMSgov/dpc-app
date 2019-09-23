package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.RosterEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Group;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Reference;

import java.util.List;
import java.util.stream.Collectors;

public class RosterEntityConverter {

    private RosterEntityConverter() {
        // Not used
    }

    public static Group convert(RosterEntity entity) {
        final Group group = new Group();
        group.setType(Group.GroupType.PERSON);
        group.setActual(true);
        group.setId(entity.getId().toString());

        final CodeableConcept attributedConcept = new CodeableConcept();
        attributedConcept.addCoding().setCode("attributed-to");

        final CodeableConcept providerConcept = new CodeableConcept();
        providerConcept.addCoding().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setCode(entity.getAttributedProvider().getProviderNPI());
        group.addCharacteristic()
                .setCode(attributedConcept)
                .setValue(providerConcept)
                .setExclude(false);

        final List<Group.GroupMemberComponent> patients = entity
                .getAttributions()
                .stream()
                .map(relationship -> {
                    final IdType id = new IdType("Patient", relationship.getPatient().getPatientID().toString());
                    final Reference reference = new Reference(id);
                    final Group.GroupMemberComponent component = new Group.GroupMemberComponent();
                    component.setInactive(relationship.isInactive());
                    return component;
                })
                .collect(Collectors.toList());

        group.setMember(patients);

        return group;
    }
}
