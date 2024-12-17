package gov.cms.dpc.attribution.jdbi;

import com.google.inject.Inject;
import gov.cms.dpc.common.entities.*;
import gov.cms.dpc.common.hibernate.attribution.DPCAbstractDAO;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import gov.cms.dpc.fhir.FHIRExtractors;
import jakarta.persistence.criteria.*;
import org.hibernate.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RosterDAO extends DPCAbstractDAO<RosterEntity> {

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

    public List<RosterEntity> findEntities(UUID resourceID, UUID organizationID, String providerNPI, String patientReference) {

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

        if (resourceID != null) {
            predicates.add(builder.equal(root.get(RosterEntity_.id), resourceID));
        }

        if (providerNPI != null) {
            predicates.add(builder.equal(root.get(RosterEntity_.ATTRIBUTED_PROVIDER).get(ProviderEntity_.PROVIDER_N_P_I), providerNPI));
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
        currentSession().remove(rosterEntity);
    }
}
