package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.attribution.models.AttributionRelationship;
import gov.cms.dpc.attribution.models.PatientEntity;
import gov.cms.dpc.attribution.models.ProviderEntity;
import gov.cms.dpc.common.interfaces.AttributionEngine;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.ResourceType;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class AttributionDAO extends AbstractDAO<ProviderEntity> implements AttributionEngine {

    @Inject
    public AttributionDAO(SessionFactory factory) {
        super(factory);
    }

    public long createAttibutionRelationship(AttributionRelationship relationship) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<Set<String>> getAttributedBeneficiaries(String providerID) {

        if (!providerExists(providerID)) {
            return Optional.empty();
        }
        final Query query = namedQuery("findByProvider")
                .setParameter("id", providerID);

        final ProviderEntity provider = uniqueResult(query);

        return Optional.of(provider.getAttributedPatients()
                .stream()
                .map(PatientEntity::getBeneficiaryID)
                .collect(Collectors.toSet()));
    }

    @Override
    public void addAttributionRelationship(String providerID, String beneficiaryID) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void addAttributionRelationships(Bundle attributionBundle) {
        // Web API check that this is ok to do
        final Practitioner practitioner = (Practitioner) attributionBundle.getEntryFirstRep().getResource();
        final ProviderEntity provider = ProviderEntity.fromFHIR(practitioner);

        // Get the patients and create the attribution

        final List<PatientEntity> patients = attributionBundle
                .getEntry()
                .stream()
                .filter(Bundle.BundleEntryComponent::hasResource)
                .map(Bundle.BundleEntryComponent::getResource)
                .filter((resource -> resource.getResourceType() == ResourceType.Patient))
                .map(patient -> PatientEntity.fromFHIR((Patient) patient))
                .collect(Collectors.toList());

        provider.setAttributedPatients(patients);
        persist(provider);
    }

    @Override
    // TODO(nickrobison): To be completed in DPC-21
    public void removeAttributionRelationship(String providerID, String beneficiaryID) {
        throw new UnsupportedOperationException("Not implemented until DPC-21");
    }

    @Override
    public boolean isAttributed(String providerID, String beneficiaryID) {
        final Query query = namedQuery("findRelationship");
        query.setParameter("provID", providerID);
        query.setParameter("patID", beneficiaryID);

        return uniqueResult(query) != null;
    }

    private boolean providerExists(String providerID) {
        final Query query = namedQuery("getProvider");
        query.setParameter("provID", providerID);
        return uniqueResult(query) != null;
    }
}
