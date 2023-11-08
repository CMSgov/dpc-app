package gov.cms.dpc.consent.jdbi;

import gov.cms.dpc.common.consent.entities.ConsentEntity;
import gov.cms.dpc.common.hibernate.consent.DPCConsentManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ConsentDAO extends AbstractDAO<ConsentEntity> {

    @Inject
    public ConsentDAO(DPCConsentManagedSessionFactory factory) {
        super(factory.getSessionFactory());
    }

    public final ConsentEntity persistConsent(ConsentEntity consentEntity) {
        return this.persist(consentEntity);
    }

    public final Optional<ConsentEntity> getConsent(UUID consentID) {
        return Optional.ofNullable(get(consentID));
    }

    public final List<ConsentEntity> list() {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<ConsentEntity> query = builder.createQuery(ConsentEntity.class);
        query.from(ConsentEntity.class);
        return list(query);
    }

    public final List<ConsentEntity> findBy(String field, String value) {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<ConsentEntity> query = builder.createQuery(ConsentEntity.class);
        final Root<ConsentEntity> root = query.from(ConsentEntity.class);

        query.select(root).where(builder.equal(root.get(field), value));

        return this.list(query);
    }
}
