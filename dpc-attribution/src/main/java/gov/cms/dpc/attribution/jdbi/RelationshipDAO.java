package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.attribution.models.AttributionRelationship;
import gov.cms.dpc.fhir.FHIRExtractors;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;

import javax.inject.Inject;

@SuppressWarnings("unchecked")
public class RelationshipDAO extends AbstractDAO<AttributionRelationship> {

    @Inject
    public RelationshipDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    AttributionRelationship lookupAttributionRelationship(Practitioner provider, Patient patient) {
        final Query query = namedQuery("findRelationship");
        query.setParameter("provID", FHIRExtractors.getProviderNPI(provider));
        query.setParameter("patID", FHIRExtractors.getPatientMPI(patient));

        // FIXME: This could be null, what exception do we want to throw?
        return uniqueResult(query);
    }

    void removeAttributionRelationship(AttributionRelationship relationship) {
        currentSession().delete(relationship);
    }
}
