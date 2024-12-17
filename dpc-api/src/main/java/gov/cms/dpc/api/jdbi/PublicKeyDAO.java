package gov.cms.dpc.api.jdbi;

import com.google.inject.Inject;
import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.entities.PublicKeyEntity_;
import gov.cms.dpc.common.hibernate.auth.DPCAuthManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

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
        final HibernateCriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final JpaCriteriaQuery<PublicKeyEntity> query = builder.createQuery(PublicKeyEntity.class);
        final JpaRoot<PublicKeyEntity> root = query.from(PublicKeyEntity.class);

        query.where(builder.equal(root.get(PublicKeyEntity_.organization_id.toString()), organizationID));
        return list(query);
    }

    public Optional<PublicKeyEntity> fetchPublicKey(UUID organizationID, UUID keyID) {

        final HibernateCriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final JpaCriteriaQuery<PublicKeyEntity> query = builder.createQuery(PublicKeyEntity.class);
        final JpaRoot<PublicKeyEntity> root = query.from(PublicKeyEntity.class);

        query.where(builder.and(builder.equal(root.get(PublicKeyEntity_.organization_id.toString()), organizationID),
                builder.equal(root.get(PublicKeyEntity_.id.toString()), keyID)));

        final List<PublicKeyEntity> resultList = list(query);

        if (resultList.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(resultList.get(0));
    }

    public void deletePublicKey(PublicKeyEntity keyEntity) {
        currentSession().remove(keyEntity);
    }

    public PublicKeyEntity findKeyByLabel(String keyLabel) {
        final HibernateCriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final JpaCriteriaQuery<PublicKeyEntity> query = builder.createQuery(PublicKeyEntity.class);
        final JpaRoot<PublicKeyEntity> root = query.from(PublicKeyEntity.class);

        query.where(builder.equal(root.get(PublicKeyEntity_.label.toString()), keyLabel));
        return currentSession().createQuery(query).getSingleResult();
    }

    public List<PublicKeyEntity> publicKeySearch(UUID keyID, UUID organizationID) {
        final HibernateCriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final JpaCriteriaQuery<PublicKeyEntity> query = builder.createQuery(PublicKeyEntity.class);
        final JpaRoot<PublicKeyEntity> root = query.from(PublicKeyEntity.class);

        query.where(builder.and(
                builder.equal(root.get(PublicKeyEntity_.id.toString()), keyID),
                builder.equal(root.get(PublicKeyEntity_.organization_id.toString()), organizationID)));
        return list(query);
    }
}
