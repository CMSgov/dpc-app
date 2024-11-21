package gov.cms.dpc.api.jdbi;

import gov.cms.dpc.api.entities.TokenEntity;
import gov.cms.dpc.api.entities.TokenEntity_;
import gov.cms.dpc.common.hibernate.auth.DPCAuthManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import com.google.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.UUID;

@Singleton
public class TokenDAO extends AbstractDAO<TokenEntity> {

    private final SessionFactory factory;
    
    @Inject
    public TokenDAO(DPCAuthManagedSessionFactory factory) {
        super(factory.getSessionFactory());
        this.factory = factory.getSessionFactory();
    }

    public TokenEntity persistToken(TokenEntity entity) {
        currentSession().flush();
        return persist(entity);
    }

    public List<TokenEntity> fetchTokens(UUID organizationID) {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<TokenEntity> query = builder.createQuery(TokenEntity.class);
        final Root<TokenEntity> root = query.from(TokenEntity.class);

        query.where(builder.equal(root.get(TokenEntity_.organizationID), organizationID));
        return this.list(query);
    }

    public List<TokenEntity> findTokenByOrgAndID(UUID organizationID, UUID tokenID) {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<TokenEntity> query = builder.createQuery(TokenEntity.class);
        final Root<TokenEntity> root = query.from(TokenEntity.class);

        query.where(builder.and(
                builder.equal(root.get(TokenEntity_.id), tokenID.toString()),
                builder.equal(root.get(TokenEntity_.organizationID), organizationID)));

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

            final CriteriaBuilder builder = session.getCriteriaBuilder();
            final CriteriaQuery<TokenEntity> query = builder.createQuery(TokenEntity.class);
            final Root<TokenEntity> root = query.from(TokenEntity.class);

            query.where(builder.equal(root.get(TokenEntity_.id), tokenID.toString()));

            final Query<TokenEntity> matchQuery = session.createQuery(query);

            return matchQuery.getSingleResult().getOrganizationID();
        }
    }

    public void deleteToken(TokenEntity entity) {
        this.currentSession().remove(entity);
    }
}
