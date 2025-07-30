package gov.cms.dpc.attribution.jdbi;

import com.google.inject.name.Named;
import gov.cms.dpc.common.entities.*;
import gov.cms.dpc.common.hibernate.attribution.DPCAbstractDAO;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import jakarta.inject.Inject;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.apache.commons.collections4.ListUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class PatientDAO extends DPCAbstractDAO<PatientEntity> {
    private final int queryChunkSize;


    @Inject
    public PatientDAO(DPCManagedSessionFactory factory, @Named("queryChunkSize") int queryChunkSize) {
        super(factory.getSessionFactory());
        this.queryChunkSize = queryChunkSize;
    }

    public PatientEntity persistPatient(PatientEntity patient) {
        return this.persist(patient);
    }

    public Optional<PatientEntity> getPatient(UUID patientID) {
        return Optional.ofNullable(get(patientID));
    }

    public int countMatchingPatients(PatientSearchQuery searchQuery) {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
        final Root<PatientEntity> root = countQuery.from(PatientEntity.class);

        List<Predicate> predicates = buildPredicates(builder, root, searchQuery);
        countQuery.select(builder.count(root));
        countQuery.where(predicates.toArray(new Predicate[0]));

        return currentSession().createQuery(countQuery).getSingleResult().intValue();
    }

    private List<Predicate> buildPredicates(CriteriaBuilder builder, Root<PatientEntity> root, PatientSearchQuery searchQuery) {
        List<Predicate> predicates = new ArrayList<>();
        if (searchQuery.getResourceID() != null) {
            predicates.add(builder.equal(root.get(PatientEntity_.id), searchQuery.getResourceID()));
        }
        if (searchQuery.getPatientMBI() != null) {
            predicates.add(builder.equal(root.get(PatientEntity_.beneficiaryID), searchQuery.getPatientMBI().toUpperCase()));
        }
        if (searchQuery.getOrganizationID() != null) {
            predicates.add(builder.equal(root.get(PatientEntity_.organization).get(OrganizationEntity_.id), searchQuery.getOrganizationID()));
        }
        if (predicates.isEmpty()) {
            throw new IllegalStateException("Must have at least one search predicate!");
        }
        return predicates;
    }

    public PageResult<PatientEntity> patientSearch(PatientSearchQuery searchQuery) {
        // UUID resourceID, String patientMBI, UUID organizationID
        // Build a selection query to get records from the database
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<PatientEntity> query = builder.createQuery(PatientEntity.class);
        final Root<PatientEntity> root = query.from(PatientEntity.class);
        query.select(root);

        final List<Predicate> predicates = buildPredicates(builder, root, searchQuery);
        query.where(predicates.toArray(new Predicate[0]));
        TypedQuery<PatientEntity> typedQuery = this.currentSession().createQuery(query);

        // Legacy compatibility for request without pagination
        if (searchQuery.getCount() == null) {
            return new PageResult<>(typedQuery.getResultList(), false);
        }

        Integer count = searchQuery.getCount();
        final int fetchSize = count + 1;
        typedQuery.setMaxResults(fetchSize);
        if (searchQuery.getPageOffset() != null && searchQuery.getPageOffset() > 0) {
            typedQuery.setFirstResult(searchQuery.getPageOffset());
        }
        final List<PatientEntity> fetched = typedQuery.getResultList();
        final boolean hasNext = fetched.size() > count;
        List<PatientEntity> pageResults = fetched;
        if (hasNext) {
            pageResults = fetched.subList(0, count);
        }

        return new PageResult<>(
                pageResults,
                hasNext
        );
    }

    /**
     * Returns a list of all {@link PatientEntity}s whose id is in resourceIds.
     * @param resourceIDs list of IDs
     * @return List of {@link PatientEntity}s
     */
    public List<PatientEntity> bulkPatientSearchById(UUID organizationId, List<UUID> resourceIDs) {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<PatientEntity> query = builder.createQuery(PatientEntity.class);
        final Root<PatientEntity> root = query.from(PatientEntity.class);

        query.select(root)
            .where(builder.and(
                root.get(PersonEntity_.id).in(resourceIDs),
                builder.equal(root.get(PatientEntity_.organization).get(OrganizationEntity_.id), organizationId))
            );
        return list(query);
    }

    /**
     * Returns a list of all {@link PatientEntity}s whose id is in resourceIds.
     * @param mbis
     * @return List of {@link PatientEntity}s
     */
    public List<PatientEntity> bulkPatientSearchByMbi(UUID organizationId, List<String> mbis) {
        List<PatientEntity> results = new ArrayList<>();

        // With large patient inserts, this can theoretically be called with 10s of thousands of patients, so break
        // it up into queries that we can handle without causing a stack overflow.
        List<List<String>> mbiChunks = ListUtils.partition(mbis, queryChunkSize);
        mbiChunks.forEach(mbiList -> {
            List<String> capitalizedMbis = mbiList.stream().map(String::toUpperCase).collect(Collectors.toList());

            final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
            final CriteriaQuery<PatientEntity> query = builder.createQuery(PatientEntity.class);
            final Root<PatientEntity> root = query.from(PatientEntity.class);

            query.select(root)
                .where(builder.and(
                    root.get(PatientEntity_.beneficiaryID).in(capitalizedMbis),
                    builder.equal(root.get(PatientEntity_.organization).get(OrganizationEntity_.id), organizationId))
                );

            results.addAll(list(query));
        });

        return results;
    }

    public boolean deletePatient(UUID patientID) {
        final PatientEntity patientEntity = this.get(patientID);

        if (patientEntity == null) {
            return false;
        }

        // Delete all the attribution relationships
        removeAttributionRelationships(patientEntity);

        this.currentSession().remove(patientEntity);

        return true;
    }

    public PatientEntity updatePatient(UUID patientID, PatientEntity updatedPatient) {
        final PatientEntity patient = this.getPatient(patientID)
                .orElseThrow(() -> new IllegalArgumentException("Cannot find patient"));

        final PatientEntity fullyUpdated = patient.update(updatedPatient);

        currentSession().merge(fullyUpdated);

        return fullyUpdated;
    }

    // We have to suppress this because the list returned is actually Strings, but we can't prove it to the compiler
    @SuppressWarnings("rawtypes")
    public List fetchPatientMBIByRosterID(UUID rosterID, boolean activeOnly) {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery query = builder.createQuery(); // untyped to avoid QueryTypeMismatchException
        final Root<PatientEntity> root = query.from(PatientEntity.class);
        query.select(root);

        // Join across the AttributionRelationships
        final ListJoin<PatientEntity, AttributionRelationship> attrJoins = root.join(PatientEntity_.attributions);
        final Join<AttributionRelationship, RosterEntity> rosterJoin = attrJoins.join(AttributionRelationship_.roster);

        query.select(root.get(PatientEntity_.BENEFICIARY_I_D));

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(builder.equal(rosterJoin.get(RosterEntity_.id), rosterID));

        if (activeOnly) {
            predicates.add(builder.equal(attrJoins.get(AttributionRelationship_.inactive), false));
        }
        query.where(predicates.toArray(new Predicate[0]));
        return list(query);
    }

    private void removeAttributionRelationships(PatientEntity patientEntity) {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaDelete<AttributionRelationship> criteriaDelete = builder.createCriteriaDelete(AttributionRelationship.class);
        final Root<AttributionRelationship> root = criteriaDelete.from(AttributionRelationship.class);

        criteriaDelete.where(builder.equal(root
                        .get(AttributionRelationship_.patient)
                        .get(PatientEntity_.id),
                patientEntity.getID()));
        this.currentSession().createMutationQuery(criteriaDelete).executeUpdate();
    }
}
