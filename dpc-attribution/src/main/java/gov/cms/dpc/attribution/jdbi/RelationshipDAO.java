package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.common.entities.AttributionRelationship;
import gov.cms.dpc.common.exceptions.UnknownRelationship;
import gov.cms.dpc.fhir.FHIRExtractors;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

@SuppressWarnings("unchecked")
public class RelationshipDAO extends AbstractDAO<AttributionRelationship> {

    private static final Logger logger = LoggerFactory.getLogger(RelationshipDAO.class);

    @Inject
    public RelationshipDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
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
    public AttributionRelationship lookupAttributionRelationship(Practitioner provider, Patient patient) {

        final String providerNPI = FHIRExtractors.getProviderNPI(provider);
        final String patientMPI = FHIRExtractors.getPatientMPI(patient);
        logger.debug("Looking up attribution between {} and {}", providerNPI, patientMPI);

        final Query query = namedQuery("findRelationship");
        query.setParameter("provID", providerNPI);
        query.setParameter("patID", patientMPI);

        final AttributionRelationship relationship = uniqueResult(query);

        if (relationship == null) {
            logger.debug("Unknown attribution relationship between {} and {}", providerNPI, patientMPI);
            throw new UnknownRelationship(providerNPI, patientMPI);
        }

        return relationship;
    }

    public void addAttributionRelationship(AttributionRelationship relationship) {
        persist(relationship);
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
