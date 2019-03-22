package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.attribution.models.AttributionRelationship;
import gov.cms.dpc.attribution.models.PatientEntity;
import gov.cms.dpc.attribution.models.ProviderEntity;
import gov.cms.dpc.common.interfaces.AttributionEngine;
import gov.cms.dpc.fhir.FHIRExtractors;
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
    public Optional<List<String>> getAttributedPatientIDs(Practitioner provider) {

        // Extract the provider NPI
        if (!providerExists(FHIRExtractors.getProviderNPI(provider))) {
            return Optional.empty();
        }
        final Query query = namedQuery("findByProvider")
                .setParameter("id", provider);

        final ProviderEntity providerEntity = uniqueResult(query);
        if (providerEntity == null) {
            return Optional.empty();
        }

        return Optional.of(providerEntity
                .getAttributedPatients()
                .stream()
                .map(PatientEntity::getBeneficiaryID)
                .collect(Collectors.toList()));
    }

    @Override
    public void addAttributionRelationship(Practitioner provider, Patient patient) {
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
    public void removeAttributionRelationship(Practitioner provider, Patient patient) {
        throw new UnsupportedOperationException("Not implemented until DPC-21");
    }

    @Override
    public boolean isAttributed(Practitioner provider, Patient patient) {
        final Query query = namedQuery("findRelationship");
        query.setParameter("provID", provider);
        query.setParameter("patID", patient);

        return uniqueResult(query) != null;
    }

    private boolean providerExists(String providerNPI) {
        final Query query = namedQuery("getProvider");
        query.setParameter("provID", providerNPI);
        return uniqueResult(query) != null;
    }
}
