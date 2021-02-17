package gov.cms.dpc.aggregation.dao;

import gov.cms.dpc.common.entities.ProviderEntity;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;

import javax.inject.Inject;
import java.util.Optional;
import java.util.UUID;

public class ProviderDAO extends AbstractDAO<ProviderEntity> {

    @Inject
    ProviderDAO(DPCManagedSessionFactory factory) {
        super(factory.getSessionFactory());
    }


    public Optional<String> fetchProviderNPI(UUID providerID, UUID orgID) {
        return Optional.ofNullable(get(providerID))
                .filter(provider -> provider.getOrganization().getId().equals(orgID))
                .map(ProviderEntity::getProviderNPI);
    }
}
