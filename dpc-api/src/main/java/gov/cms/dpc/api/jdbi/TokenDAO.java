package gov.cms.dpc.api.jdbi;

import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.common.entities.OrganizationEntity_;
import gov.cms.dpc.common.entities.TokenEntity;
import gov.cms.dpc.common.entities.TokenEntity_;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.query.Query;

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
        // Check to ensure that the organization exists
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<Boolean> query = builder.createQuery(Boolean.class);
        final Root<OrganizationEntity> root = query.from(OrganizationEntity.class);

        query.select(builder.literal(true))
                .where(builder.equal(root.get(OrganizationEntity_.id), entity.getOrganization().getId()));
        final Query<Boolean> orgExists = this.currentSession().createQuery(query);
        // If the org is not present, an exception will be thrown, which we can catch in the caller
        orgExists.getSingleResult();

        return persist(entity);
    }

    public List<TokenEntity> fetchTokens(UUID organizationID) {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<TokenEntity> query = builder.createQuery(TokenEntity.class);
        final Root<TokenEntity> root = query.from(TokenEntity.class);

        query.where(builder.equal(root.get(TokenEntity_.organization).get(OrganizationEntity_.ID), organizationID));
        return this.list(query);
    }

    public List<TokenEntity> findTokenByOrgAndID(UUID organizationID, UUID tokenID) {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<TokenEntity> query = builder.createQuery(TokenEntity.class);
        final Root<TokenEntity> root = query.from(TokenEntity.class);

        query.where(builder.and(
                builder.equal(root.get(TokenEntity_.id), tokenID.toString()),
                builder.equal(root.get(TokenEntity_.organization).get(OrganizationEntity_.id), organizationID)));

        return this.list(query);
    }

    public void deleteToken(TokenEntity entity) {
        this.currentSession().delete(entity);
    }
}
