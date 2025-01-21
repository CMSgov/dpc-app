package gov.cms.dpc.api.jdbi;

import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.entities.PublicKeyEntity_;
import gov.cms.dpc.common.hibernate.auth.DPCAuthManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;
import jakarta.inject.Inject;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.util.List;
import java.util.Optional;
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

    public Optional<PublicKeyEntity> fetchPublicKey(UUID organizationID, UUID keyID) {

        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<PublicKeyEntity> query = builder.createQuery(PublicKeyEntity.class);
        final Root<PublicKeyEntity> root = query.from(PublicKeyEntity.class);

        query.where(builder.and(builder.equal(root.get(PublicKeyEntity_.organization_id), organizationID),
                builder.equal(root.get(PublicKeyEntity_.id), keyID)));

        final List<PublicKeyEntity> resultList = list(query);

        if (resultList.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(resultList.get(0));
    }

    public void deletePublicKey(PublicKeyEntity keyEntity) {
        currentSession().delete(keyEntity);
    }

    public PublicKeyEntity findKeyByLabel(String keyLabel) {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<PublicKeyEntity> query = builder.createQuery(PublicKeyEntity.class);
        final Root<PublicKeyEntity> root = query.from(PublicKeyEntity.class);

        query.where(builder.equal(root.get(PublicKeyEntity_.label), keyLabel));
        return currentSession().createQuery(query).getSingleResult();
    }

    public List<PublicKeyEntity> publicKeySearch(UUID keyID, UUID organizationID) {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<PublicKeyEntity> query = builder.createQuery(PublicKeyEntity.class);
        final Root<PublicKeyEntity> root = query.from(PublicKeyEntity.class);

        query.where(builder.and(
                builder.equal(root.get(PublicKeyEntity_.id), keyID),
                builder.equal(root.get(PublicKeyEntity_.organization_id), organizationID)));
        return list(query);
    }
}
