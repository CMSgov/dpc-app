package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.attribution.engine.AttributionEngine;
import gov.cms.dpc.attribution.models.AttributionRelationship;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AttributionDAO extends AbstractDAO<AttributionRelationship> implements AttributionEngine {

    @Inject
    public AttributionDAO(SessionFactory factory) {
        super(factory);
    }

    public long createAttibutionRelationship(AttributionRelationship relationship) {
        return persist(relationship).getAttributionID();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<String> getAttributedBeneficiaries(String providerID) {
        final Query query = namedQuery("gov.cms.dpc.attribution.models.AttributionRelationship.findByProvider")
                .setParameter("id", providerID);

        List<AttributionRelationship> patients = list(query);

        return patients
                .stream()
                .map(AttributionRelationship::getAttributedPatient)
                .collect(Collectors.toSet());
    }

    @Override
    public void addAttributionRelationship(String providerID, String beneficiaryID) {
        persist(new AttributionRelationship(providerID, beneficiaryID));
    }

    @Override
    public void removeAttributionRelationship(String providerID, String beneficiaryID) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
