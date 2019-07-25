package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.common.entities.EndpointEntity;
import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.common.hibernate.DPCManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;
import org.hl7.fhir.dstu3.model.Organization;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class OrganizationDAO extends AbstractDAO<OrganizationEntity> {

    @Inject
    OrganizationDAO(DPCManagedSessionFactory factory) {
        super(factory.getSessionFactory());
    }

    public Organization registerOrganization(Organization resource, List<EndpointEntity> endpoints) {
        final OrganizationEntity entity = new OrganizationEntity().fromFHIR(resource);
        endpoints.forEach(endpointEntity -> endpointEntity.setOrganization(entity));
        entity.setEndpoints(endpoints);
        final OrganizationEntity persisted = persist(entity);

        return persisted.toFHIR();
    }

    public Optional<OrganizationEntity> fetchOrganization(UUID organizationID) {
     return Optional.ofNullable(get(organizationID));
    }

    public List<OrganizationEntity> listOrganizations() {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<OrganizationEntity> query = builder.createQuery(OrganizationEntity.class);
        final Root<OrganizationEntity> root = query.from(OrganizationEntity.class);
        query.select(root);

        return list(query);
    }

    public void updateOrganization(OrganizationEntity entity) {
        persist(entity);
    }

    public List<OrganizationEntity> searchByToken(String tokenID) {

        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<OrganizationEntity> query = builder.createQuery(OrganizationEntity.class);
        final Root<OrganizationEntity> root = query.from(OrganizationEntity.class);

        query.where(builder.equal(root.join("tokens").get("id"), tokenID));

        return list(query);
    }

    public List<OrganizationEntity> searchByIdentifier(String identifier) {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<OrganizationEntity> query = builder.createQuery(OrganizationEntity.class);
        final Root<OrganizationEntity> root = query.from(OrganizationEntity.class);

        query.where(builder.equal(root.get("organizationID").get("value"), identifier));

        return list(query);
    }
}
