package gov.cms.dpc.api.jdbi;

import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.entities.PublicKeyEntity_;
import gov.cms.dpc.common.hibernate.auth.DPCAuthManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.UUID;

public class PublicKeyDAO extends AbstractDAO<PublicKeyEntity> {

    @Inject
    PublicKeyDAO(DPCAuthManagedSessionFactory factory) {
        super(factory.getSessionFactory());
    }

    public PublicKeyEntity persistPublicKey(PublicKeyEntity entity) {
        return persist(entity);
    }

    public List<PublicKeyEntity> fetchPublicKeys(UUID organizationID) {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<PublicKeyEntity> query = builder.createQuery(PublicKeyEntity.class);
        final Root<PublicKeyEntity> root = query.from(PublicKeyEntity.class);

        query.where(builder.equal(root.get(PublicKeyEntity_.organization_id), organizationID));
        return list(query);
    }

    public List<PublicKeyEntity> fetchPublicKey(UUID keyID, UUID organizationID) {
        return publicKeySearch(keyID, organizationID);
    }

    public void deletePublicKey(UUID keyID, UUID organizationID) {
        publicKeySearch(keyID, organizationID)
                .forEach(key -> currentSession().delete(key));
    }

    public PublicKeyEntity findKeyByLabel(String keyLabel) {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<PublicKeyEntity> query = builder.createQuery(PublicKeyEntity.class);
        final Root<PublicKeyEntity> root = query.from(PublicKeyEntity.class);

        query.where(builder.equal(root.get(PublicKeyEntity_.label), keyLabel));
        return currentSession().createQuery(query).getSingleResult();
    }

    private List<PublicKeyEntity> publicKeySearch(UUID keyID, UUID organizationID) {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<PublicKeyEntity> query = builder.createQuery(PublicKeyEntity.class);
        final Root<PublicKeyEntity> root = query.from(PublicKeyEntity.class);

        query.where(builder.and(
                builder.equal(root.get(PublicKeyEntity_.id), keyID),
                builder.equal(root.get(PublicKeyEntity_.organization_id), organizationID)));
        return list(query);
    }
}
