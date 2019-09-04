package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.common.entities.OrganizationEntity_;
import gov.cms.dpc.common.entities.TokenEntity;
import gov.cms.dpc.common.entities.TokenEntity_;
import gov.cms.dpc.common.hibernate.DPCManagedSessionFactory;
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

        final Query<Boolean> query1 = this.currentSession().createQuery(query);

        query1.getSingleResult();

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
