package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class OrganizationDAO extends AbstractDAO<OrganizationEntity> {

    @Inject
    OrganizationDAO(DPCManagedSessionFactory factory) {
        super(factory.getSessionFactory());
    }

    public OrganizationEntity registerOrganization(OrganizationEntity entity) {
        return this.persist(entity);
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

    public List<OrganizationEntity> getOrganizationsByIds(Set<String> ids) {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<OrganizationEntity> query = builder.createQuery(OrganizationEntity.class);
        final Root<OrganizationEntity> root = query.from(OrganizationEntity.class);
        
        query.select(root).where(root.get("organizationID").get("value").in(ids));

        return list(query);
    }

    public OrganizationEntity updateOrganization(UUID organizationID, OrganizationEntity updatedOrganization) {
        OrganizationEntity organization = fetchOrganization(organizationID)
                .orElseThrow(() -> new IllegalArgumentException("Cannot find organization"));
        organization.update(updatedOrganization);
        currentSession().merge(organization);
        return organization;
    }

    public void deleteOrganization(OrganizationEntity entity) {
        currentSession().delete(entity);
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
