package gov.cms.dpc.api.jdbi;

import gov.cms.dpc.api.entities.TokenEntity;
import gov.cms.dpc.api.entities.TokenEntity_;
import gov.cms.dpc.common.hibernate.auth.DPCAuthManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

public class TokenDAO extends AbstractDAO<TokenEntity> {

    private final SessionFactory factory;

    @Inject
    public TokenDAO(DPCAuthManagedSessionFactory factory) {
        super(factory.getSessionFactory());
        this.factory = factory.getSessionFactory();
    }

    public TokenEntity persistToken(TokenEntity entity) {
        return persist(entity);
    }

    public List<TokenEntity> fetchTokens(UUID organizationID) {
        final HibernateCriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final JpaCriteriaQuery<TokenEntity> query = builder.createQuery(TokenEntity.class);
        final JpaRoot<TokenEntity> root = query.from(TokenEntity.class);

        query.where(builder.equal(root.get(TokenEntity_.organizationID.toString()), organizationID));
        return this.list(query);
    }

    public List<TokenEntity> findTokenByOrgAndID(UUID organizationID, UUID tokenID) {
        final HibernateCriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final JpaCriteriaQuery<TokenEntity> query = builder.createQuery(TokenEntity.class);
        final JpaRoot<TokenEntity> root = query.from(TokenEntity.class);

        query.where(builder.and(
                builder.equal(root.get(TokenEntity_.id.toString()), tokenID.toString()),
                builder.equal(root.get(TokenEntity_.organizationID.toString()), organizationID)));

        return this.list(query);
    }

    /**
     * Method for matching a given token ID with the corresponding Organization.
     * This is designed to be used within the authentication handlers, thus it creates and manages a {@link Session} on its own
     *
     * @param tokenID - {@link UUID} tokenID to match with organization
     * @return - {@link UUID} organization ID which was issued the token
     */
    public UUID findOrgByToken(UUID tokenID) {
        try (Session session = this.factory.openSession()) {

            final HibernateCriteriaBuilder builder = session.getCriteriaBuilder();
            final JpaCriteriaQuery<TokenEntity> query = builder.createQuery(TokenEntity.class);
            final JpaRoot<TokenEntity> root = query.from(TokenEntity.class);

            query.where(builder.equal(root.get(TokenEntity_.id.toString()), tokenID.toString()));

            final Query<TokenEntity> matchQuery = session.createQuery(query);

            return matchQuery.getSingleResult().getOrganizationID();
        }
    }

    public void deleteToken(TokenEntity entity) {
        this.currentSession().delete(entity);
    }
}
