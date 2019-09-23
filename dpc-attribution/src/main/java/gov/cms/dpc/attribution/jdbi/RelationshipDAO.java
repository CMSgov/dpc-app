package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.common.entities.AttributionRelationship;
import gov.cms.dpc.common.entities.AttributionRelationship_;
import gov.cms.dpc.common.entities.PatientEntity_;
import gov.cms.dpc.common.entities.RosterEntity_;
import gov.cms.dpc.common.exceptions.UnknownRelationship;
import gov.cms.dpc.common.hibernate.DPCManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.UUID;

public class RelationshipDAO extends AbstractDAO<AttributionRelationship> {

    private static final Logger logger = LoggerFactory.getLogger(RelationshipDAO.class);

    @Inject
    public RelationshipDAO(DPCManagedSessionFactory sessionFactory) {
        super(sessionFactory.getSessionFactory());
    }

    /**
     * Attempts to retrieve the attribution relationship between the given rosterID and patient pair.
     * If no relationship exists, an exception is raised.
     *
     * @param rosterID - {@link UUID} rosterID to determine attribution with
     * @param patient  - {@link UUID} patientID to determine attribution for
     * @return - {@link AttributionRelationship} if one exists
     * @throws UnknownRelationship - thrown if attribution is missing
     */
    public AttributionRelationship lookupAttributionRelationship(UUID rosterID, UUID patient) {

        logger.debug("Looking up attribution for Group/{} and Patient/{}", rosterID, patient);

        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<AttributionRelationship> query = builder.createQuery(AttributionRelationship.class);
        final Root<AttributionRelationship> root = query.from(AttributionRelationship.class);
        query.select(root);

        query.where(builder.and(
                builder.equal(root.get(AttributionRelationship_.roster).get(RosterEntity_.id), rosterID),
                builder.equal(root.get(AttributionRelationship_.patient).get(PatientEntity_.patientID), patient)));

        final AttributionRelationship relationship = uniqueResult(query);

        if (relationship == null) {
            logger.debug("Unknown attribution relationship between Group/{} and Patient/{}", rosterID, patient);
            throw new UnknownRelationship(rosterID.toString(), patient.toString());
        }

        return relationship;
    }

    /**
     * Remove all {@link AttributionRelationship} for the given attribution roster
     *
     * @param rosterID - {@link UUID} of roster to remove attributions from
     */
    public void removeRosterAttributions(UUID rosterID) {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaDelete<AttributionRelationship> query = builder.createCriteriaDelete(AttributionRelationship.class);
        final Root<AttributionRelationship> root = query.from(AttributionRelationship.class);

        query.where(builder.equal(root.get(AttributionRelationship_.roster).get(RosterEntity_.id), rosterID));

        this.currentSession().createQuery(query).executeUpdate();
    }

    /**
     * Update existing {@link AttributionRelationship}. Mostly used to set patients as inactive
     *
     * @param relationship - {@link AttributionRelationship} to update
     */
    public void updateAttributionRelationship(AttributionRelationship relationship) {
        this.currentSession().update(relationship);
    }

    /**
     * Create new {@link AttributionRelationship}
     *
     * @param relationship - {@link AttributionRelationship} to add
     */
    public void addAttributionRelationship(AttributionRelationship relationship) {
        persist(relationship);
    }

    /**
     * Retrieve all {@link AttributionRelationship} entities in the database.
     *
     * @return - {@link List} {@link AttributionRelationship}
     */
    public List<AttributionRelationship> getAttributions() {
        return list(query("FROM ATTRIBUTIONS"));
    }
}
