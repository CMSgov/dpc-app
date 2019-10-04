package gov.cms.dpc.api.jdbi;

import gov.cms.dpc.common.entities.TokenEntity;
import gov.cms.dpc.common.entities.TokenEntity_;
import gov.cms.dpc.common.hibernate.auth.DPCAuthManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.Session;
import org.hibernate.query.Query;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.UUID;

public class TokenDAO extends AbstractDAO<TokenEntity> {

    @Inject
    public TokenDAO(DPCAuthManagedSessionFactory factory) {
        super(factory.getSessionFactory());
    }

    public TokenEntity persistToken(TokenEntity entity) {
        // FIXME: We need a way to ensure the organization exists, maybe placing this earlier in the call stack?
        // Check to ensure that the organization exists
//        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
//        final CriteriaQuery<Boolean> query = builder.createQuery(Boolean.class);
//        final Root<OrganizationEntity> root = query.from(OrganizationEntity.class);
//
//        query.select(builder.literal(true))
//                .where(builder.equal(root.get(OrganizationEntity_.id), entity.getOrganizationID()));
//        final Query<Boolean> orgExists = this.currentSession().createQuery(query);
//        // If the org is not present, an exception will be thrown, which we can catch in the caller
//        orgExists.getSingleResult();

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
     * This is designed to be used within the authentication handlers, thus is expects an existing {@link Session} to be passed in an handled appropriately.
     *
     * @param session - {@link Session} existing session to use for accessing the database
     * @param tokenID - {@link UUID} tokenID to match with organization
     * @return - {@link UUID} organization ID which was issued the token
     */
    public static UUID findOrgByToken(Session session, UUID tokenID) {
        final CriteriaBuilder builder = session.getCriteriaBuilder();
        final CriteriaQuery<TokenEntity> query = builder.createQuery(TokenEntity.class);
        final Root<TokenEntity> root = query.from(TokenEntity.class);

        query.where(builder.equal(root.get(TokenEntity_.id), tokenID.toString()));

        final Query<TokenEntity> matchQuery = session.createQuery(query);

        return matchQuery.getSingleResult().getOrganizationID();
    }

    public void deleteToken(TokenEntity entity) {
        this.currentSession().delete(entity);
    }
}
