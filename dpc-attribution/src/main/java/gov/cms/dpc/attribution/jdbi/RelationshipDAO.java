package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.common.entities.AttributionRelationship;
import gov.cms.dpc.common.entities.AttributionRelationship_;
import gov.cms.dpc.common.entities.PatientEntity_;
import gov.cms.dpc.common.entities.RosterEntity_;
import gov.cms.dpc.common.exceptions.UnknownRelationship;
import gov.cms.dpc.common.hibernate.DPCManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("unchecked")
public class RelationshipDAO extends AbstractDAO<AttributionRelationship> {

    private static final Logger logger = LoggerFactory.getLogger(RelationshipDAO.class);

    @Inject
    public RelationshipDAO(DPCManagedSessionFactory sessionFactory) {
        super(sessionFactory.getSessionFactory());
    }

    /**
     * Attempts to retrieve the attribution relationship between the given provider and patient pair.
     * If no relationship exists, an exception is raised.
     *
     * @param provider - {@link Practitioner} provider to determine attribution with
     * @param patient  - {@link Patient} patient to determine attribution for
     * @return - {@link AttributionRelationship} if one exists
     * @throws UnknownRelationship - thrown if attribution is missing
     */
    public AttributionRelationship lookupAttributionRelationship(UUID provider, UUID patient) {

        logger.debug("Looking up attribution for Group/{} and Patient/{}", provider, patient);

        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<AttributionRelationship> query = builder.createQuery(AttributionRelationship.class);
        final Root<AttributionRelationship> root = query.from(AttributionRelationship.class);
        query.select(root);

        query.where(builder.and(
                builder.equal(root.get(AttributionRelationship_.roster).get(RosterEntity_.id), provider),
                builder.equal(root.get(AttributionRelationship_.patient).get(PatientEntity_.patientID), patient)));

        final AttributionRelationship relationship = uniqueResult(query);

        if (relationship == null) {
            logger.debug("Unknown attribution relationship between Group/{} and Patient/{}", provider, patient);
            throw new UnknownRelationship(provider.toString(), patient.toString());
        }

        return relationship;
    }

    public void updateAttributionRelationship(AttributionRelationship relationship) {
        this.currentSession().update(relationship);
    }

    public AttributionRelationship addAttributionRelationship(AttributionRelationship relationship) {
        return persist(relationship);
    }

    /**
     * Remove the given attribution relationship
     *
     * @param relationship - {@link AttributionRelationship} to remove
     */
    public void removeAttributionRelationship(AttributionRelationship relationship) {
        currentSession().delete(relationship);
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
