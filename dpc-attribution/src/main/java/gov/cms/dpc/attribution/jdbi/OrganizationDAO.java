package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.common.entities.EndpointEntity;
import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.common.hibernate.DPCManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;
import org.hl7.fhir.dstu3.model.Organization;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class OrganizationDAO extends AbstractDAO<OrganizationEntity> {

    @Inject
    OrganizationDAO(DPCManagedSessionFactory factory) {
        super(factory.getSessionFactory());
    }

    public void registerOrganization(Organization resource, List<EndpointEntity> endpoints) {
        final OrganizationEntity entity = new OrganizationEntity().fromFHIR(resource);
        endpoints.forEach(endpointEntity -> endpointEntity.setOrganization(entity));
        entity.setEndpoints(endpoints);
        persist(entity);
    }

    public Optional<OrganizationEntity> fetchOrganization(UUID organizationID) {
     return Optional.ofNullable(get(organizationID));
    }

    public void updateOrganization(OrganizationEntity entity) {
        persist(entity);
    }
}
