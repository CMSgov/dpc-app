package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.common.entities.ProviderRoleEntity;
import gov.cms.dpc.common.hibernate.DPCManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProviderRoleDAO extends AbstractDAO<ProviderRoleEntity> {

    @Inject
    ProviderRoleDAO(DPCManagedSessionFactory factory) {
        super(factory.getSessionFactory());
    }

    public ProviderRoleEntity persistRole(ProviderRoleEntity roleEntity) {
        return this.persist(roleEntity);
    }

    public ProviderRoleEntity fetchRole(UUID roleID) {
        return get(roleID);
    }

    public boolean removeRole(UUID roleID) {
        final ProviderRoleEntity entity = get(roleID);
        if (entity == null) {
            return false;
        }

        currentSession().remove(entity);
        return true;
    }

    public List<ProviderRoleEntity> findRoles(UUID organizationID, UUID providerID) {
        // Build a query to find a role that matches
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<ProviderRoleEntity> query = builder.createQuery(ProviderRoleEntity.class);
        final Root<ProviderRoleEntity> root = query.from(ProviderRoleEntity.class);
        query.select(root);

        // Add either, or both IDs
        // Both
        List<Predicate> idPredicates = new ArrayList<>();
        if (organizationID != null) {
            idPredicates.add(builder.equal(root.get("organization").get("id"), organizationID));
        }

        if (providerID != null) {
            idPredicates.add(builder.equal(root.get("provider").get("providerID"), providerID));
        }

        query.where(builder.and(idPredicates.toArray(new Predicate[0])));

        return list(query);
    }
}
