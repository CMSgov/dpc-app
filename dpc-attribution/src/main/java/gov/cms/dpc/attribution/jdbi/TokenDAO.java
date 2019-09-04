package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.common.entities.TokenEntity;
import gov.cms.dpc.common.entities.TokenEntity_;
import gov.cms.dpc.common.hibernate.DPCManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.UUID;

public class TokenDAO extends AbstractDAO<TokenEntity> {

    @Inject
    TokenDAO(DPCManagedSessionFactory factory) {
        super(factory.getSessionFactory());
    }

    public TokenEntity persistToken(TokenEntity entity) {
        return persist(entity);
    }

    public List<TokenEntity> fetchTokens(UUID organizationID) {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<TokenEntity> query = builder.createQuery(TokenEntity.class);
        final Root<TokenEntity> root = query.from(TokenEntity.class);

        query.where(builder.equal(root.get(TokenEntity_.organization), organizationID));
        return this.list(query);
    }

    public List<TokenEntity> findTokenByOrgAndID(UUID organizationID, UUID tokenID) {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<TokenEntity> query = builder.createQuery(TokenEntity.class);
        final Root<TokenEntity> root = query.from(TokenEntity.class);

        query.where(builder.and(
                builder.equal(root.get(TokenEntity_.id), tokenID),
                builder.equal(root.get(TokenEntity_.organization), organizationID)));

        return this.list(query);
    }

    public void deleteToken(TokenEntity entity) {
        this.currentSession().delete(entity);
    }
}
