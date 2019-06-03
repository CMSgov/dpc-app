package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.common.entities.AttributionRelationship;
import gov.cms.dpc.common.entities.PatientEntity;
import gov.cms.dpc.common.entities.ProviderEntity;
import gov.cms.dpc.common.exceptions.UnknownRelationship;
import gov.cms.dpc.common.hibernate.DPCManagedSessionFactory;
import gov.cms.dpc.common.interfaces.AttributionEngine;
import gov.cms.dpc.fhir.FHIRExtractors;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class ProviderDAO extends AbstractDAO<ProviderEntity> implements AttributionEngine {

    private static final Logger logger = LoggerFactory.getLogger(ProviderDAO.class);

    private final RelationshipDAO rDAO;

    @Inject
    public ProviderDAO(DPCManagedSessionFactory factory) {
        super(factory.getSessionFactory());
        this.rDAO = new RelationshipDAO(factory);
    }

    @Override
    public Optional<List<String>> getAttributedPatientIDs(Practitioner provider) {

        // Extract the provider NPI
        if (!providerExists(FHIRExtractors.getProviderNPI(provider))) {
            return Optional.empty();
        }
        final Query<ProviderEntity> query = namedQuery("findByProvider")
                .setParameter("id", FHIRExtractors.getProviderNPI(provider));

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
        attributionBundle
                .getEntry()
                .stream()
                .filter(Bundle.BundleEntryComponent::hasResource)
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource.getResourceType() == ResourceType.Patient)
                .map(patient -> PatientEntity.fromFHIR((Patient) patient))
                .map(pEntity -> new AttributionRelationship(provider, pEntity))
                .forEach(this.rDAO::addAttributionRelationship);
    }

    @Override
    public void removeAttributionRelationship(Practitioner provider, Patient patient) {

        // Lookup the attribution relationship by NPI and MPI
        // If nothing exists, simply note it and return
        try {
            final AttributionRelationship attributionRelationship = this.rDAO.lookupAttributionRelationship(provider, patient);
            this.rDAO.removeAttributionRelationship(attributionRelationship);
        } catch (UnknownRelationship e) {
            logger.warn("Attempting to delete unknown attribution relationship.", e);
        }
    }

    @Override
    public boolean isAttributed(Practitioner provider, Patient patient) {
        try {
            this.rDAO.lookupAttributionRelationship(provider, patient);
            return true;
        } catch (UnknownRelationship e) {
            return false;
        }
    }

    @Override
    public void assertHealthy() {
        // If we can reach it, it's healthy
    }

    private boolean providerExists(String providerNPI) {
        final Query<ProviderEntity> query = namedQuery("getProvider");
        query.setParameter("provID", providerNPI);
        return uniqueResult(query) != null;
    }
}
