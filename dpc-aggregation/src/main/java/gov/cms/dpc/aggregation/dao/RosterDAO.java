package gov.cms.dpc.aggregation.dao;

import gov.cms.dpc.common.entities.*;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
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

    public UUID retrieveProviderIDFromRoster(UUID organizationID, UUID ambiguousID, String patientMBI) {
        // Build a selection query to get records from the database
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<UUID> query = builder.createQuery(UUID.class);
        final Root<RosterEntity> root = query.from(RosterEntity.class);

        query.select(root.get(RosterEntity_.ATTRIBUTED_PROVIDER).get(ProviderEntity_.ID));
        List<Predicate> predicates = new ArrayList<>();
        // Restrict by Organization via ID
        predicates.add(organizationPredicate(builder, root, organizationID));
        // Restrict by Provider or Roster via ID
        predicates.add(ambiguousIDPredicate(builder, root, ambiguousID));
        // Restrict by Patient via MBI
        predicates.add(mbiPredicate(builder, root, patientMBI));

        query.where(predicates.toArray(new Predicate[0]));


        Query<UUID> q = currentSession().createQuery(query);
        return q.getSingleResult();
    }

    private Predicate organizationPredicate(CriteriaBuilder builder, Root<RosterEntity> root, UUID organizationID) {
        // Always restrict by Organization
        return builder.equal(root.join(RosterEntity_.MANAGING_ORGANIZATION).get(OrganizationEntity_.ID), organizationID);
    }

    private Predicate ambiguousIDPredicate(CriteriaBuilder builder, Root<RosterEntity> root, UUID ambiguousID) {
        //Group Export passes in the rosterID as the jobBatch providerID
        final Predicate rosterIDPredicate = builder.equal(root.get(RosterEntity_.ID), ambiguousID);
        //DataService passes in the providerID as the jobBatch providerID
        final Predicate providerIDPredicate = builder.equal(root.get(RosterEntity_.ATTRIBUTED_PROVIDER).get(ProviderEntity_.ID), ambiguousID);
        return builder.or(rosterIDPredicate, providerIDPredicate);
    }

    private Predicate mbiPredicate(CriteriaBuilder builder, Root<RosterEntity> root, String patientMBI) {
        final Join<RosterEntity, AttributionRelationship> attrJoin = root.join(RosterEntity_.ATTRIBUTIONS);
        final Join<AttributionRelationship, PatientEntity> patientJoin = attrJoin.join(AttributionRelationship_.PATIENT);
        //The database labels the column beneficiaryId but it's actually storing the MBI
        return builder.equal(patientJoin.get(PatientEntity_.BENEFICIARY_ID), patientMBI);
    }


}