package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.common.entities.*;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;

import javax.inject.Inject;
import javax.persistence.criteria.*;
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

        query.where(predicates.toArray(new Predicate[0]));
        return this.list(query);
    }

    public boolean deletePatient(UUID patientID) {
        final PatientEntity patientEntity = this.get(patientID);

        if (patientEntity == null) {
            return false;
        }

        // Delete all the attribution relationships
        removeAttributionRelationships(patientEntity);

        this.currentSession().delete(patientEntity);

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
        final CriteriaQuery<PatientEntity> query = builder.createQuery(PatientEntity.class);
        final Root<PatientEntity> root = query.from(PatientEntity.class);
        query.select(root);

        // Join across the AttributionRelationships
        final ListJoin<PatientEntity, AttributionRelationship> attrJoins = root.join(PatientEntity_.attributions);
        final Join<AttributionRelationship, RosterEntity> rosterJoin = attrJoins.join(AttributionRelationship_.roster);

        query.select(root.get(PatientEntity_.BENEFICIARY_ID));

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(builder.equal(rosterJoin.get(RosterEntity_.id), rosterID));

        if (activeOnly) {
            predicates.add(builder.equal(attrJoins.get(AttributionRelationship_.inactive), false));
        }
        query.where(predicates.toArray(new Predicate[0]));
        return this.list(query);
    }

    private int removeAttributionRelationships(PatientEntity patientEntity) {

        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaDelete<AttributionRelationship> criteriaDelete = builder.createCriteriaDelete(AttributionRelationship.class);
        final Root<AttributionRelationship> root = criteriaDelete.from(AttributionRelationship.class);

        criteriaDelete.where(builder.equal(root
                        .get(AttributionRelationship_.patient)
                        .get(PatientEntity_.id),
                patientEntity.getID()));
        return this.currentSession().createQuery(criteriaDelete).executeUpdate();
    }
}
