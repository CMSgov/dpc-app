package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.AttributionRelationship;
import gov.cms.dpc.common.entities.RosterEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.converters.FHIRConverter;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import org.hl7.fhir.dstu3.model.*;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class RosterEntityConverter implements FHIRConverter<Group, RosterEntity> {

    public RosterEntityConverter() {
        // Not used
    }

    @Override
    public RosterEntity fromFHIR(FHIREntityConverter converter, Group resource) {
        throw new UnsupportedOperationException("Entity cannot be converted from FHIR, using this class");
    }

    @Override
    public Group toFHIR(FHIREntityConverter converter, RosterEntity entity) {
        final Group group = new Group();

        group.setType(Group.GroupType.PERSON);
        group.setActual(true);
        group.setId(entity.getId().toString());
        if(entity.getManagingOrganization() != null
                && entity.getManagingOrganization().getOrganizationID() != null)
            group.getMeta().addTag(DPCIdentifierSystem.DPC.getSystem(), entity.getManagingOrganization().getOrganizationID().getValue(), "Organization ID");

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

    @Override
    public Class<Group> getFHIRResource() {
        return Group.class;
    }

    @Override
    public Class<RosterEntity> getJavaClass() {
        return RosterEntity.class;
    }

    @SuppressWarnings("JdkObsolete") // Date class is used by FHIR stu3 Period model
    private static Group.GroupMemberComponent buildComponent(AttributionRelationship relationship) {
        final IdType id = new IdType("Patient", relationship.getPatient().getID().toString());
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
