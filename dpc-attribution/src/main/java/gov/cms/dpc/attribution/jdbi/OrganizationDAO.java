package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.common.hibernate.attribution.DPCAbstractDAO;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class OrganizationDAO extends DPCAbstractDAO<OrganizationEntity> {

    @Inject
    public OrganizationDAO(DPCManagedSessionFactory factory) {
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
        // Deletes are cascaded to both rosters and patients, and sometimes Hibernate tries to delete the patients
        // first, which violates a foreign key in the DB.  We need to force rosters and attributions to get deleted
        // before patients.
        deleteRosters(entity);
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

    /**
     * Deletes all rosters attached to the organization, flushes the changes and refreshes the org in the Hibernate
     * persistence layer.
     * @param entity {@link OrganizationEntity} that will have its rosters deleted.
     */
    private void deleteRosters(OrganizationEntity entity) {
        entity.getRosters().forEach(org -> currentSession().delete(org));
        refresh(entity);
    }
}
