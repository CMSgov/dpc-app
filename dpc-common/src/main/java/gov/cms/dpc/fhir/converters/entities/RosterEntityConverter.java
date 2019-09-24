package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.AttributionRelationship;
import gov.cms.dpc.common.entities.RosterEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.hl7.fhir.dstu3.model.*;

import java.util.Date;
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
                .map(RosterEntityConverter::buildComponent)
                .collect(Collectors.toList());

        group.setMember(patients);

        return group;
    }

    private static Group.GroupMemberComponent buildComponent(AttributionRelationship relationship) {
        final IdType id = new IdType("Patient", relationship.getPatient().getPatientID().toString());
        final Reference reference = new Reference(id);
        final Group.GroupMemberComponent component = new Group.GroupMemberComponent();
        component.setInactive(relationship.isInactive());
        component.setEntity(reference);

        // Set the period begin, end
        final Period period = new Period();
        period.setStart(Date.from(relationship.getPeriodBegin().toInstant()));
        period.setEnd(Date.from(relationship.getPeriodEnd().toInstant()));
        component.setPeriod(period);
        return component;
    }
}
