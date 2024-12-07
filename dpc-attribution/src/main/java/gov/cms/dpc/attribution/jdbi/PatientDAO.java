package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.common.entities.*;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;

import jakarta.inject.Inject;
import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PatientDAO extends AbstractDAO<PatientEntity> {

    @Inject
    public PatientDAO(DPCManagedSessionFactory factory) {
        super(factory.getSessionFactory());
    }

    public PatientEntity persistPatient(PatientEntity patient) {
        return this.persist(patient);
    }

    public Optional<PatientEntity> getPatient(UUID patientID) {
        return Optional.ofNullable(get(patientID));
    }

    public List<PatientEntity> patientSearch(UUID resourceID, String patientMBI, UUID organizationID) {
        // Build a selection query to get records from the database
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<PatientEntity> query = builder.createQuery(PatientEntity.class);
        final Root<PatientEntity> root = query.from(PatientEntity.class);
        query.select(root);

        List<Predicate> predicates = new ArrayList<>();
        if (resourceID != null) {
            predicates.add(builder.equal(root.get(PatientEntity_.id), resourceID));
        }

        if (patientMBI != null) {
            predicates.add(builder.equal(root.get(PatientEntity_.beneficiaryID), patientMBI.toUpperCase()));
        }
        if (organizationID != null) {
            predicates.add(builder.equal(root.get(PatientEntity_.organization).get(OrganizationEntity_.id), organizationID));
        }
        if (predicates.isEmpty()) {
            throw new IllegalStateException("Must have at least one search predicate!");
        }

        query.where(predicates.toArray(Predicate[]::new));
        return this.list(query);
    }

    /**
     * Returns a list of all {@link PatientEntity}s whose id is in resourceIds.
     * @param organizationId UUID of the organization to search in
     * @param resourceIDs List of UUIDs for each patient ID to search
     * @return List of {@link PatientEntity}s
     */
    public List<PatientEntity> patientSearch(UUID organizationId, List<UUID> resourceIDs) {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<PatientEntity> query = builder.createQuery(PatientEntity.class);
        final Root<PatientEntity> root = query.from(PatientEntity.class);

        query.select(root)
            .where(builder.and(
                root.get(PatientEntity_.id).in(resourceIDs),
                builder.equal(root.get(PatientEntity_.organization).get(OrganizationEntity_.id), organizationId))
            );
        return list(query);
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

    public List<PatientEntity> fetchPatientMBIByRosterID(UUID rosterID, boolean activeOnly) {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<PatientEntity> query = builder.createQuery(PatientEntity.class);
        final Root<PatientEntity> root = query.from(PatientEntity.class);
        
        // Join across the AttributionRelationships
        final ListJoin<PatientEntity, AttributionRelationship> attrJoins = root.join(PatientEntity_.attributions);
        final Join<AttributionRelationship, RosterEntity> rosterJoin = attrJoins.join(AttributionRelationship_.roster);

        query.select(root);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(builder.equal(rosterJoin.get(RosterEntity_.id), rosterID));

        if (activeOnly) {
            predicates.add(builder.equal(attrJoins.get(AttributionRelationship_.inactive), false));
        }
        query.where(predicates.toArray(Predicate[]::new));
        
        List<PatientEntity> patients = this.list(query);
        List<PatientEntity> fakePatients = new ArrayList<>();
        for(PatientEntity patient : patients)
        {
            PatientEntity p = new PatientEntity();
            p.setBeneficiaryID(patient.getBeneficiaryID());
            fakePatients.add(p);
        }
        
        return fakePatients;
    }

    private int removeAttributionRelationships(PatientEntity patientEntity) {

        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaDelete<AttributionRelationship> criteriaDelete = builder.createCriteriaDelete(AttributionRelationship.class);
        final Root<AttributionRelationship> root = criteriaDelete.from(AttributionRelationship.class);

        criteriaDelete.where(builder.equal(root
                        .get(AttributionRelationship_.patient)
                        .get(PatientEntity_.id),
                patientEntity.getID()));
        return this.currentSession().createMutationQuery(criteriaDelete).executeUpdate();
    }
}
