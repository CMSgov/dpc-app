package gov.cms.dpc.aggregation.dao;

import gov.cms.dpc.common.entities.*;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import gov.cms.dpc.fhir.FHIRExtractors;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.query.Query;

import javax.inject.Inject;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RosterDAO extends AbstractDAO<RosterEntity> {

    @Inject
    public RosterDAO(DPCManagedSessionFactory factory) {
        super(factory.getSessionFactory());
    }

    public boolean withinRoster(UUID organizationID, String providerID, String patientReference) {

        // Build a selection query to get records from the database
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<Boolean> query = builder.createQuery(Boolean.class);
        final Root<RosterEntity> root = query.from(RosterEntity.class);
        query.select(builder.literal(true));


        List<Predicate> predicates = new ArrayList<>();
        // Always restrict by Organization
        predicates.add(builder
                .equal(root.join(RosterEntity_.MANAGING_ORGANIZATION)
                                .get(OrganizationEntity_.ID),
                        organizationID));

        final UUID providerUUID = FHIRExtractors.getEntityUUID(providerID);
        predicates.add(builder.equal(root.get(RosterEntity_.ATTRIBUTED_PROVIDER).get(ProviderEntity_.ID), providerUUID));

        final UUID patientID = FHIRExtractors.getEntityUUID(patientReference);
        final Join<RosterEntity, AttributionRelationship> attrJoin = root.join(RosterEntity_.ATTRIBUTIONS);
        final Join<AttributionRelationship, PatientEntity> patientJoin = attrJoin.join(AttributionRelationship_.PATIENT);
        predicates.add(builder.equal(patientJoin.get(PatientEntity_.id), patientID));

        query.where(predicates.toArray(new Predicate[0]));
        final Query<Boolean> booleanQuery = this.currentSession().createQuery(query);

        return booleanQuery.getSingleResult();
    }

}