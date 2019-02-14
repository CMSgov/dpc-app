package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.attribution.engine.AttributionEngine;
import gov.cms.dpc.attribution.models.AttributionRelationship;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class AttributionDAO extends AbstractDAO<AttributionRelationship> implements AttributionEngine {

    @Inject
    public AttributionDAO(SessionFactory factory) {
        super(factory);
    }

    public long createAttibutionRelationship(AttributionRelationship relationship) {
        return persist(relationship).getAttributionID();
    }

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
        final Optional<AttributionRelationship> relationship = findAttribution(providerID, beneficiaryID);
        relationship.ifPresent((rel) -> currentSession().delete(rel));
    }

    @Override
    public boolean isAttributed(String providerID, String beneficiaryID) {
        return findAttribution(providerID, beneficiaryID).isPresent();
    }

    private Optional<AttributionRelationship> findAttribution(String providerID, String beneficiaryID) {
        final Query query = namedQuery("gov.cms.dpc.attribution.AttributionRelationship.findProvider");
        query.setParameter("provID", providerID);
        query.setParameter("patID", beneficiaryID);
        return Optional.ofNullable(uniqueResult(query));
    }
}
