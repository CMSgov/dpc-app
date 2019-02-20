package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.attribution.models.AttributionRelationship;
import gov.cms.dpc.common.interfaces.AttributionEngine;
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
    public Optional<Set<String>> getAttributedBeneficiaries(String providerID) {

        if (!providerExists(providerID)) {
            return Optional.empty();
        }
        final Query query = namedQuery("findByProvider")
                .setParameter("id", providerID);

        List<AttributionRelationship> patients = list(query);

        return Optional.of(patients
                .stream()
                .map(AttributionRelationship::getAttributedPatient)
                .collect(Collectors.toSet()));
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

    private boolean providerExists(String providerID) {
        final Query query = namedQuery("getProvider");
        query.setParameter("provID", providerID);
        return uniqueResult(query) != null;
    }

    private Optional<AttributionRelationship> findAttribution(String providerID, String beneficiaryID) {
        final Query query = namedQuery("findRelationship");
        query.setParameter("provID", providerID);
        query.setParameter("patID", beneficiaryID);
        return Optional.ofNullable(uniqueResult(query));
    }
}
