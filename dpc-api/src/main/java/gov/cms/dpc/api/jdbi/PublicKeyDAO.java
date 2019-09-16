package gov.cms.dpc.api.jdbi;

import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.entities.PublicKeyEntity_;
import gov.cms.dpc.common.entities.OrganizationEntity_;
import gov.cms.dpc.common.hibernate.DPCManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.UUID;

public class PublicKeyDAO extends AbstractDAO<PublicKeyEntity> {

    @Inject
    PublicKeyDAO(DPCManagedSessionFactory factory) {
        super(factory.getSessionFactory());
    }

    public PublicKeyEntity persistPublicKey(PublicKeyEntity entity) {
        return persist(entity);
    }

    public List<PublicKeyEntity> fetchPublicKeys(UUID organizationID) {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<PublicKeyEntity> query = builder.createQuery(PublicKeyEntity.class);
        final Root<PublicKeyEntity> root = query.from(PublicKeyEntity.class);

        query.where(builder.equal(root.get(PublicKeyEntity_.MANAGING_ORGANIZATION).get(OrganizationEntity_.ID), organizationID));

        return list(query);
    }

    public List<PublicKeyEntity> fetchPublicKey(UUID certificateID, UUID organizationID) {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<PublicKeyEntity> query = builder.createQuery(PublicKeyEntity.class);
        final Root<PublicKeyEntity> root = query.from(PublicKeyEntity.class);

        query.where(builder.and(
                builder.equal(root.get(PublicKeyEntity_.id), certificateID),
                builder.equal(root.get(PublicKeyEntity_.MANAGING_ORGANIZATION).get(OrganizationEntity_.ID), organizationID)));
        
        return list(query);
    }
}
