package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.common.entities.*;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import gov.cms.dpc.fhir.FHIRExtractors;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.query.Query;

import javax.inject.Inject;
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

    public RosterEntity persistEntity(RosterEntity roster) {
        return this.persist(roster);
    }

    public Optional<RosterEntity> getEntity(UUID rosterID) {
        return Optional.ofNullable(this.get(rosterID));
    }

    public List<RosterEntity> findEntities(UUID organizationID, String providerNPI, String patientReference) {

        // Build a selection query to get records from the database
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<RosterEntity> query = builder.createQuery(RosterEntity.class);
        final Root<RosterEntity> root = query.from(RosterEntity.class);
        query.select(root);


        List<Predicate> predicates = new ArrayList<>();
        // Always restrict by Organization
        predicates.add(builder
                .equal(root.join(RosterEntity_.MANAGING_ORGANIZATION)
                                .get(OrganizationEntity_.ID),
                        organizationID));

        if (providerNPI != null) {
            predicates.add(builder.equal(root.get(RosterEntity_.ATTRIBUTED_PROVIDER).get(ProviderEntity_.PROVIDER_NP_I), providerNPI));
        }

        if (patientReference != null) {
            final UUID patientID = FHIRExtractors.getEntityUUID(patientReference);
            final Join<RosterEntity, AttributionRelationship> attrJoin = root.join(RosterEntity_.ATTRIBUTIONS);
            final Join<AttributionRelationship, PatientEntity> patientJoin = attrJoin.join(AttributionRelationship_.PATIENT);
            predicates.add(builder.equal(patientJoin.get(PatientEntity_.id), patientID));
        }

        query.where(predicates.toArray(new Predicate[0]));
        return this.list(query);
    }

    public boolean rosterExists(UUID rosterID) {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<Boolean> query = builder.createQuery(Boolean.class);
        final Root<RosterEntity> root = query.from(RosterEntity.class);
        query.select(builder.literal(true));

        query.where(builder.equal(root.get(RosterEntity_.id), rosterID));

        final Query<Boolean> booleanQuery = this.currentSession().createQuery(query);

        return booleanQuery.getSingleResult();
    }

    public void delete(RosterEntity rosterEntity) {
        currentSession().delete(rosterEntity);
    }
}
