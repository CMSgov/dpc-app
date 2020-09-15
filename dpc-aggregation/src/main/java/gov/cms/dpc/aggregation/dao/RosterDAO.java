package gov.cms.dpc.aggregation.dao;

import gov.cms.dpc.common.entities.*;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.query.Query;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RosterDAO extends AbstractDAO<RosterEntity> {

    @Inject
    public RosterDAO(DPCManagedSessionFactory factory) {
        super(factory.getSessionFactory());
    }

    /**
     * Retrieves the ProviderID from the roster by orgID, patientMBI and either rosterID or providerID
     * @param organizationID        The organizationID
     * @param providerOrRosterID    Either a rosterID or the providerID
     * @param patientMBI            The patient MBI
     * @return the provider ID for that roster
     */
    public Optional<String> retrieveProviderNPIFromRoster(UUID organizationID, UUID providerOrRosterID, String patientMBI) {
        // Build a selection query to get records from the database
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<String> query = builder.createQuery(String.class);
        final Root<RosterEntity> root = query.from(RosterEntity.class);

        query.select(root.get(RosterEntity_.ATTRIBUTED_PROVIDER).get(ProviderEntity_.PROVIDER_NP_I));
        List<Predicate> predicates = new ArrayList<>();
        // Restrict by Organization via ID
        predicates.add(organizationPredicate(builder, root, organizationID));
        // Restrict by Provider or Roster via ID
        predicates.add(providerOrRosterIDPredicate(builder, root, providerOrRosterID));
        // Restrict by Patient via MBI
        predicates.add(mbiPredicate(builder, root, patientMBI));

        query.where(predicates.toArray(new Predicate[0]));


        Query<String> q = currentSession().createQuery(query);
        try {
            return Optional.ofNullable(q.getSingleResult());
        } catch(NoResultException e) {
            return Optional.empty();
        }
    }

    private Predicate organizationPredicate(CriteriaBuilder builder, Root<RosterEntity> root, UUID organizationID) {
        // Always restrict by Organization
        return builder.equal(root.join(RosterEntity_.MANAGING_ORGANIZATION).get(OrganizationEntity_.ID), organizationID);
    }

    private Predicate providerOrRosterIDPredicate(CriteriaBuilder builder, Root<RosterEntity> root, UUID providerOrRosterID) {
        //Group Export passes in the rosterID as the jobBatch providerID
        final Predicate rosterIDPredicate = builder.equal(root.get(RosterEntity_.ID), providerOrRosterID);
        //DataService passes in the providerID as the jobBatch providerID
        final Predicate providerIDPredicate = builder.equal(root.get(RosterEntity_.ATTRIBUTED_PROVIDER).get(ProviderEntity_.ID), providerOrRosterID);
        return builder.or(rosterIDPredicate, providerIDPredicate);
    }

    private Predicate mbiPredicate(CriteriaBuilder builder, Root<RosterEntity> root, String patientMBI) {
        final Join<RosterEntity, AttributionRelationship> attrJoin = root.join(RosterEntity_.ATTRIBUTIONS);
        final Join<AttributionRelationship, PatientEntity> patientJoin = attrJoin.join(AttributionRelationship_.PATIENT);
        //The database labels the column beneficiaryId but it's actually storing the MBI
        return builder.equal(patientJoin.get(PatientEntity_.BENEFICIARY_ID), patientMBI);
    }


}