package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.attribution.models.AttributionRelationship;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public class AttributedPatientsDAO extends AbstractDAO<AttributionRelationship> {

    @Inject
    public AttributedPatientsDAO(SessionFactory factory) {
        super(factory);
    }

    public long createAttibutionRelationship(AttributionRelationship relationship) {
        return persist(relationship).getAttributionID();
    }


    @SuppressWarnings("unchecked")
    public List<String> getAttributedPatients(String providerID) {
        final Query query = namedQuery("gov.cms.dpc.attribution.models.AttributionRelationship.findByProvider")
                .setParameter("id", providerID);

        List<AttributionRelationship> patients = list(query);

        return patients
                .stream()
                .map(AttributionRelationship::getAttributedPatient)
                .collect(Collectors.toList());
    }
}
