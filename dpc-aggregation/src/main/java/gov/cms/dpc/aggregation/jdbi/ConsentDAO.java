package gov.cms.dpc.aggregation.jdbi;

import gov.cms.dpc.common.consent.entities.ConsentEntity;
import gov.cms.dpc.common.consent.entities.ConsentEntity_;
import gov.cms.dpc.common.hibernate.consent.DPCConsentManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;
import jakarta.inject.Inject;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ConsentDAO extends AbstractDAO<ConsentEntity> {

    private final SessionFactory sessionFactory;

    @Inject
    public ConsentDAO(DPCConsentManagedSessionFactory factory) {
        super(factory.getSessionFactory());
        this.sessionFactory = factory.getSessionFactory();
    }

    public final void persistConsent(ConsentEntity consentEntity) {
        this.persist(consentEntity);
    }

    public final Optional<ConsentEntity> getConsent(UUID consentID) {
        return Optional.ofNullable(get(consentID));
    }

    public final List<ConsentEntity> list() {
        try (Session session = this.sessionFactory.openSession()) {
            final CriteriaBuilder builder = session.getCriteriaBuilder();
            final CriteriaQuery<ConsentEntity> query = builder.createQuery(ConsentEntity.class);

            query.from(ConsentEntity.class);

            return list(query);
        }
    }

    public final List<ConsentEntity> findBy(String field, String value) {
        try (Session session = this.sessionFactory.openSession()) {
            final CriteriaBuilder builder = session.getCriteriaBuilder();
            final CriteriaQuery<ConsentEntity> query = builder.createQuery(ConsentEntity.class);
            final Root<ConsentEntity> root = query.from(ConsentEntity.class);

            query.select(root).where(builder.equal(root.get(field), value));

            return list(query);
        }
    }

    public final List<ConsentEntity> findByMbis(List<String> mbis) {
        try (Session session = this.sessionFactory.openSession()) {
            final CriteriaBuilder builder = session.getCriteriaBuilder();
            final CriteriaQuery<ConsentEntity> query = builder.createQuery(ConsentEntity.class);
            final Root<ConsentEntity> root = query.from(ConsentEntity.class);

            query.select(root).where(root.get(ConsentEntity_.MBI).in(mbis));

            return list(query);
        }
    }
}
