package gov.cms.dpc.aggregation.dao;

import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import io.dropwizard.hibernate.AbstractDAO;

import javax.inject.Inject;
import java.util.Optional;
import java.util.UUID;

public class OrganizationDAO extends AbstractDAO<OrganizationEntity> {

    @Inject
    OrganizationDAO(DPCManagedSessionFactory factory) {
        super(factory.getSessionFactory());
    }


    public Optional<String> fetchOrganizationNPI(UUID organizationID) {
        return Optional.ofNullable(get(organizationID))
                .map(OrganizationEntity::getOrganizationID)
                .filter(id -> DPCIdentifierSystem.NPPES.equals(id.getSystem()))
                .map(OrganizationEntity.OrganizationID::getValue);
    }
}
