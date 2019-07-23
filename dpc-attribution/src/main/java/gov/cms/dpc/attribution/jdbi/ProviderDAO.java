package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.common.entities.AttributionRelationship;
import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.common.entities.PatientEntity;
import gov.cms.dpc.common.entities.ProviderEntity;
import gov.cms.dpc.common.exceptions.UnknownRelationship;
import gov.cms.dpc.common.hibernate.DPCManagedSessionFactory;
import gov.cms.dpc.common.interfaces.AttributionEngine;
import gov.cms.dpc.fhir.FHIRExtractors;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.query.Query;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    public ProviderEntity persistProvider(ProviderEntity provider) {
        return this.persist(provider);
    }

    public Optional<ProviderEntity> getProvider(UUID providerID) {
        return Optional.ofNullable(get(providerID));
    }

    public List<ProviderEntity> getProviders(UUID providerID, String providerNPI, UUID organizationID) {

        // Build a selection query to get records from the database
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<ProviderEntity> query = builder.createQuery(ProviderEntity.class);
        final Root<ProviderEntity> root = query.from(ProviderEntity.class);

        query.select(root);

        List<Predicate> predicates = new ArrayList<>();
        // Always restrict by Organization
        predicates.add(builder
                .equal(root.join("organization").get("id"),
                        organizationID));

        // If we're provided a resource ID, query for that
        if (providerID != null) {
            predicates.add(builder
                    .equal(root.get("providerID"), providerID));
        }

        // If we've provided an NPI, use it as a query restriction.
        // Otherwise, return everything
        if (providerNPI != null && !providerNPI.isEmpty()) {
            predicates.add(builder
                    .equal(root.get("providerNPI"),
                            providerNPI));
        }

        query.where(predicates.toArray(new Predicate[0]));
        return this.list(query);
    }

    public void deleteProvider(ProviderEntity provider) {
        this.currentSession().remove(provider);
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
    public void addAttributionRelationships(Bundle attributionBundle, UUID organizationID) {
        // Web API check that this is ok to do
        final Practitioner practitioner = (Practitioner) attributionBundle.getEntryFirstRep().getResource();
        final ProviderEntity provider = ProviderEntity.fromFHIR(practitioner);
        final OrganizationEntity organization = new OrganizationEntity();
        organization.setId(organizationID);

        // Get the patients and create the attribution
        attributionBundle
                .getEntry()
                .stream()
                .filter(Bundle.BundleEntryComponent::hasResource)
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource.getResourceType() == ResourceType.Patient)
                .map(patient -> PatientEntity.fromFHIR((Patient) patient))
                .peek(patientEntity -> patientEntity.setOrganization(organization))
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
