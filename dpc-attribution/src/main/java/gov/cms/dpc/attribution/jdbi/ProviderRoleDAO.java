package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.common.entities.ProviderRoleEntity;
import gov.cms.dpc.common.hibernate.DPCManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;

import javax.inject.Inject;
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
}
